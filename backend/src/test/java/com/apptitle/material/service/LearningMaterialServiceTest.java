package com.apptitle.material.service;

import com.apptitle.classsection.entity.ClassEnrollmentRequest;
import com.apptitle.classsection.entity.ClassEnrollmentRequestStatus;
import com.apptitle.classsection.entity.ClassSection;
import com.apptitle.classsection.repository.ClassEnrollmentRequestRepository;
import com.apptitle.common.entity.BaseEntity;
import com.apptitle.common.exception.ApiException;
import com.apptitle.config.MinioProperties;
import com.apptitle.material.dto.CompleteUploadRequest;
import com.apptitle.material.dto.InitUploadRequest;
import com.apptitle.material.dto.InitUploadResponse;
import com.apptitle.material.dto.LearningMaterialResponse;
import com.apptitle.material.entity.LearningMaterial;
import com.apptitle.material.entity.LearningMaterialStatus;
import com.apptitle.material.repository.LearningMaterialRepository;
import com.apptitle.material.storage.MultipartStorage;
import com.apptitle.student.entity.Student;
import com.apptitle.student.repository.StudentRepository;
import com.apptitle.subject.entity.ClassSubjectLink;
import com.apptitle.subject.entity.ClassSubjectLinkStatus;
import com.apptitle.subject.entity.Subject;
import com.apptitle.subject.repository.ClassSubjectLinkRepository;
import com.apptitle.teacher.entity.Teacher;
import com.apptitle.teacher.repository.TeacherRepository;
import com.apptitle.user.entity.Role;
import com.apptitle.user.entity.User;
import com.apptitle.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LearningMaterialServiceTest {

    @Mock private LearningMaterialRepository learningMaterialRepository;
    @Mock private ClassSubjectLinkRepository classSubjectLinkRepository;
    @Mock private ClassEnrollmentRequestRepository classEnrollmentRequestRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private UserRepository userRepository;
    @Mock private MultipartStorage multipartStorage;

    private LearningMaterialService service;
    private Teacher subjectTeacher;
    private Teacher otherTeacher;
    private Student student;
    private ClassSection classSection;
    private Subject subject;
    private ClassSubjectLink approvedLink;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new LearningMaterialService(
                learningMaterialRepository,
                classSubjectLinkRepository,
                classEnrollmentRequestRepository,
                teacherRepository,
                studentRepository,
                userRepository,
                multipartStorage,
                new MinioProperties(
                        "http://localhost:9000",
                        "us-east-1",
                        "test",
                        "test",
                        "materials-test",
                        900,
                        500L * 1024 * 1024
                )
        );

        subjectTeacher = teacher("subject@example.com", "Subject Teacher");
        otherTeacher = teacher("other@example.com", "Other Teacher");
        student = student("student@example.com", "Student");

        classSection = new ClassSection();
        setId(classSection);
        classSection.setAdviser(otherTeacher);
        classSection.setGradeLevel("Grade 12");
        classSection.setSection("Integrity");
        classSection.setSchoolYear("2026-2027");
        classSection.setClassCode("ABC123");

        subject = new Subject();
        setId(subject);
        subject.setSubjectTeacher(subjectTeacher);
        subject.setName("Physics");
        subject.setSubjectCode("PHYS123");

        approvedLink = new ClassSubjectLink();
        setId(approvedLink);
        approvedLink.setClassSection(classSection);
        approvedLink.setSubject(subject);
        approvedLink.setRequestedBy(otherTeacher);
        approvedLink.setStatus(ClassSubjectLinkStatus.APPROVED);

        when(classSubjectLinkRepository.findByClassSectionIdAndSubjectId(
                classSection.getId(), subject.getId()))
                .thenReturn(Optional.of(approvedLink));
        when(learningMaterialRepository.save(any(LearningMaterial.class)))
                .thenAnswer(invocation -> {
                    LearningMaterial material = invocation.getArgument(0);
                    if (material.getId() == null) {
                        setId(material);
                        material.setCreatedAt(Instant.now());
                        material.setUpdatedAt(Instant.now());
                    }
                    return material;
                });
    }

    @Test
    void initUpload_createsMultipartUploadAndPresignedParts() {
        long fileSize = LearningMaterialService.CHUNK_SIZE_BYTES + 1024;
        when(multipartStorage.initiateUpload(anyString())).thenReturn("upload-1");
        when(multipartStorage.presignUploadParts(anyString(), anyString(), anyInt()))
                .thenReturn(List.of(
                        new MultipartStorage.PresignedPart(1, "https://minio/part-1"),
                        new MultipartStorage.PresignedPart(2, "https://minio/part-2")
                ));

        InitUploadResponse response = service.initUpload(
                "subject@example.com",
                classSection.getId(),
                subject.getId(),
                new InitUploadRequest(
                        "Lesson slides",
                        "Week one",
                        "lesson.pdf",
                        fileSize,
                        "application/pdf"
                )
        );

        assertEquals("upload-1", response.uploadId());
        assertEquals(2, response.totalParts());
        assertEquals(2, response.presignedUrls().size());
        assertTrue(response.fileKey().startsWith("materials/"));
    }

    @Test
    void completeUpload_finalizesPartsAndPersistsMetadata() {
        LearningMaterial material = initializedMaterial();
        when(learningMaterialRepository.findById(material.getId()))
                .thenReturn(Optional.of(material));

        LearningMaterialResponse response = service.completeUpload(
                "subject@example.com",
                material.getId(),
                new CompleteUploadRequest(
                        material.getUploadId(),
                        material.getStorageKey(),
                        List.of(
                                new CompleteUploadRequest.CompletedPart(2, "etag-2"),
                                new CompleteUploadRequest.CompletedPart(1, "etag-1")
                        )
                )
        );

        assertEquals(material.getId(), response.id());
        assertEquals(LearningMaterialStatus.COMPLETED, material.getStatus());
        assertEquals(2, material.getChunks().size());
        assertEquals(1024, material.getChunks().get(1).getSizeBytes());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MultipartStorage.CompletedPart>> partsCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(multipartStorage).completeUpload(
                anyString(), anyString(), partsCaptor.capture());
        assertEquals(1, partsCaptor.getValue().get(0).partNumber());
    }

    @Test
    void initUpload_rejectsTeacherWhoDoesNotOwnSubject() {
        ApiException exception = assertThrows(ApiException.class, () ->
                service.initUpload(
                        "other@example.com",
                        classSection.getId(),
                        subject.getId(),
                        validRequest()
                ));

        assertEquals(403, exception.getStatus().value());
        verify(multipartStorage, never()).initiateUpload(anyString());
    }

    @Test
    void listForStudent_returnsCompletedMaterialsForApprovedClass() {
        ClassEnrollmentRequest enrollment = approvedEnrollment();
        LearningMaterial material = completedMaterial();
        when(classEnrollmentRequestRepository.findByStudentIdAndStatus(
                student.getId(), ClassEnrollmentRequestStatus.APPROVED))
                .thenReturn(List.of(enrollment));
        when(learningMaterialRepository
                .findByClassSubjectLinkIdAndStatusOrderByCreatedAtDesc(
                        approvedLink.getId(), LearningMaterialStatus.COMPLETED))
                .thenReturn(List.of(material));

        List<LearningMaterialResponse> response = service.listForStudent(
                "student@example.com", subject.getId());

        assertEquals(1, response.size());
        assertEquals("Lesson slides", response.get(0).title());
    }

    @Test
    void listForStudent_rejectsStudentWithoutApprovedEnrollment() {
        when(classEnrollmentRequestRepository.findByStudentIdAndStatus(
                student.getId(), ClassEnrollmentRequestStatus.APPROVED))
                .thenReturn(List.of());

        ApiException exception = assertThrows(ApiException.class, () ->
                service.listForStudent("student@example.com", subject.getId()));

        assertEquals(403, exception.getStatus().value());
    }

    @Test
    void completeUpload_rejectsMissingMaterial() {
        UUID missingId = UUID.randomUUID();
        when(learningMaterialRepository.findById(missingId)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () ->
                service.completeUpload(
                        "subject@example.com",
                        missingId,
                        new CompleteUploadRequest(
                                "missing-upload",
                                "missing-key",
                                List.of(new CompleteUploadRequest.CompletedPart(1, "etag"))
                        )
                ));

        assertEquals(404, exception.getStatus().value());
    }

    @Test
    void initUpload_rejectsMissingClassSubjectLink() {
        UUID missingSubjectId = UUID.randomUUID();

        ApiException exception = assertThrows(ApiException.class, () ->
                service.initUpload(
                        "subject@example.com",
                        classSection.getId(),
                        missingSubjectId,
                        validRequest()
                ));

        assertEquals(404, exception.getStatus().value());
    }

    @Test
    void initUpload_rejectsUnsupportedFileType() {
        ApiException exception = assertThrows(ApiException.class, () ->
                service.initUpload(
                        "subject@example.com",
                        classSection.getId(),
                        subject.getId(),
                        new InitUploadRequest(
                                "Executable",
                                null,
                                "unsafe.exe",
                                1024,
                                "application/octet-stream"
                        )
                ));

        assertEquals(400, exception.getStatus().value());
        verify(multipartStorage, never()).initiateUpload(anyString());
    }

    @Test
    void initUpload_rejectsOversizedFile() {
        ApiException exception = assertThrows(ApiException.class, () ->
                service.initUpload(
                        "subject@example.com",
                        classSection.getId(),
                        subject.getId(),
                        new InitUploadRequest(
                                "Large file",
                                null,
                                "large.pdf",
                                501L * 1024 * 1024,
                                "application/pdf"
                        )
                ));

        assertEquals(400, exception.getStatus().value());
        verify(multipartStorage, never()).initiateUpload(anyString());
    }

    @Test
    void completeUpload_rejectsDuplicatePartNumbers() {
        LearningMaterial material = initializedMaterial();
        when(learningMaterialRepository.findById(material.getId()))
                .thenReturn(Optional.of(material));

        ApiException exception = assertThrows(ApiException.class, () ->
                service.completeUpload(
                        "subject@example.com",
                        material.getId(),
                        new CompleteUploadRequest(
                                material.getUploadId(),
                                material.getStorageKey(),
                                List.of(
                                        new CompleteUploadRequest.CompletedPart(1, "etag-1"),
                                        new CompleteUploadRequest.CompletedPart(1, "etag-duplicate")
                                )
                        )
                ));

        assertEquals(400, exception.getStatus().value());
        verify(multipartStorage, never()).completeUpload(anyString(), anyString(), any());
    }

    @Test
    void completeUpload_rejectsMissingETag() {
        LearningMaterial material = initializedMaterial();
        when(learningMaterialRepository.findById(material.getId()))
                .thenReturn(Optional.of(material));

        ApiException exception = assertThrows(ApiException.class, () ->
                service.completeUpload(
                        "subject@example.com",
                        material.getId(),
                        new CompleteUploadRequest(
                                material.getUploadId(),
                                material.getStorageKey(),
                                List.of(
                                        new CompleteUploadRequest.CompletedPart(1, "etag-1"),
                                        new CompleteUploadRequest.CompletedPart(2, " ")
                                )
                        )
                ));

        assertEquals(400, exception.getStatus().value());
        verify(multipartStorage, never()).completeUpload(anyString(), anyString(), any());
    }

    @Test
    void completeUpload_rejectsNonOwner() {
        LearningMaterial material = initializedMaterial();
        when(learningMaterialRepository.findById(material.getId()))
                .thenReturn(Optional.of(material));

        ApiException exception = assertThrows(ApiException.class, () ->
                service.completeUpload(
                        "other@example.com",
                        material.getId(),
                        new CompleteUploadRequest(
                                material.getUploadId(),
                                material.getStorageKey(),
                                List.of(
                                        new CompleteUploadRequest.CompletedPart(1, "etag-1"),
                                        new CompleteUploadRequest.CompletedPart(2, "etag-2")
                                )
                        )
                ));

        assertEquals(403, exception.getStatus().value());
        verify(multipartStorage, never()).completeUpload(anyString(), anyString(), any());
    }

    @Test
    void getStudentDownload_rejectsMaterialFromAnotherClass() {
        ClassSection otherClass = new ClassSection();
        setId(otherClass);
        ClassEnrollmentRequest enrollment = approvedEnrollment();
        enrollment.setClassSection(otherClass);
        LearningMaterial material = completedMaterial();
        when(classEnrollmentRequestRepository.findByStudentIdAndStatus(
                student.getId(), ClassEnrollmentRequestStatus.APPROVED))
                .thenReturn(List.of(enrollment));
        when(learningMaterialRepository.findById(material.getId()))
                .thenReturn(Optional.of(material));

        ApiException exception = assertThrows(ApiException.class, () ->
                service.getStudentDownload("student@example.com", material.getId()));

        assertEquals(403, exception.getStatus().value());
        verify(multipartStorage, never()).presignDownload(anyString());
    }

    private InitUploadRequest validRequest() {
        return new InitUploadRequest(
                "Lesson slides",
                null,
                "lesson.pdf",
                1024,
                "application/pdf"
        );
    }

    private LearningMaterial initializedMaterial() {
        LearningMaterial material = baseMaterial();
        material.setStatus(LearningMaterialStatus.INITIALIZED);
        material.setFileSizeBytes(LearningMaterialService.CHUNK_SIZE_BYTES + 1024);
        material.setTotalParts(2);
        return material;
    }

    private LearningMaterial completedMaterial() {
        LearningMaterial material = baseMaterial();
        material.setStatus(LearningMaterialStatus.COMPLETED);
        material.setCompletedAt(Instant.now());
        return material;
    }

    private LearningMaterial baseMaterial() {
        LearningMaterial material = new LearningMaterial();
        setId(material);
        material.setCreatedAt(Instant.now());
        material.setUpdatedAt(Instant.now());
        material.setClassSubjectLink(approvedLink);
        material.setUploadedBy(subjectTeacher);
        material.setTitle("Lesson slides");
        material.setFileName("lesson.pdf");
        material.setContentType("application/pdf");
        material.setStorageKey("materials/class/subject/id/lesson.pdf");
        material.setFileSizeBytes(1024);
        material.setUploadId("upload-1");
        material.setChunkSizeBytes(LearningMaterialService.CHUNK_SIZE_BYTES);
        material.setTotalParts(1);
        return material;
    }

    private ClassEnrollmentRequest approvedEnrollment() {
        ClassEnrollmentRequest enrollment = new ClassEnrollmentRequest();
        setId(enrollment);
        enrollment.setStudent(student);
        enrollment.setClassSection(classSection);
        enrollment.setStatus(ClassEnrollmentRequestStatus.APPROVED);
        return enrollment;
    }

    private Teacher teacher(String email, String name) {
        User user = user(email, Role.TEACHER);
        Teacher teacher = new Teacher();
        setId(teacher);
        teacher.setUser(user);
        teacher.setName(name);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(teacherRepository.findByUserId(user.getId())).thenReturn(Optional.of(teacher));
        return teacher;
    }

    private Student student(String email, String name) {
        User user = user(email, Role.STUDENT);
        Student student = new Student();
        setId(student);
        student.setUser(user);
        student.setName(name);
        student.setLrn("123456789012");
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(studentRepository.findByUserId(user.getId())).thenReturn(Optional.of(student));
        return student;
    }

    private User user(String email, Role role) {
        User user = new User();
        setId(user);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    private void setId(BaseEntity entity) {
        entity.setId(UUID.randomUUID());
    }
}
