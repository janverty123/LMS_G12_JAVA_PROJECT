package com.apptitle.progress.service;

import com.apptitle.activity.entity.Activity;
import com.apptitle.activity.entity.ActivitySubmission;
import com.apptitle.activity.repository.ActivityRepository;
import com.apptitle.activity.repository.ActivitySubmissionRepository;
import com.apptitle.classsection.entity.ClassEnrollmentRequestStatus;
import com.apptitle.classsection.repository.ClassEnrollmentRequestRepository;
import com.apptitle.common.exception.ApiException;
import com.apptitle.grade.service.GradebookService;
import com.apptitle.progress.dto.ProgressConfigurationRequest;
import com.apptitle.progress.dto.StudentProgressResponse;
import com.apptitle.progress.entity.ProgressConfiguration;
import com.apptitle.progress.entity.ProgressStatus;
import com.apptitle.progress.repository.ProgressConfigurationRepository;
import com.apptitle.student.entity.Student;
import com.apptitle.student.repository.StudentRepository;
import com.apptitle.subject.entity.ClassSubjectLink;
import com.apptitle.subject.entity.ClassSubjectLinkStatus;
import com.apptitle.subject.repository.ClassSubjectLinkRepository;
import com.apptitle.teacher.repository.TeacherRepository;
import com.apptitle.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class ProgressService {
    private final ProgressConfigurationRepository configurationRepository;
    private final ActivityRepository activityRepository;
    private final ActivitySubmissionRepository submissionRepository;
    private final ClassSubjectLinkRepository linkRepository;
    private final ClassEnrollmentRequestRepository enrollmentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final GradebookService gradebookService;

    public ProgressService(ProgressConfigurationRepository configurationRepository,
            ActivityRepository activityRepository,
            ActivitySubmissionRepository submissionRepository,
            ClassSubjectLinkRepository linkRepository,
            ClassEnrollmentRequestRepository enrollmentRepository,
            TeacherRepository teacherRepository, StudentRepository studentRepository,
            UserRepository userRepository, GradebookService gradebookService) {
        this.configurationRepository = configurationRepository;
        this.activityRepository = activityRepository;
        this.submissionRepository = submissionRepository;
        this.linkRepository = linkRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.gradebookService = gradebookService;
    }

    @Transactional
    public void configure(String email, UUID classId, UUID subjectId,
            ProgressConfigurationRequest request) {
        ClassSubjectLink link = teacherLink(email, classId, subjectId);
        if (request.needsAttentionMinimum().compareTo(request.onTrackMinimum()) >= 0) {
            throw ApiException.badRequest(
                    "Needs Attention minimum must be lower than On Track minimum.");
        }
        ProgressConfiguration config = configurationRepository.findByClassSubjectLinkId(link.getId())
                .orElseGet(ProgressConfiguration::new);
        config.setClassSubjectLink(link);
        config.setOnTrackMinimum(request.onTrackMinimum());
        config.setNeedsAttentionMinimum(request.needsAttentionMinimum());
        configurationRepository.save(config);
    }

    @Transactional(readOnly = true)
    public List<StudentProgressResponse> dashboard(String email, UUID classId, UUID subjectId) {
        ClassSubjectLink link = teacherLink(email, classId, subjectId);
        ProgressConfiguration config = config(link);
        var grades = gradebookService.teacherGradebook(email, classId, subjectId);
        List<Activity> activities = activityRepository
                .findByClassSubjectLinkIdOrderByDeadlineAsc(link.getId());
        return grades.students().stream().map(grade -> progress(
                grade.studentId(), grade.studentName(), grade.finalGrade(), activities, config)).toList();
    }

    @Transactional(readOnly = true)
    public StudentProgressResponse own(String email, UUID subjectId) {
        Student student = resolveStudent(email);
        var enrollment = enrollmentRepository.findByStudentIdAndStatus(
                        student.getId(), ClassEnrollmentRequestStatus.APPROVED)
                .stream().findFirst().orElseThrow(() -> ApiException.forbidden(
                        "You need an approved enrollment to view progress."));
        UUID classId = enrollment.getClassSection().getId();
        ClassSubjectLink link = approvedLink(classId, subjectId);
        ProgressConfiguration config = config(link);
        var grade = gradebookService.studentGrades(email, subjectId);
        return progress(student.getId(), student.getName(), grade.finalGrade(),
                activityRepository.findByClassSubjectLinkIdOrderByDeadlineAsc(link.getId()), config);
    }

    private StudentProgressResponse progress(UUID studentId, String name, BigDecimal grade,
            List<Activity> activities, ProgressConfiguration config) {
        List<String> missing = activities.stream()
                .filter(activity -> submissionRepository
                        .findByActivityIdAndStudentId(activity.getId(), studentId)
                        .map(ActivitySubmission::getFile).isEmpty())
                .map(Activity::getTitle).toList();
        BigDecimal completion = activities.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(activities.size() - missing.size())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(activities.size()), 2, RoundingMode.HALF_UP);
        ProgressStatus status = completion.compareTo(config.getOnTrackMinimum()) >= 0
                ? ProgressStatus.ON_TRACK
                : completion.compareTo(config.getNeedsAttentionMinimum()) >= 0
                ? ProgressStatus.NEEDS_ATTENTION : ProgressStatus.AT_RISK;
        return new StudentProgressResponse(studentId, name, completion, missing.size(),
                missing, grade, status, status.getLabel());
    }

    private ProgressConfiguration config(ClassSubjectLink link) {
        return configurationRepository.findByClassSubjectLinkId(link.getId())
                .orElseThrow(() -> ApiException.conflict(
                        "Configure progress status thresholds first."));
    }

    private ClassSubjectLink teacherLink(String email, UUID classId, UUID subjectId) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
        var teacher = teacherRepository.findByUserId(user.getId())
                .orElseThrow(() -> ApiException.forbidden("Only teachers can view class progress."));
        ClassSubjectLink link = approvedLink(classId, subjectId);
        if (!link.getSubject().getSubjectTeacher().getId().equals(teacher.getId())) {
            throw ApiException.forbidden("You do not own this subject.");
        }
        return link;
    }

    private ClassSubjectLink approvedLink(UUID classId, UUID subjectId) {
        ClassSubjectLink link = linkRepository.findByClassSectionIdAndSubjectId(classId, subjectId)
                .orElseThrow(() -> ApiException.notFound("Class-subject link not found."));
        if (link.getStatus() != ClassSubjectLinkStatus.APPROVED) {
            throw ApiException.forbidden("The subject link is not approved.");
        }
        return link;
    }

    private Student resolveStudent(String email) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
        return studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> ApiException.forbidden("Only students can view student progress."));
    }
}
