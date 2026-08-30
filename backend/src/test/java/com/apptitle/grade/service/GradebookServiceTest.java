package com.apptitle.grade.service;

import com.apptitle.activity.entity.Activity;
import com.apptitle.activity.entity.ActivitySubmission;
import com.apptitle.activity.entity.ActivityType;
import com.apptitle.activity.entity.SubmissionStatus;
import com.apptitle.activity.repository.ActivityRepository;
import com.apptitle.activity.repository.ActivitySubmissionRepository;
import com.apptitle.classsection.entity.ClassEnrollmentRequest;
import com.apptitle.classsection.entity.ClassEnrollmentRequestStatus;
import com.apptitle.classsection.entity.ClassSection;
import com.apptitle.classsection.repository.ClassEnrollmentRequestRepository;
import com.apptitle.common.entity.BaseEntity;
import com.apptitle.common.exception.ApiException;
import com.apptitle.grade.dto.GradeConfigurationRequest;
import com.apptitle.grade.entity.GradeConfiguration;
import com.apptitle.grade.repository.GradeConfigurationRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class GradebookServiceTest {
    @Mock private GradeConfigurationRepository configurationRepository;
    @Mock private ActivityRepository activityRepository;
    @Mock private ActivitySubmissionRepository submissionRepository;
    @Mock private ClassSubjectLinkRepository linkRepository;
    @Mock private ClassEnrollmentRequestRepository enrollmentRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private UserRepository userRepository;

    private GradebookService service;
    private Teacher teacher;
    private Student student;
    private ClassSubjectLink link;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new GradebookService(configurationRepository, activityRepository,
                submissionRepository, linkRepository, enrollmentRepository,
                teacherRepository, studentRepository, userRepository);
        User teacherUser = user("teacher@example.com", Role.TEACHER);
        teacher = new Teacher(); id(teacher); teacher.setUser(teacherUser);
        when(userRepository.findByEmailIgnoreCase("teacher@example.com"))
                .thenReturn(Optional.of(teacherUser));
        when(teacherRepository.findByUserId(teacherUser.getId())).thenReturn(Optional.of(teacher));
        User studentUser = user("student@example.com", Role.STUDENT);
        student = new Student(); id(student); student.setUser(studentUser);
        student.setName("Student"); student.setLrn("123456789012");
        ClassSection section = new ClassSection(); id(section);
        Subject subject = new Subject(); id(subject); subject.setSubjectTeacher(teacher);
        link = new ClassSubjectLink(); id(link); link.setClassSection(section);
        link.setSubject(subject); link.setStatus(ClassSubjectLinkStatus.APPROVED);
        when(linkRepository.findByClassSectionIdAndSubjectId(section.getId(), subject.getId()))
                .thenReturn(Optional.of(link));
    }

    @Test
    void configure_rejectsWeightsThatDoNotTotalOneHundred() {
        ApiException exception = assertThrows(ApiException.class, () -> service.configure(
                "teacher@example.com", link.getClassSection().getId(), link.getSubject().getId(),
                new GradeConfigurationRequest(new BigDecimal("40"),
                        new BigDecimal("40"), new BigDecimal("10"))));
        assertEquals(400, exception.getStatus().value());
    }

    @Test
    void gradebook_calculatesCategoryAndWeightedFinalGrade() {
        GradeConfiguration config = config();
        Activity written = activity(ActivityType.WRITTEN_ACTIVITY, "20");
        Activity performance = activity(ActivityType.PERFORMANCE_TASK, "50");
        Activity test = activity(ActivityType.TEST, "100");
        when(configurationRepository.findByClassSubjectLinkId(link.getId()))
                .thenReturn(Optional.of(config));
        when(activityRepository.findByClassSubjectLinkIdOrderByDeadlineAsc(link.getId()))
                .thenReturn(List.of(written, performance, test));
        ClassEnrollmentRequest enrollment = new ClassEnrollmentRequest(); id(enrollment);
        enrollment.setStudent(student); enrollment.setClassSection(link.getClassSection());
        enrollment.setStatus(ClassEnrollmentRequestStatus.APPROVED);
        when(enrollmentRepository.findByClassSectionIdAndStatus(link.getClassSection().getId(),
                ClassEnrollmentRequestStatus.APPROVED)).thenReturn(List.of(enrollment));
        graded(written, "18"); graded(performance, "40"); graded(test, "70");

        var row = service.teacherGradebook("teacher@example.com",
                link.getClassSection().getId(), link.getSubject().getId()).students().getFirst();

        assertEquals(new BigDecimal("90.00"), row.writtenActivityAverage());
        assertEquals(new BigDecimal("80.00"), row.performanceTaskAverage());
        assertEquals(new BigDecimal("70.00"), row.testAverage());
        assertEquals(new BigDecimal("82.00"), row.finalGrade());
    }

    @Test
    void gradebookTreatsCategoryWithoutGradedActivitiesAsZeroWithoutRenormalizing() {
        when(configurationRepository.findByClassSubjectLinkId(link.getId()))
                .thenReturn(Optional.of(config()));
        Activity written = activity(ActivityType.WRITTEN_ACTIVITY, "100");
        when(activityRepository.findByClassSubjectLinkIdOrderByDeadlineAsc(link.getId()))
                .thenReturn(List.of(written));
        ClassEnrollmentRequest enrollment = new ClassEnrollmentRequest(); id(enrollment);
        enrollment.setStudent(student); enrollment.setClassSection(link.getClassSection());
        enrollment.setStatus(ClassEnrollmentRequestStatus.APPROVED);
        when(enrollmentRepository.findByClassSectionIdAndStatus(link.getClassSection().getId(),
                ClassEnrollmentRequestStatus.APPROVED)).thenReturn(List.of(enrollment));
        graded(written, "100");

        var row = service.teacherGradebook("teacher@example.com",
                link.getClassSection().getId(), link.getSubject().getId()).students().getFirst();

        assertEquals(new BigDecimal("40.00"), row.finalGrade());
        assertEquals(new BigDecimal("0"), row.performanceTaskAverage());
        assertEquals(new BigDecimal("0"), row.testAverage());
    }

    @Test
    void anotherTeacherCannotReadOrExportGradebook() {
        User otherUser = user("other-teacher@example.com", Role.TEACHER);
        Teacher other = new Teacher(); id(other); other.setUser(otherUser);
        when(userRepository.findByEmailIgnoreCase("other-teacher@example.com"))
                .thenReturn(Optional.of(otherUser));
        when(teacherRepository.findByUserId(otherUser.getId())).thenReturn(Optional.of(other));

        assertEquals(403, assertThrows(ApiException.class, () -> service.teacherGradebook(
                "other-teacher@example.com", link.getClassSection().getId(),
                link.getSubject().getId())).getStatus().value());
        assertEquals(403, assertThrows(ApiException.class, () -> service.export(
                "other-teacher@example.com", link.getClassSection().getId(),
                link.getSubject().getId())).getStatus().value());
    }

    @Test
    void export_producesXlsxBytes() {
        when(configurationRepository.findByClassSubjectLinkId(link.getId()))
                .thenReturn(Optional.of(config()));
        when(activityRepository.findByClassSubjectLinkIdOrderByDeadlineAsc(link.getId()))
                .thenReturn(List.of());
        when(enrollmentRepository.findByClassSectionIdAndStatus(any(), any()))
                .thenReturn(List.of());
        byte[] bytes = service.export("teacher@example.com",
                link.getClassSection().getId(), link.getSubject().getId());
        assertEquals('P', bytes[0]);
        assertEquals('K', bytes[1]);
    }

    @Test
    void pendingAndRejectedProposalsAreExcludedWithoutGradedAuthoritativeScores() {
        GradeConfiguration config = config();
        Activity pending = activity(ActivityType.TEST, "100");
        Activity rejected = activity(ActivityType.TEST, "100");
        when(configurationRepository.findByClassSubjectLinkId(link.getId()))
                .thenReturn(Optional.of(config));
        when(activityRepository.findByClassSubjectLinkIdOrderByDeadlineAsc(link.getId()))
                .thenReturn(List.of(pending, rejected));
        ClassEnrollmentRequest enrollment = new ClassEnrollmentRequest(); id(enrollment);
        enrollment.setStudent(student); enrollment.setClassSection(link.getClassSection());
        enrollment.setStatus(ClassEnrollmentRequestStatus.APPROVED);
        when(enrollmentRepository.findByClassSectionIdAndStatus(link.getClassSection().getId(),
                ClassEnrollmentRequestStatus.APPROVED)).thenReturn(List.of(enrollment));
        ActivitySubmission notAuthoritative = new ActivitySubmission(); id(notAuthoritative);
        notAuthoritative.setActivity(pending); notAuthoritative.setStudent(student);
        notAuthoritative.setScore(new BigDecimal("100"));
        notAuthoritative.setStatus(SubmissionStatus.SUBMITTED);
        when(submissionRepository.findByActivityIdAndStudentId(pending.getId(), student.getId()))
                .thenReturn(Optional.of(notAuthoritative));

        var row = service.teacherGradebook("teacher@example.com",
                link.getClassSection().getId(), link.getSubject().getId()).students().getFirst();

        assertEquals(new BigDecimal("0"), row.testAverage());
        assertEquals(new BigDecimal("0.00"), row.finalGrade());
    }

    private void graded(Activity activity, String score) {
        ActivitySubmission submission = new ActivitySubmission(); id(submission);
        submission.setActivity(activity); submission.setStudent(student);
        submission.setScore(new BigDecimal(score)); submission.setStatus(SubmissionStatus.GRADED);
        when(submissionRepository.findByActivityIdAndStudentId(activity.getId(), student.getId()))
                .thenReturn(Optional.of(submission));
    }

    private Activity activity(ActivityType type, String perfect) {
        Activity activity = new Activity(); id(activity); activity.setClassSubjectLink(link);
        activity.setType(type); activity.setTitle(type.name());
        activity.setPerfectScore(new BigDecimal(perfect));
        return activity;
    }

    private GradeConfiguration config() {
        GradeConfiguration config = new GradeConfiguration(); id(config);
        config.setClassSubjectLink(link);
        config.setWrittenActivityWeight(new BigDecimal("40"));
        config.setPerformanceTaskWeight(new BigDecimal("40"));
        config.setTestWeight(new BigDecimal("20"));
        return config;
    }

    private User user(String email, Role role) {
        User user = new User(); id(user); user.setEmail(email); user.setRole(role); return user;
    }

    private void id(BaseEntity entity) { entity.setId(UUID.randomUUID()); }
}
