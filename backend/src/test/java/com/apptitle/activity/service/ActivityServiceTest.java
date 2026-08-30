package com.apptitle.activity.service;

import com.apptitle.activity.dto.SaveActivityRequest;
import com.apptitle.activity.entity.Activity;
import com.apptitle.activity.entity.ActivityType;
import com.apptitle.activity.repository.ActivityRepository;
import com.apptitle.classsection.entity.ClassEnrollmentRequest;
import com.apptitle.classsection.entity.ClassEnrollmentRequestStatus;
import com.apptitle.classsection.entity.ClassSection;
import com.apptitle.classsection.repository.ClassEnrollmentRequestRepository;
import com.apptitle.common.entity.BaseEntity;
import com.apptitle.common.exception.ApiException;
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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActivityServiceTest {

    @Mock private ActivityRepository activityRepository;
    @Mock private ClassSubjectLinkRepository linkRepository;
    @Mock private ClassEnrollmentRequestRepository enrollmentRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private UserRepository userRepository;

    private ActivityService service;
    private Teacher owner;
    private Teacher other;
    private Student student;
    private ClassSubjectLink link;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ActivityService(activityRepository, linkRepository,
                enrollmentRepository, teacherRepository, studentRepository, userRepository);
        owner = teacher("owner@example.com");
        other = teacher("other@example.com");
        student = student("student@example.com");

        ClassSection section = new ClassSection();
        id(section);
        section.setAdviser(other);
        Subject subject = new Subject();
        id(subject);
        subject.setSubjectTeacher(owner);
        subject.setName("Physics");
        link = new ClassSubjectLink();
        id(link);
        link.setClassSection(section);
        link.setSubject(subject);
        link.setRequestedBy(other);
        link.setStatus(ClassSubjectLinkStatus.APPROVED);
        when(linkRepository.findByClassSectionIdAndSubjectId(section.getId(), subject.getId()))
                .thenReturn(Optional.of(link));
        when(activityRepository.save(any(Activity.class))).thenAnswer(invocation -> {
            Activity activity = invocation.getArgument(0);
            if (activity.getId() == null) id(activity);
            activity.setCreatedAt(Instant.now());
            activity.setUpdatedAt(Instant.now());
            return activity;
        });
    }

    @Test
    void create_persistsLockedActivityCategory() {
        var response = service.create("owner@example.com",
                link.getClassSection().getId(), link.getSubject().getId(), request());

        assertEquals(ActivityType.WRITTEN_ACTIVITY, response.type());
        assertEquals("Written Activity", response.typeLabel());
        assertEquals(new BigDecimal("20.00"), response.perfectScore());
    }

    @Test
    void create_rejectsNonOwningTeacher() {
        ApiException exception = assertThrows(ApiException.class, () ->
                service.create("other@example.com", link.getClassSection().getId(),
                        link.getSubject().getId(), request()));

        assertEquals(403, exception.getStatus().value());
        verify(activityRepository, never()).save(any());
    }

    @Test
    void update_rejectsNonOwningTeacher() {
        Activity activity = activity();
        when(activityRepository.findById(activity.getId())).thenReturn(Optional.of(activity));

        ApiException exception = assertThrows(ApiException.class, () ->
                service.update("other@example.com", activity.getId(), request()));

        assertEquals(403, exception.getStatus().value());
    }

    @Test
    void listForStudent_returnsActivitiesForApprovedEnrollment() {
        ClassEnrollmentRequest enrollment = new ClassEnrollmentRequest();
        id(enrollment);
        enrollment.setStudent(student);
        enrollment.setClassSection(link.getClassSection());
        enrollment.setStatus(ClassEnrollmentRequestStatus.APPROVED);
        when(enrollmentRepository.findByStudentIdAndStatus(
                student.getId(), ClassEnrollmentRequestStatus.APPROVED))
                .thenReturn(List.of(enrollment));
        when(activityRepository.findByClassSubjectLinkIdOrderByDeadlineAsc(link.getId()))
                .thenReturn(List.of(activity()));

        assertEquals(1, service.listForStudent(
                "student@example.com", link.getSubject().getId()).size());
    }

    @Test
    void listForStudent_rejectsStudentWithoutApprovedEnrollment() {
        when(enrollmentRepository.findByStudentIdAndStatus(
                student.getId(), ClassEnrollmentRequestStatus.APPROVED))
                .thenReturn(List.of());

        ApiException exception = assertThrows(ApiException.class, () ->
                service.listForStudent("student@example.com", link.getSubject().getId()));

        assertEquals(403, exception.getStatus().value());
    }

    private SaveActivityRequest request() {
        return new SaveActivityRequest(ActivityType.WRITTEN_ACTIVITY, "Activity 5",
                new BigDecimal("20.00"), Instant.now().plus(7, ChronoUnit.DAYS),
                "Complete every item.", true);
    }

    private Activity activity() {
        Activity activity = new Activity();
        id(activity);
        activity.setCreatedAt(Instant.now());
        activity.setUpdatedAt(Instant.now());
        activity.setClassSubjectLink(link);
        activity.setCreatedBy(owner);
        activity.setType(ActivityType.TEST);
        activity.setTitle("Quarterly test");
        activity.setPerfectScore(new BigDecimal("50.00"));
        activity.setDeadline(Instant.now().plus(7, ChronoUnit.DAYS));
        activity.setInstructions("Answer all questions.");
        return activity;
    }

    private Teacher teacher(String email) {
        User user = user(email, Role.TEACHER);
        Teacher teacher = new Teacher();
        id(teacher);
        teacher.setUser(user);
        teacher.setName(email);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(teacherRepository.findByUserId(user.getId())).thenReturn(Optional.of(teacher));
        return teacher;
    }

    private Student student(String email) {
        User user = user(email, Role.STUDENT);
        Student value = new Student();
        id(value);
        value.setUser(user);
        value.setName(email);
        value.setLrn("123456789012");
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        when(studentRepository.findByUserId(user.getId())).thenReturn(Optional.of(value));
        return value;
    }

    private User user(String email, Role role) {
        User user = new User();
        id(user);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    private void id(BaseEntity entity) {
        entity.setId(UUID.randomUUID());
    }
}
