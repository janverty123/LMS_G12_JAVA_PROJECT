package com.apptitle.activity.service;

import com.apptitle.activity.dto.ActivityFileDownloadResponse;
import com.apptitle.activity.dto.ActivityFileResponse;
import com.apptitle.activity.dto.ActivityFileUploadRequest;
import com.apptitle.activity.dto.ActivityFileUploadResponse;
import com.apptitle.activity.dto.ActivitySubmissionResponse;
import com.apptitle.activity.dto.ScoreActivitySubmissionRequest;
import com.apptitle.activity.entity.Activity;
import com.apptitle.activity.entity.ActivityFile;
import com.apptitle.activity.entity.ActivityFileChunk;
import com.apptitle.activity.entity.ActivityFilePurpose;
import com.apptitle.activity.entity.ActivitySubmission;
import com.apptitle.activity.entity.SubmissionStatus;
import com.apptitle.activity.repository.ActivityFileRepository;
import com.apptitle.activity.repository.ActivityRepository;
import com.apptitle.activity.repository.ActivitySubmissionRepository;
import com.apptitle.classsection.entity.ClassEnrollmentRequest;
import com.apptitle.classsection.entity.ClassEnrollmentRequestStatus;
import com.apptitle.classsection.repository.ClassEnrollmentRequestRepository;
import com.apptitle.common.exception.ApiException;
import com.apptitle.config.MinioProperties;
import com.apptitle.material.dto.CompleteUploadRequest;
import com.apptitle.material.service.LearningMaterialService;
import com.apptitle.material.storage.MultipartStorage;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ActivitySubmissionService {

    private final ActivityRepository activityRepository;
    private final ActivityFileRepository fileRepository;
    private final ActivitySubmissionRepository submissionRepository;
    private final ClassEnrollmentRequestRepository enrollmentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final MultipartStorage multipartStorage;
    private final MinioProperties minioProperties;
    private NotificationService notificationService;

    @Autowired(required = false)
    void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public ActivitySubmissionService(
            ActivityRepository activityRepository,
            ActivityFileRepository fileRepository,
            ActivitySubmissionRepository submissionRepository,
            ClassEnrollmentRequestRepository enrollmentRepository,
            TeacherRepository teacherRepository,
            StudentRepository studentRepository,
            UserRepository userRepository,
            MultipartStorage multipartStorage,
            MinioProperties minioProperties
    ) {
        this.activityRepository = activityRepository;
        this.fileRepository = fileRepository;
        this.submissionRepository = submissionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.multipartStorage = multipartStorage;
        this.minioProperties = minioProperties;
    }

    @Transactional
    public ActivityFileUploadResponse initializeTeacherAttachment(
            String email, UUID activityId, ActivityFileUploadRequest request) {
        Teacher teacher = resolveTeacher(email);
        Activity activity = resolveActivity(activityId);
        requireTeacherOwner(teacher, activity);
        return initialize(activity, null, ActivityFilePurpose.TEACHER_ATTACHMENT, request);
    }

    @Transactional
    public ActivityFileUploadResponse initializeStudentSubmission(
            String email, UUID activityId, ActivityFileUploadRequest request) {
        Student student = resolveStudent(email);
        Activity activity = resolveActivity(activityId);
        requireStudentAccess(student, activity);
        return initialize(activity, student, ActivityFilePurpose.STUDENT_SUBMISSION, request);
    }

    @Transactional
    public ActivityFileUploadResponse initializeScoreProof(
            String email, UUID activityId, ActivityFileUploadRequest request) {
        Student student = resolveStudent(email);
        Activity activity = resolveActivity(activityId);
        requireStudentAccess(student, activity);
        if (!activity.isAllowStudentSelfSubmissionScore()) {
            throw ApiException.forbidden("Self-submitted scores are not enabled for this activity.");
        }
        return initialize(activity, student, ActivityFilePurpose.SCORE_PROOF, request);
    }

    @Transactional
    public ActivityFileResponse completeTeacherAttachment(
            String email, UUID fileId, CompleteUploadRequest request) {
        Teacher teacher = resolveTeacher(email);
        ActivityFile file = resolveFile(fileId);
        requirePurpose(file, ActivityFilePurpose.TEACHER_ATTACHMENT);
        requireTeacherOwner(teacher, file.getActivity());
        return complete(file, request);
    }

    @Transactional
    public ActivitySubmissionResponse completeStudentSubmission(
            String email, UUID fileId, CompleteUploadRequest request) {
        Student student = resolveStudent(email);
        ActivityFile file = resolveFile(fileId);
        requirePurpose(file, ActivityFilePurpose.STUDENT_SUBMISSION);
        if (!file.getStudent().getId().equals(student.getId())) {
            throw ApiException.forbidden("You can only complete your own upload.");
        }
        requireStudentAccess(student, file.getActivity());
        complete(file, request);

        Instant submittedAt = Instant.now();
        ActivitySubmission submission = submissionRepository
                .findByActivityIdAndStudentId(file.getActivity().getId(), student.getId())
                .orElseGet(ActivitySubmission::new);
        submission.setActivity(file.getActivity());
        submission.setStudent(student);
        submission.setFile(file);
        submission.setSubmittedAt(submittedAt);
        submission.setScore(null);
        submission.setStatus(submittedAt.isAfter(file.getActivity().getDeadline())
                ? SubmissionStatus.LATE : SubmissionStatus.SUBMITTED);
        return toResponse(submissionRepository.save(submission));
    }

    @Transactional
    public ActivityFileResponse completeScoreProof(
            String email, UUID fileId, CompleteUploadRequest request) {
        Student student = resolveStudent(email);
        ActivityFile file = resolveFile(fileId);
        requirePurpose(file, ActivityFilePurpose.SCORE_PROOF);
        if (!file.getStudent().getId().equals(student.getId())) {
            throw ApiException.forbidden("You can only complete your own proof upload.");
        }
        requireStudentAccess(student, file.getActivity());
        return complete(file, request);
    }

    @Transactional(readOnly = true)
    public List<ActivityFileResponse> listAttachments(String email, UUID activityId) {
        Activity activity = resolveActivity(activityId);
        if (isTeacher(email)) {
            requireTeacherOwner(resolveTeacher(email), activity);
        } else {
            requireStudentAccess(resolveStudent(email), activity);
        }
        return fileRepository.findByActivityIdAndPurposeAndCompletedTrue(
                        activityId, ActivityFilePurpose.TEACHER_ATTACHMENT)
                .stream().map(this::toFileResponse).toList();
    }

    @Transactional(readOnly = true)
    public ActivitySubmissionResponse getOwnSubmission(String email, UUID activityId) {
        Student student = resolveStudent(email);
        Activity activity = resolveActivity(activityId);
        requireStudentAccess(student, activity);
        return submissionRepository.findByActivityIdAndStudentId(activityId, student.getId())
                .map(this::toResponse)
                .orElseGet(() -> notSubmitted(student, activity));
    }

    @Transactional(readOnly = true)
    public List<ActivitySubmissionResponse> roster(String email, UUID activityId) {
        Teacher teacher = resolveTeacher(email);
        Activity activity = resolveActivity(activityId);
        requireTeacherOwner(teacher, activity);
        UUID classId = activity.getClassSubjectLink().getClassSection().getId();
        Map<UUID, ActivitySubmission> submitted = new HashMap<>();
        submissionRepository.findByActivityId(activityId)
                .forEach(value -> submitted.put(value.getStudent().getId(), value));
        return enrollmentRepository.findByClassSectionIdAndStatus(
                        classId, ClassEnrollmentRequestStatus.APPROVED)
                .stream()
                .map(ClassEnrollmentRequest::getStudent)
                .map(student -> submitted.containsKey(student.getId())
                        ? toResponse(submitted.get(student.getId()))
                        : notSubmitted(student, activity))
                .toList();
    }

    @Transactional
    public ActivitySubmissionResponse score(
            String email, UUID submissionId, ScoreActivitySubmissionRequest request) {
        Teacher teacher = resolveTeacher(email);
        ActivitySubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> ApiException.notFound("Submission not found."));
        requireTeacherOwner(teacher, submission.getActivity());
        if (request.score().signum() < 0
                || request.score().compareTo(submission.getActivity().getPerfectScore()) > 0) {
            throw ApiException.badRequest("Score must be between zero and the perfect score.");
        }
        submission.setScore(request.score());
        submission.setStatus(SubmissionStatus.GRADED);
        submission = submissionRepository.save(submission);
        if (notificationService != null) {
            notificationService.notifyUser(submission.getStudent().getUser(),
                    NotificationType.RELEASED_GRADE,
                    "Grade released for " + submission.getActivity().getTitle(),
                    submission.getActivity().getId().toString());
        }
        return toResponse(submission);
    }

    @Transactional(readOnly = true)
    public ActivityFileDownloadResponse download(String email, UUID fileId) {
        ActivityFile file = resolveCompletedFile(fileId);
        if (isTeacher(email)) {
            requireTeacherOwner(resolveTeacher(email), file.getActivity());
        } else {
            Student student = resolveStudent(email);
            requireStudentAccess(student, file.getActivity());
            if (file.getStudent() != null
                    && !file.getStudent().getId().equals(student.getId())) {
                throw ApiException.forbidden("You can only download your own student file.");
            }
        }
        return new ActivityFileDownloadResponse(file.getId(), file.getFileName(),
                file.getContentType(), file.getFileSizeBytes(),
                multipartStorage.presignDownload(file.getStorageKey()),
                Instant.now().plusSeconds(minioProperties.presignedExpirySeconds()));
    }

    private ActivityFileUploadResponse initialize(Activity activity, Student student,
            ActivityFilePurpose purpose, ActivityFileUploadRequest request) {
        if (request.totalSizeBytes() <= 0) {
            throw ApiException.badRequest("File size must be greater than zero.");
        }
        if (request.totalSizeBytes() > minioProperties.maxFileSizeBytes()) {
            throw ApiException.badRequest("File exceeds the maximum allowed size.");
        }
        String name = leafName(request.fileName());
        long chunkSize = LearningMaterialService.CHUNK_SIZE_BYTES;
        int totalParts = Math.toIntExact((request.totalSizeBytes() + chunkSize - 1) / chunkSize);
        String key = "activities/%s/%s/%s/%s".formatted(activity.getId(),
                purpose.name().toLowerCase(), UUID.randomUUID(),
                name.replaceAll("[^A-Za-z0-9._-]", "_"));
        String uploadId = multipartStorage.initiateUpload(key);
        ActivityFile file = new ActivityFile();
        file.setActivity(activity);
        file.setStudent(student);
        file.setPurpose(purpose);
        file.setFileName(name);
        file.setContentType(request.contentType().trim());
        file.setStorageKey(key);
        file.setFileSizeBytes(request.totalSizeBytes());
        file.setUploadId(uploadId);
        file.setChunkSizeBytes(chunkSize);
        file.setTotalParts(totalParts);
        file = fileRepository.save(file);
        var urls = multipartStorage.presignUploadParts(key, uploadId, totalParts)
                .stream().map(part -> new ActivityFileUploadResponse.PresignedPartUrl(
                        part.partNumber(), part.url())).toList();
        return new ActivityFileUploadResponse(file.getId(), uploadId, key,
                chunkSize, totalParts, urls);
    }

    private ActivityFileResponse complete(ActivityFile file, CompleteUploadRequest request) {
        if (file.isCompleted()) throw ApiException.conflict("Upload is already complete.");
        if (!file.getUploadId().equals(request.uploadId())
                || !file.getStorageKey().equals(request.fileKey())) {
            throw ApiException.badRequest("Upload ID or file key does not match.");
        }
        var parts = validateParts(request.parts(), file.getTotalParts());
        multipartStorage.completeUpload(file.getStorageKey(), file.getUploadId(),
                parts.stream().map(part -> new MultipartStorage.CompletedPart(
                        part.partNumber(), part.eTag().trim())).toList());
        for (var part : parts) {
            ActivityFileChunk chunk = new ActivityFileChunk();
            chunk.setActivityFile(file);
            chunk.setPartNumber(part.partNumber());
            chunk.setETag(part.eTag().trim());
            chunk.setSizeBytes(part.partNumber() < file.getTotalParts()
                    ? file.getChunkSizeBytes()
                    : file.getFileSizeBytes() - file.getChunkSizeBytes()
                    * (file.getTotalParts() - 1L));
            file.getChunks().add(chunk);
        }
        file.setCompleted(true);
        file.setCompletedAt(Instant.now());
        return toFileResponse(fileRepository.save(file));
    }

    private List<CompleteUploadRequest.CompletedPart> validateParts(
            List<CompleteUploadRequest.CompletedPart> values, int expected) {
        if (values.size() != expected) throw ApiException.badRequest("Part count does not match.");
        List<CompleteUploadRequest.CompletedPart> parts = new ArrayList<>(values);
        parts.sort(Comparator.comparingInt(CompleteUploadRequest.CompletedPart::partNumber));
        Set<Integer> seen = new HashSet<>();
        for (int index = 0; index < parts.size(); index++) {
            var part = parts.get(index);
            if (part.partNumber() != index + 1 || !seen.add(part.partNumber())
                    || part.eTag() == null || part.eTag().isBlank()) {
                throw ApiException.badRequest("Parts must be consecutive with valid ETags.");
            }
        }
        return parts;
    }

    private void requireStudentAccess(Student student, Activity activity) {
        var link = activity.getClassSubjectLink();
        if (link.getStatus() != ClassSubjectLinkStatus.APPROVED
                || !enrollmentRepository.existsByStudentIdAndClassSectionId(
                        student.getId(), link.getClassSection().getId())
                || enrollmentRepository.findByStudentIdAndClassSectionId(
                        student.getId(), link.getClassSection().getId())
                        .filter(value -> value.getStatus() == ClassEnrollmentRequestStatus.APPROVED)
                        .isEmpty()) {
            throw ApiException.forbidden("You do not have access to this activity.");
        }
    }

    private void requireTeacherOwner(Teacher teacher, Activity activity) {
        if (!activity.getClassSubjectLink().getSubject().getSubjectTeacher()
                .getId().equals(teacher.getId())) {
            throw ApiException.forbidden("You do not own this activity's subject.");
        }
    }

    private boolean isTeacher(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(user -> user.getRole().name().equals("TEACHER")).orElse(false);
    }

    private Activity resolveActivity(UUID id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Activity not found."));
    }

    private ActivityFile resolveFile(UUID id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Activity file not found."));
    }

    private ActivityFile resolveCompletedFile(UUID id) {
        ActivityFile file = resolveFile(id);
        if (!file.isCompleted()) throw ApiException.notFound("Activity file is not available.");
        return file;
    }

    private Teacher resolveTeacher(String email) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
        return teacherRepository.findByUserId(user.getId())
                .orElseThrow(() -> ApiException.forbidden("Only teachers can access this resource."));
    }

    private Student resolveStudent(String email) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
        return studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> ApiException.forbidden("Only students can access this resource."));
    }

    private void requirePurpose(ActivityFile file, ActivityFilePurpose purpose) {
        if (file.getPurpose() != purpose) throw ApiException.forbidden("Invalid upload purpose.");
    }

    private String leafName(String supplied) {
        String normalized = supplied.trim().replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1);
        if (name.isBlank()) throw ApiException.badRequest("File name is invalid.");
        return name;
    }

    private ActivityFileResponse toFileResponse(ActivityFile file) {
        return new ActivityFileResponse(file.getId(), file.getFileName(),
                file.getContentType(), file.getFileSizeBytes(), file.getCompletedAt());
    }

    private ActivitySubmissionResponse toResponse(ActivitySubmission submission) {
        return new ActivitySubmissionResponse(submission.getId(), submission.getStudent().getId(),
                submission.getStudent().getName(), submission.getStudent().getLrn(),
                submission.getStatus(), submission.getSubmittedAt(),
                submission.getFile() == null ? null : toFileResponse(submission.getFile()),
                submission.getScore(), submission.getActivity().getPerfectScore());
    }

    private ActivitySubmissionResponse notSubmitted(Student student, Activity activity) {
        return new ActivitySubmissionResponse(null, student.getId(), student.getName(),
                student.getLrn(), SubmissionStatus.NOT_SUBMITTED, null, null, null,
                activity.getPerfectScore());
    }
}
