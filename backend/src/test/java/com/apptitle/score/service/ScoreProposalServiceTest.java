package com.apptitle.score.service;

import com.apptitle.activity.entity.Activity;
import com.apptitle.activity.entity.ActivityFile;
import com.apptitle.activity.entity.ActivitySubmission;
import com.apptitle.activity.entity.ActivityType;
import com.apptitle.activity.entity.SubmissionStatus;
import com.apptitle.activity.repository.ActivityFileRepository;
import com.apptitle.activity.repository.ActivityRepository;
import com.apptitle.activity.repository.ActivitySubmissionRepository;
import com.apptitle.activity.service.ActivitySubmissionService;
import com.apptitle.classsection.entity.ClassSection;
import com.apptitle.classsection.repository.ClassEnrollmentRequestRepository;
import com.apptitle.common.entity.BaseEntity;
import com.apptitle.score.dto.ReviewScoreProposalRequest;
import com.apptitle.score.entity.ScoreProposal;
import com.apptitle.score.entity.ScoreProposalStatus;
import com.apptitle.score.repository.ScoreProposalRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.apptitle.common.exception.ApiException;

class ScoreProposalServiceTest {
    @Mock private ScoreProposalRepository proposalRepository;
    @Mock private ActivityRepository activityRepository;
    @Mock private ActivityFileRepository fileRepository;
    @Mock private ActivitySubmissionRepository submissionRepository;
    @Mock private ClassEnrollmentRequestRepository enrollmentRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private UserRepository userRepository;
    @Mock private ActivitySubmissionService uploadService;

    private ScoreProposalService service;
    private Teacher teacher;
    private Student student;
    private ScoreProposal proposal;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ScoreProposalService(proposalRepository, activityRepository,
                fileRepository, submissionRepository, enrollmentRepository,
                teacherRepository, studentRepository, userRepository, uploadService);
        User teacherUser = user("teacher@example.com", Role.TEACHER);
        teacher = new Teacher(); id(teacher); teacher.setUser(teacherUser);
        when(userRepository.findByEmailIgnoreCase("teacher@example.com"))
                .thenReturn(Optional.of(teacherUser));
        when(teacherRepository.findByUserId(teacherUser.getId())).thenReturn(Optional.of(teacher));
        User studentUser = user("student@example.com", Role.STUDENT);
        student = new Student(); id(student); student.setUser(studentUser);
        student.setName("Student"); student.setLrn("123456789012");
        proposal = proposal();
        when(proposalRepository.findById(proposal.getId())).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any(ScoreProposal.class))).thenAnswer(value -> value.getArgument(0));
        when(submissionRepository.save(any(ActivitySubmission.class))).thenAnswer(value -> value.getArgument(0));
    }

    @Test
    void rejectedProposalDoesNotWriteAuthoritativeScore() {
        var response = service.reject("teacher@example.com", proposal.getId());
        assertEquals(ScoreProposalStatus.REJECTED, response.status());
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void approvedProposalWritesReportedScore() {
        service.approve("teacher@example.com", proposal.getId(),
                new ReviewScoreProposalRequest(null));
        ArgumentCaptor<ActivitySubmission> captor = ArgumentCaptor.forClass(ActivitySubmission.class);
        verify(submissionRepository).save(captor.capture());
        assertEquals(new BigDecimal("80"), captor.getValue().getScore());
        assertEquals(SubmissionStatus.GRADED, captor.getValue().getStatus());
        assertEquals(ScoreProposalStatus.APPROVED, proposal.getStatus());
    }

    @Test
    void editedApprovedProposalWritesTeacherScore() {
        service.approve("teacher@example.com", proposal.getId(),
                new ReviewScoreProposalRequest(new BigDecimal("75")));
        ArgumentCaptor<ActivitySubmission> captor = ArgumentCaptor.forClass(ActivitySubmission.class);
        verify(submissionRepository).save(captor.capture());
        assertEquals(new BigDecimal("75"), captor.getValue().getScore());
        assertEquals(ScoreProposalStatus.EDITED_APPROVED, proposal.getStatus());
    }

    @Test
    void teacherCannotApproveProposalForAnotherTeachersSubject() {
        User otherUser = user("other-teacher@example.com", Role.TEACHER);
        Teacher other = new Teacher(); id(other); other.setUser(otherUser);
        when(userRepository.findByEmailIgnoreCase("other-teacher@example.com"))
                .thenReturn(Optional.of(otherUser));
        when(teacherRepository.findByUserId(otherUser.getId())).thenReturn(Optional.of(other));

        ApiException exception = assertThrows(ApiException.class, () -> service.approve(
                "other-teacher@example.com", proposal.getId(),
                new ReviewScoreProposalRequest(null)));

        assertEquals(403, exception.getStatus().value());
        verify(submissionRepository, never()).save(any());
    }

    private ScoreProposal proposal() {
        ClassSection section = new ClassSection(); id(section);
        Subject subject = new Subject(); id(subject); subject.setSubjectTeacher(teacher);
        ClassSubjectLink link = new ClassSubjectLink(); id(link); link.setClassSection(section);
        link.setSubject(subject); link.setStatus(ClassSubjectLinkStatus.APPROVED);
        Activity activity = new Activity(); id(activity); activity.setClassSubjectLink(link);
        activity.setType(ActivityType.TEST); activity.setPerfectScore(new BigDecimal("100"));
        activity.setAllowStudentSelfSubmissionScore(true);
        ActivityFile proof = new ActivityFile(); id(proof); proof.setFileName("proof.jpg");
        proof.setContentType("image/jpeg"); proof.setFileSizeBytes(100); proof.setCompletedAt(Instant.now());
        ScoreProposal value = new ScoreProposal(); id(value); value.setActivity(activity);
        value.setStudent(student); value.setProofFile(proof);
        value.setReportedScore(new BigDecimal("80")); value.setStatus(ScoreProposalStatus.PENDING);
        value.setSubmittedAt(Instant.now());
        return value;
    }

    private User user(String email, Role role) {
        User value = new User(); id(value); value.setEmail(email); value.setRole(role); return value;
    }

    private void id(BaseEntity entity) { entity.setId(UUID.randomUUID()); }
}
