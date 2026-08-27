package com.apptitle.classsection.service;

import com.apptitle.classsection.dto.ClassEnrollmentRequestResponse;
import com.apptitle.classsection.entity.ClassEnrollmentRequest;
import com.apptitle.classsection.entity.ClassEnrollmentRequestStatus;
import com.apptitle.classsection.entity.ClassSection;
import com.apptitle.classsection.repository.ClassEnrollmentRequestRepository;
import com.apptitle.classsection.repository.ClassSectionRepository;
import com.apptitle.common.entity.BaseEntity;
import com.apptitle.common.exception.ApiException;
import com.apptitle.student.entity.Student;
import com.apptitle.student.repository.StudentRepository;
import com.apptitle.subject.repository.ClassSubjectLinkRepository;
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

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ClassEnrollmentRequestServiceTest {

    @Mock private ClassEnrollmentRequestRepository enrollmentRepository;
    @Mock private ClassSectionRepository classSectionRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private ClassSubjectLinkRepository classSubjectLinkRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private UserRepository userRepository;

    private ClassEnrollmentRequestService service;
    private Teacher adviser;
    private Teacher otherTeacher;
    private Student student;
    private ClassSection classSection;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ClassEnrollmentRequestService(
                enrollmentRepository,
                classSectionRepository,
                studentRepository,
                classSubjectLinkRepository,
                teacherRepository,
                userRepository);

        adviser = teacher("adviser@example.com", "Ms. Santos");
        otherTeacher = teacher("other@example.com", "Mr. Cruz");
        student = student("student@example.com");

        classSection = new ClassSection();
        setId(classSection);
        classSection.setAdviser(adviser);
        classSection.setGradeLevel("Grade 12");
        classSection.setSection("Rossum");
        classSection.setSchoolYear("2026-2027");
        classSection.setClassCode("ABC123");

        when(enrollmentRepository.save(any(ClassEnrollmentRequest.class)))
                .thenAnswer(invocation -> {
                    ClassEnrollmentRequest request = invocation.getArgument(0);
                    if (request.getId() == null) setId(request);
                    return request;
                });
    }

    @Test
    void approve_succeedsForOwningAdviser() {
        ClassEnrollmentRequest request = pendingRequest();
        when(enrollmentRepository.findById(request.getId())).thenReturn(Optional.of(request));

        ClassEnrollmentRequestResponse response = service.approve(
                "adviser@example.com", request.getId());

        assertEquals(ClassEnrollmentRequestStatus.APPROVED, response.status());
    }

    @Test
    void approve_rejectsNonOwningTeacher() {
        ClassEnrollmentRequest request = pendingRequest();
        when(enrollmentRepository.findById(request.getId())).thenReturn(Optional.of(request));

        ApiException exception = assertThrows(ApiException.class, () ->
                service.approve("other@example.com", request.getId()));

        assertEquals(403, exception.getStatus().value());
    }

    @Test
    void decline_rejectsNonOwningTeacher() {
        ClassEnrollmentRequest request = pendingRequest();
        when(enrollmentRepository.findById(request.getId())).thenReturn(Optional.of(request));

        ApiException exception = assertThrows(ApiException.class, () ->
                service.decline("other@example.com", request.getId()));

        assertEquals(403, exception.getStatus().value());
    }

    @Test
    void approve_rejectsSecondApprovedClass() {
        ClassEnrollmentRequest request = pendingRequest();
        when(enrollmentRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(enrollmentRepository.existsByStudentIdAndStatus(
                student.getId(), ClassEnrollmentRequestStatus.APPROVED)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () ->
                service.approve("adviser@example.com", request.getId()));

        assertEquals(409, exception.getStatus().value());
    }

    @Test
    void createJoinRequest_rejectsDuplicateRequest() {
        when(classSectionRepository.findByClassCode("ABC123")).thenReturn(Optional.of(classSection));
        when(enrollmentRepository.existsByStudentIdAndClassSectionId(
                student.getId(), classSection.getId())).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () ->
                service.createJoinRequest("student@example.com", "abc123"));

        assertEquals(409, exception.getStatus().value());
    }

    @Test
    void createJoinRequest_succeedsWithValidCode() {
        when(classSectionRepository.findByClassCode("ABC123")).thenReturn(Optional.of(classSection));

        ClassEnrollmentRequestResponse response = service.createJoinRequest(
                "student@example.com", "abc123");

        assertEquals(ClassEnrollmentRequestStatus.PENDING, response.status());
        assertEquals("Grade 12 - Rossum", response.classSectionName());
    }

    @Test
    void listOwnApprovedSubjects_inheritsSubjectsFromApprovedClass() {
        ClassEnrollmentRequest enrollment = pendingRequest();
        enrollment.setStatus(ClassEnrollmentRequestStatus.APPROVED);
        Subject subject = new Subject();
        setId(subject);
        subject.setName("Programming");
        subject.setSubjectCode("PROG123");
        subject.setSubjectTeacher(adviser);
        ClassSubjectLink link = new ClassSubjectLink();
        setId(link);
        link.setClassSection(classSection);
        link.setSubject(subject);
        link.setRequestedBy(adviser);
        link.setStatus(ClassSubjectLinkStatus.APPROVED);
        when(enrollmentRepository.findByStudentIdAndStatus(
                student.getId(), ClassEnrollmentRequestStatus.APPROVED))
                .thenReturn(List.of(enrollment));
        when(classSubjectLinkRepository.findByClassSectionIdAndStatus(
                classSection.getId(), ClassSubjectLinkStatus.APPROVED))
                .thenReturn(List.of(link));

        var subjects = service.listOwnApprovedSubjects("student@example.com");

        assertEquals(1, subjects.size());
        assertEquals("Programming", subjects.get(0).name());
    }

    private ClassEnrollmentRequest pendingRequest() {
        ClassEnrollmentRequest request = new ClassEnrollmentRequest();
        setId(request);
        request.setStudent(student);
        request.setClassSection(classSection);
        request.setStatus(ClassEnrollmentRequestStatus.PENDING);
        return request;
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

    private Student student(String email) {
        User user = user(email, Role.STUDENT);
        Student student = new Student();
        setId(student);
        student.setUser(user);
        student.setName("Juan Dela Cruz");
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
