package com.apptitle.score.service;

import com.apptitle.activity.dto.ActivityFileResponse;
import com.apptitle.activity.dto.ActivityFileUploadRequest;
import com.apptitle.activity.dto.ActivityFileUploadResponse;
import com.apptitle.activity.entity.Activity;
import com.apptitle.activity.entity.ActivityFile;
import com.apptitle.activity.entity.ActivitySubmission;
import com.apptitle.activity.entity.SubmissionStatus;
import com.apptitle.activity.repository.ActivityFileRepository;
import com.apptitle.activity.repository.ActivityRepository;
import com.apptitle.activity.repository.ActivitySubmissionRepository;
import com.apptitle.activity.service.ActivitySubmissionService;
import com.apptitle.classsection.entity.ClassEnrollmentRequestStatus;
import com.apptitle.classsection.repository.ClassEnrollmentRequestRepository;
import com.apptitle.common.exception.ApiException;
import com.apptitle.material.dto.CompleteUploadRequest;
import com.apptitle.score.dto.InitializeScoreProofRequest;
import com.apptitle.score.dto.ReviewScoreProposalRequest;
import com.apptitle.score.dto.ScoreProposalResponse;
import com.apptitle.score.entity.ScoreProposal;
import com.apptitle.score.entity.ScoreProposalStatus;
import com.apptitle.score.repository.ScoreProposalRepository;
import com.apptitle.student.entity.Student;
import com.apptitle.student.repository.StudentRepository;
import com.apptitle.subject.entity.ClassSubjectLinkStatus;
import com.apptitle.teacher.entity.Teacher;
import com.apptitle.teacher.repository.TeacherRepository;
import com.apptitle.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.apptitle.notification.entity.NotificationType;
import com.apptitle.notification.service.NotificationService;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ScoreProposalService {
    private final ScoreProposalRepository proposalRepository;
    private final ActivityRepository activityRepository;
    private final ActivityFileRepository fileRepository;
    private final ActivitySubmissionRepository submissionRepository;
    private final ClassEnrollmentRequestRepository enrollmentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final ActivitySubmissionService uploadService;
    private NotificationService notificationService;

    @Autowired(required = false)
    void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public ScoreProposalService(ScoreProposalRepository proposalRepository,
            ActivityRepository activityRepository, ActivityFileRepository fileRepository,
            ActivitySubmissionRepository submissionRepository,
            ClassEnrollmentRequestRepository enrollmentRepository,
            TeacherRepository teacherRepository, StudentRepository studentRepository,
            UserRepository userRepository, ActivitySubmissionService uploadService) {
        this.proposalRepository = proposalRepository;
        this.activityRepository = activityRepository;
        this.fileRepository = fileRepository;
        this.submissionRepository = submissionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.uploadService = uploadService;
    }

    @Transactional
    public ActivityFileUploadResponse initialize(String email, UUID activityId,
            InitializeScoreProofRequest request) {
        Student student = resolveStudent(email);
        Activity activity = resolveActivity(activityId);
        requireStudentAccess(student, activity);
        validateScore(request.reportedScore(), activity);
        proposalRepository.findByActivityIdAndStudentId(activityId, student.getId())
                .filter(value -> value.getStatus() != ScoreProposalStatus.REJECTED)
                .ifPresent(value -> { throw ApiException.conflict(
                        "A score proposal already exists for this activity."); });
        ActivityFileUploadResponse upload = uploadService.initializeScoreProof(email, activityId,
                new ActivityFileUploadRequest(request.fileName(), request.totalSizeBytes(),
                        request.contentType()));
        ActivityFile proof = fileRepository.findById(upload.fileId())
                .orElseThrow(() -> ApiException.notFound("Proof upload not found."));
        ScoreProposal proposal = proposalRepository
                .findByActivityIdAndStudentId(activityId, student.getId())
                .orElseGet(ScoreProposal::new);
        proposal.setActivity(activity);
        proposal.setStudent(student);
        proposal.setProofFile(proof);
        proposal.setReportedScore(request.reportedScore());
        proposal.setApprovedScore(null);
        proposal.setStatus(ScoreProposalStatus.PENDING);
        proposal.setSubmittedAt(Instant.now());
        proposal.setReviewedAt(null);
        proposal.setReviewedBy(null);
        proposalRepository.save(proposal);
        return upload;
    }

    @Transactional
    public ScoreProposalResponse complete(String email, UUID fileId,
            CompleteUploadRequest request) {
        ActivityFileResponse ignored = uploadService.completeScoreProof(email, fileId, request);
        ScoreProposal proposal = proposalRepository.findByProofFileId(fileId)
                .orElseThrow(() -> ApiException.notFound("Score proposal not found."));
        if (notificationService != null) {
            notificationService.notifyUser(proposal.getActivity().getCreatedBy().getUser(),
                    NotificationType.SCORE_SUBMISSION_REQUEST,
                    "Score proposal from " + proposal.getStudent().getName(),
                    proposal.getId().toString());
        }
        return toResponse(proposal);
    }

    @Transactional(readOnly = true)
    public ScoreProposalResponse own(String email, UUID activityId) {
        Student student = resolveStudent(email);
        Activity activity = resolveActivity(activityId);
        requireStudentAccess(student, activity);
        return proposalRepository.findByActivityIdAndStudentId(activityId, student.getId())
                .map(this::toResponse).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ScoreProposalResponse> listForTeacher(String email, UUID activityId) {
        Teacher teacher = resolveTeacher(email);
        Activity activity = resolveActivity(activityId);
        requireTeacherOwner(teacher, activity);
        return proposalRepository.findByActivityIdOrderBySubmittedAtAsc(activityId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public ScoreProposalResponse approve(String email, UUID proposalId,
            ReviewScoreProposalRequest request) {
        Teacher teacher = resolveTeacher(email);
        ScoreProposal proposal = resolveProposal(proposalId);
        requireTeacherOwner(teacher, proposal.getActivity());
        if (proposal.getStatus() != ScoreProposalStatus.PENDING) {
            throw ApiException.conflict("Only pending proposals can be reviewed.");
        }
        BigDecimal approved = request.approvedScore() == null
                ? proposal.getReportedScore() : request.approvedScore();
        validateScore(approved, proposal.getActivity());
        proposal.setApprovedScore(approved);
        proposal.setStatus(request.approvedScore() == null
                ? ScoreProposalStatus.APPROVED : ScoreProposalStatus.EDITED_APPROVED);
        proposal.setReviewedAt(Instant.now());
        proposal.setReviewedBy(teacher);
        writeAuthoritativeScore(proposal, approved);
        if (notificationService != null) {
            notificationService.notifyUser(proposal.getStudent().getUser(),
                    NotificationType.RELEASED_GRADE,
                    "Grade released for " + proposal.getActivity().getTitle(),
                    proposal.getActivity().getId().toString());
        }
        return toResponse(proposalRepository.save(proposal));
    }

    @Transactional
    public ScoreProposalResponse reject(String email, UUID proposalId) {
        Teacher teacher = resolveTeacher(email);
        ScoreProposal proposal = resolveProposal(proposalId);
        requireTeacherOwner(teacher, proposal.getActivity());
        if (proposal.getStatus() != ScoreProposalStatus.PENDING) {
            throw ApiException.conflict("Only pending proposals can be reviewed.");
        }
        proposal.setStatus(ScoreProposalStatus.REJECTED);
        proposal.setApprovedScore(null);
        proposal.setReviewedAt(Instant.now());
        proposal.setReviewedBy(teacher);
        return toResponse(proposalRepository.save(proposal));
    }

    private void writeAuthoritativeScore(ScoreProposal proposal, BigDecimal score) {
        ActivitySubmission submission = submissionRepository.findByActivityIdAndStudentId(
                        proposal.getActivity().getId(), proposal.getStudent().getId())
                .orElseGet(ActivitySubmission::new);
        submission.setActivity(proposal.getActivity());
        submission.setStudent(proposal.getStudent());
        submission.setScore(score);
        submission.setStatus(SubmissionStatus.GRADED);
        if (submission.getSubmittedAt() == null) submission.setSubmittedAt(proposal.getSubmittedAt());
        submissionRepository.save(submission);
    }

    private void validateScore(BigDecimal score, Activity activity) {
        if (score.signum() < 0 || score.compareTo(activity.getPerfectScore()) > 0) {
            throw ApiException.badRequest("Score must be between zero and the perfect score.");
        }
    }

    private void requireStudentAccess(Student student, Activity activity) {
        var link = activity.getClassSubjectLink();
        boolean approvedEnrollment = enrollmentRepository.findByStudentIdAndClassSectionId(
                        student.getId(), link.getClassSection().getId())
                .filter(value -> value.getStatus() == ClassEnrollmentRequestStatus.APPROVED)
                .isPresent();
        if (!approvedEnrollment || link.getStatus() != ClassSubjectLinkStatus.APPROVED
                || !activity.isAllowStudentSelfSubmissionScore()) {
            throw ApiException.forbidden("Self-submitted scores are not available for this activity.");
        }
    }

    private void requireTeacherOwner(Teacher teacher, Activity activity) {
        if (!activity.getClassSubjectLink().getSubject().getSubjectTeacher().getId()
                .equals(teacher.getId())) {
            throw ApiException.forbidden("You do not own this activity's subject.");
        }
    }

    private Activity resolveActivity(UUID id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Activity not found."));
    }

    private ScoreProposal resolveProposal(UUID id) {
        return proposalRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Score proposal not found."));
    }

    private Student resolveStudent(String email) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
        return studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> ApiException.forbidden("Only students can propose scores."));
    }

    private Teacher resolveTeacher(String email) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
        return teacherRepository.findByUserId(user.getId())
                .orElseThrow(() -> ApiException.forbidden("Only teachers can review scores."));
    }

    private ScoreProposalResponse toResponse(ScoreProposal proposal) {
        ActivityFile file = proposal.getProofFile();
        return new ScoreProposalResponse(proposal.getId(), proposal.getActivity().getId(),
                proposal.getStudent().getId(), proposal.getStudent().getName(),
                proposal.getStudent().getLrn(), proposal.getReportedScore(),
                proposal.getApprovedScore(), proposal.getActivity().getPerfectScore(),
                proposal.getStatus(), new ActivityFileResponse(file.getId(), file.getFileName(),
                file.getContentType(), file.getFileSizeBytes(), file.getCompletedAt()),
                proposal.getSubmittedAt(), proposal.getReviewedAt());
    }
}
