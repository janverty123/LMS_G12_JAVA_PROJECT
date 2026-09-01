package com.apptitle.activity.service;

import com.apptitle.activity.dto.ScoreActivitySubmissionRequest;
import com.apptitle.activity.entity.Activity;
import com.apptitle.activity.entity.ActivityFile;
import com.apptitle.activity.entity.ActivityFilePurpose;
import com.apptitle.activity.entity.ActivitySubmission;
import com.apptitle.activity.entity.ActivityType;
import com.apptitle.activity.entity.SubmissionStatus;
import com.apptitle.activity.repository.ActivityFileRepository;
import com.apptitle.activity.repository.ActivityRepository;
import com.apptitle.activity.repository.ActivitySubmissionRepository;
import com.apptitle.classsection.entity.ClassEnrollmentRequest;
import com.apptitle.classsection.entity.ClassEnrollmentRequestStatus;
import com.apptitle.classsection.entity.ClassSection;
import com.apptitle.classsection.repository.ClassEnrollmentRequestRepository;
import com.apptitle.common.entity.BaseEntity;
import com.apptitle.common.exception.ApiException;
import com.apptitle.config.MinioProperties;
import com.apptitle.material.dto.CompleteUploadRequest;
import com.apptitle.material.storage.MultipartStorage;
import com.apptitle.student.entity.Student;
import com.apptitle.student.repository.StudentRepository;
import com.apptitle.subject.entity.ClassSubjectLink;
import com.apptitle.subject.entity.ClassSubjectLinkStatus;
import com.apptitle.subject.entity.Subject;
import com.apptitle.teacher.entity.Teacher;
import com.apptitle.teacher.repository.TeacherRepository;
import com.apptitle.user.entity.Role;
import com.apptitle.user.entity.User;
import com.apptitle.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ActivitySubmissionServiceTest {

    @Mock private ActivityRepository activityRepository;
    @Mock private ActivityFileRepository fileRepository;
    @Mock private ActivitySubmissionRepository submissionRepository;
    @Mock private ClassEnrollmentRequestRepository enrollmentRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private UserRepository userRepository;
    @Mock private MultipartStorage storage;

    private ActivitySubmissionService service;
    private Teacher teacher;
    private Student student;
    private Student otherStudent;
    private Activity activity;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ActivitySubmissionService(activityRepository, fileRepository,
                submissionRepository, enrollmentRepository, teacherRepository,
                studentRepository, userRepository, storage,
                new MinioProperties("http://localhost:9000", "us-east-1", "key", "secret",
                        "test", 900, 500L * 1024 * 1024));
        teacher = teacher("teacher@example.com");
        student = student("student@example.com");
        otherStudent = student("other-student@example.com");
        activity = activity();
        when(activityRepository.findById(activity.getId())).thenReturn(Optional.of(activity));
        approve(student);
        approve(otherStudent);
        when(fileRepository.save(any(ActivityFile.class))).thenAnswer(invocation -> {
            ActivityFile file = invocation.getArgument(0);
            if (file.getId() == null) id(file);
            return file;
        });
        when(submissionRepository.save(any(ActivitySubmission.class))).thenAnswer(invocation -> {
            ActivitySubmission submission = invocation.getArgument(0);
            if (submission.getId() == null) id(submission);
            return submission;
        });
    }

    @Test
    void completeStudentSubmission_marksLateAfterDeadline() {
        activity.setDeadline(Instant.now().minusSeconds(60));
        ActivityFile file = pendingFile(student);
        when(fileRepository.findById(file.getId())).thenReturn(Optional.of(file));

        var response = service.completeStudentSubmission("student@example.com", file.getId(),
                completeRequest(file));

        assertEquals(SubmissionStatus.LATE, response.status());
    }

    @Test
    void studentCannotDownloadAnotherStudentsSubmission() {
        ActivityFile file = pendingFile(otherStudent);
        file.setCompleted(true);
        when(fileRepository.findById(file.getId())).thenReturn(Optional.of(file));

        ApiException exception = assertThrows(ApiException.class, () ->
                service.download("student@example.com", file.getId()));

        assertEquals(403, exception.getStatus().value());
    }

    @Test
    void studentCannotDownloadAnotherStudentsScoreProof() {
        ActivityFile file = pendingFile(otherStudent);
        file.setPurpose(ActivityFilePurpose.SCORE_PROOF);
        file.setCompleted(true);
        when(fileRepository.findById(file.getId())).thenReturn(Optional.of(file));

        ApiException exception = assertThrows(ApiException.class, () ->
                service.download("student@example.com", file.getId()));

        assertEquals(403, exception.getStatus().value());
    }

    @Test
    void studentCannotDownloadFileWhenSubjectLinkIsNoLongerApproved() {
        ActivityFile file = pendingFile(student);
        file.setCompleted(true);
        activity.getClassSubjectLink().setStatus(ClassSubjectLinkStatus.DECLINED);
        when(fileRepository.findById(file.getId())).thenReturn(Optional.of(file));

        ApiException exception = assertThrows(ApiException.class, () ->
                service.download("student@example.com", file.getId()));

        assertEquals(403, exception.getStatus().value());
    }

    @Test
    void initializeSubmissionRejectsEmptyFileBeforeStartingMultipartUpload() {
        ApiException exception = assertThrows(ApiException.class, () ->
                service.initializeStudentSubmission("student@example.com", activity.getId(),
                        new com.apptitle.activity.dto.ActivityFileUploadRequest(
                                "empty.pdf", 0, "application/pdf")));

        assertEquals(400, exception.getStatus().value());
    }

    @Test
    void scoreRejectsNegativeAndAbovePerfectScore() {
        ActivitySubmission submission = submission(student, pendingFile(student));
        when(submissionRepository.findById(submission.getId())).thenReturn(Optional.of(submission));

        assertEquals(400, assertThrows(ApiException.class, () -> service.score(
                "teacher@example.com", submission.getId(),
                new ScoreActivitySubmissionRequest(new BigDecimal("-1"))))
                .getStatus().value());
        assertEquals(400, assertThrows(ApiException.class, () -> service.score(
                "teacher@example.com", submission.getId(),
                new ScoreActivitySubmissionRequest(new BigDecimal("101"))))
                .getStatus().value());
    }

    @Test
    void resubmissionReplacesFileAndClearsPreviousGrade() {
        ActivityFile oldFile = pendingFile(student);
        ActivitySubmission existing = submission(student, oldFile);
        existing.setScore(new BigDecimal("80"));
        existing.setStatus(SubmissionStatus.GRADED);
        ActivityFile newFile = pendingFile(student);
        when(fileRepository.findById(newFile.getId())).thenReturn(Optional.of(newFile));
        when(submissionRepository.findByActivityIdAndStudentId(
                activity.getId(), student.getId())).thenReturn(Optional.of(existing));

        var response = service.completeStudentSubmission("student@example.com", newFile.getId(),
                completeRequest(newFile));

        assertEquals(newFile.getId(), response.file().id());
        assertEquals(null, response.score());
        assertEquals(SubmissionStatus.SUBMITTED, response.status());
    }

    @Test
    void studentCannotBypassApprovalAndWriteAuthoritativeScore() {
        ActivitySubmission submission = submission(student, pendingFile(student));
        when(submissionRepository.findById(submission.getId())).thenReturn(Optional.of(submission));

        ApiException exception = assertThrows(ApiException.class, () -> service.score(
                "student@example.com", submission.getId(),
                new ScoreActivitySubmissionRequest(new BigDecimal("90"))));

        assertEquals(403, exception.getStatus().value());
    }

    private CompleteUploadRequest completeRequest(ActivityFile file) {
        return new CompleteUploadRequest(file.getUploadId(), file.getStorageKey(),
                List.of(new CompleteUploadRequest.CompletedPart(1, "etag")));
    }

    private ActivityFile pendingFile(Student owner) {
        ActivityFile file = new ActivityFile();
        id(file);
        file.setActivity(activity);
        file.setStudent(owner);
        file.setPurpose(ActivityFilePurpose.STUDENT_SUBMISSION);
        file.setFileName("work.pdf");
        file.setContentType("application/pdf");
        file.setStorageKey("activities/" + UUID.randomUUID());
        file.setUploadId(UUID.randomUUID().toString());
        file.setFileSizeBytes(100);
        file.setChunkSizeBytes(5L * 1024 * 1024);
        file.setTotalParts(1);
        return file;
    }

    private ActivitySubmission submission(Student owner, ActivityFile file) {
        ActivitySubmission submission = new ActivitySubmission();
        id(submission);
        submission.setActivity(activity);
        submission.setStudent(owner);
        submission.setFile(file);
        submission.setSubmittedAt(Instant.now());
        submission.setStatus(SubmissionStatus.SUBMITTED);
        return submission;
    }

    private Activity activity() {
        ClassSection section = new ClassSection();
        id(section);
        Subject subject = new Subject();
        id(subject);
        subject.setSubjectTeacher(teacher);
        ClassSubjectLink link = new ClassSubjectLink();
        id(link);
        link.setClassSection(section);
        link.setSubject(subject);
        link.setStatus(ClassSubjectLinkStatus.APPROVED);
        Activity value = new Activity();
        id(value);
        value.setClassSubjectLink(link);
        value.setCreatedBy(teacher);
        value.setType(ActivityType.TEST);
        value.setTitle("Test");
        value.setPerfectScore(new BigDecimal("100"));
        value.setDeadline(Instant.now().plusSeconds(3600));
        value.setInstructions("Answer all items.");
        return value;
    }

    private void approve(Student value) {
        ClassEnrollmentRequest enrollment = new ClassEnrollmentRequest();
        id(enrollment);
        enrollment.setStudent(value);
        enrollment.setClassSection(activity.getClassSubjectLink().getClassSection());
        enrollment.setStatus(ClassEnrollmentRequestStatus.APPROVED);
        UUID classId = activity.getClassSubjectLink().getClassSection().getId();
        when(enrollmentRepository.existsByStudentIdAndClassSectionId(value.getId(), classId))
                .thenReturn(true);
        when(enrollmentRepository.findByStudentIdAndClassSectionId(value.getId(), classId))
                .thenReturn(Optional.of(enrollment));
    }

    private Teacher teacher(String email) {
        User user = user(email, Role.TEACHER);
        Teacher value = new Teacher();
        id(value);
        value.setUser(user);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(teacherRepository.findByUserId(user.getId())).thenReturn(Optional.of(value));
        return value;
    }

    private Student student(String email) {
        User user = user(email, Role.STUDENT);
        Student value = new Student();
        id(value);
        value.setUser(user);
        value.setName(email);
        value.setLrn(UUID.randomUUID().toString());
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(studentRepository.findByUserId(user.getId())).thenReturn(Optional.of(value));
        return value;
    }

    private User user(String email, Role role) {
        User value = new User();
        id(value);
        value.setEmail(email);
        value.setRole(role);
        return value;
    }

    private void id(BaseEntity entity) {
        entity.setId(UUID.randomUUID());
    }
}
