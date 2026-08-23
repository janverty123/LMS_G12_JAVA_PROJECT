package com.apptitle.joinrequest.service;

import com.apptitle.common.entity.BaseEntity;
import com.apptitle.common.exception.ApiException;
import com.apptitle.joinrequest.dto.CreateJoinRequestRequest;
import com.apptitle.joinrequest.dto.JoinRequestResponse;
import com.apptitle.joinrequest.entity.JoinRequest;
import com.apptitle.joinrequest.entity.JoinRequestStatus;
import com.apptitle.joinrequest.repository.JoinRequestRepository;
import com.apptitle.section.entity.Section;
import com.apptitle.section.repository.SectionRepository;
import com.apptitle.student.entity.Student;
import com.apptitle.student.repository.StudentRepository;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class JoinRequestServiceTest {

    @Mock private JoinRequestRepository joinRequestRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private UserRepository userRepository;

    private JoinRequestService service;

    private User teacherUser;
    private Teacher teacher;
    private User otherTeacherUser;
    private Teacher otherTeacher;
    private User studentUser;
    private Student student;
    private Section section;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new JoinRequestService(
                joinRequestRepository, sectionRepository, studentRepository, teacherRepository, userRepository);

        teacherUser = new User();
        setId(teacherUser);
        teacherUser.setEmail("santos@example.com");
        teacherUser.setRole(Role.TEACHER);

        teacher = new Teacher();
        setId(teacher);
        teacher.setUser(teacherUser);
        teacher.setName("Ms. Santos");

        otherTeacherUser = new User();
        setId(otherTeacherUser);
        otherTeacherUser.setEmail("cruz@example.com");
        otherTeacherUser.setRole(Role.TEACHER);

        otherTeacher = new Teacher();
        setId(otherTeacher);
        otherTeacher.setUser(otherTeacherUser);
        otherTeacher.setName("Mr. Cruz");

        studentUser = new User();
        setId(studentUser);
        studentUser.setEmail("juan@example.com");
        studentUser.setRole(Role.STUDENT);

        student = new Student();
        setId(student);
        student.setUser(studentUser);
        student.setName("Juan Dela Cruz");
        student.setLrn("123456789012");

        section = new Section();
        setId(section);
        section.setTeacher(teacher);
        section.setName("Grade 12 - Rossum");
        section.setSubjectName("ICT Programming");
        section.setClassCode("ABC1234");

        when(userRepository.findByEmailIgnoreCase("santos@example.com")).thenReturn(Optional.of(teacherUser));
        when(userRepository.findByEmailIgnoreCase("cruz@example.com")).thenReturn(Optional.of(otherTeacherUser));
        when(userRepository.findByEmailIgnoreCase("juan@example.com")).thenReturn(Optional.of(studentUser));
        when(teacherRepository.findByUserId(teacherUser.getId())).thenReturn(Optional.of(teacher));
        when(teacherRepository.findByUserId(otherTeacherUser.getId())).thenReturn(Optional.of(otherTeacher));
        when(studentRepository.findByUserId(studentUser.getId())).thenReturn(Optional.of(student));
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(inv -> {
            JoinRequest jr = inv.getArgument(0);
            if (jr.getId() == null) setId(jr);
            return jr;
        });
    }

    private void setId(BaseEntity entity) {
        entity.setId(UUID.randomUUID());
    }

    private JoinRequest pendingRequest() {
        JoinRequest jr = new JoinRequest();
        setId(jr);
        jr.setStudent(student);
        jr.setSection(section);
        jr.setStatus(JoinRequestStatus.PENDING);
        return jr;
    }

    @Test
    void approve_succeedsForOwningTeacher() {
        JoinRequest jr = pendingRequest();
        when(joinRequestRepository.findById(jr.getId())).thenReturn(Optional.of(jr));

        JoinRequestResponse response = service.approve("santos@example.com", jr.getId());

        assertEquals(JoinRequestStatus.APPROVED, response.status());
    }

    @Test
    void approve_rejectsNonOwningTeacher() {
        JoinRequest jr = pendingRequest();
        when(joinRequestRepository.findById(jr.getId())).thenReturn(Optional.of(jr));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.approve("cruz@example.com", jr.getId()));
        assertEquals(403, ex.getStatus().value());
    }

    @Test
    void decline_succeedsForOwningTeacher() {
        JoinRequest jr = pendingRequest();
        when(joinRequestRepository.findById(jr.getId())).thenReturn(Optional.of(jr));

        JoinRequestResponse response = service.decline("santos@example.com", jr.getId());

        assertEquals(JoinRequestStatus.DECLINED, response.status());
    }

    @Test
    void removeMember_deletesApprovedJoinRequest() {
        JoinRequest jr = pendingRequest();
        jr.setStatus(JoinRequestStatus.APPROVED);
        when(sectionRepository.findById(section.getId())).thenReturn(Optional.of(section));
        when(joinRequestRepository.findByStudentIdAndSectionId(student.getId(), section.getId()))
                .thenReturn(Optional.of(jr));

        service.removeMember("santos@example.com", section.getId(), student.getId());

        org.mockito.Mockito.verify(joinRequestRepository).delete(jr);
    }

    @Test
    void removeMember_rejectsNonOwningTeacher() {
        when(sectionRepository.findById(section.getId())).thenReturn(Optional.of(section));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.removeMember("cruz@example.com", section.getId(), student.getId()));
        assertEquals(403, ex.getStatus().value());
    }

    @Test
    void createJoinRequest_rejectsInvalidCode() {
        when(sectionRepository.findByClassCode("BADCODE")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () ->
                service.createJoinRequest("juan@example.com", new CreateJoinRequestRequest("badcode")));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void createJoinRequest_rejectsDuplicateRequest() {
        when(sectionRepository.findByClassCode("ABC1234")).thenReturn(Optional.of(section));
        when(joinRequestRepository.existsByStudentIdAndSectionId(student.getId(), section.getId()))
                .thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () ->
                service.createJoinRequest("juan@example.com", new CreateJoinRequestRequest("ABC1234")));
        assertEquals(409, ex.getStatus().value());
    }

    @Test
    void createJoinRequest_succeedsWithValidUnusedCode() {
        when(sectionRepository.findByClassCode("ABC1234")).thenReturn(Optional.of(section));
        when(joinRequestRepository.existsByStudentIdAndSectionId(student.getId(), section.getId()))
                .thenReturn(false);

        JoinRequestResponse response = service.createJoinRequest(
                "juan@example.com", new CreateJoinRequestRequest("abc1234"));

        assertEquals(JoinRequestStatus.PENDING, response.status());
        assertEquals("Grade 12 - Rossum", response.sectionName());
    }
}
