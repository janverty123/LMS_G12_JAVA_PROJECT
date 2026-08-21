package com.apptitle.auth.service;

import com.apptitle.auth.dto.AuthResponse;
import com.apptitle.auth.dto.LoginRequest;
import com.apptitle.auth.dto.RegisterStudentRequest;
import com.apptitle.auth.dto.RegisterTeacherRequest;
import com.apptitle.common.entity.BaseEntity;
import com.apptitle.common.exception.ApiException;
import com.apptitle.config.JwtService;
import com.apptitle.joinrequest.entity.JoinRequest;
import com.apptitle.joinrequest.repository.JoinRequestRepository;
import com.apptitle.section.entity.Section;
import com.apptitle.section.repository.SectionRepository;
import com.apptitle.student.entity.Student;
import com.apptitle.student.repository.StudentRepository;
import com.apptitle.teacher.repository.TeacherRepository;
import com.apptitle.user.entity.Role;
import com.apptitle.user.entity.User;
import com.apptitle.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private JoinRequestRepository joinRequestRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(
                userRepository, teacherRepository, studentRepository,
                sectionRepository, joinRequestRepository, passwordEncoder, jwtService);

        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) setId(u);
            return u;
        });
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            if (s.getId() == null) setId(s);
            return s;
        });
        when(jwtService.generateToken(any(), anyString(), any())).thenReturn("fake.jwt.token");
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
    }

    private void setId(BaseEntity entity) {
        entity.setId(UUID.randomUUID());
    }

    private RegisterStudentRequest studentRequest(String classroomCode) {
        return new RegisterStudentRequest(
                "Juan Dela Cruz", "123456789012", "juan@example.com", "password123", classroomCode);
    }

    @Test
    void registerStudent_rejectsInvalidClassroomCode() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(studentRepository.existsByLrn(anyString())).thenReturn(false);
        when(sectionRepository.findByClassCode("BADCODE")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> authService.registerStudent(studentRequest("badcode")));
        assertEquals(400, ex.getStatus().value());

        verify(userRepository, times(0)).save(any());
        verify(joinRequestRepository, times(0)).save(any());
    }

    @Test
    void registerStudent_succeedsAndCreatesPendingJoinRequest() {
        Section section = new Section();
        setId(section);
        section.setName("Grade 12 - Rossum");
        section.setClassCode("ABC1234");

        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(studentRepository.existsByLrn(anyString())).thenReturn(false);
        when(sectionRepository.findByClassCode("ABC1234")).thenReturn(Optional.of(section));

        AuthResponse response = authService.registerStudent(studentRequest("abc1234")); // lowercase input

        assertEquals("Juan Dela Cruz", response.name());
        assertEquals(Role.STUDENT, response.role());
        assertEquals("Grade 12 - Rossum", response.sectionName());
        assertEquals("PENDING", response.joinRequestStatus());
        verify(joinRequestRepository, times(1)).save(any(JoinRequest.class));
    }

    @Test
    void registerStudent_rejectsDuplicateEmail_beforeCheckingClassroomCode() {
        when(userRepository.existsByEmailIgnoreCase("juan@example.com")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class,
                () -> authService.registerStudent(studentRequest("ABC1234")));
        assertEquals(409, ex.getStatus().value());
        verify(sectionRepository, times(0)).findByClassCode(anyString());
    }

    @Test
    void registerStudent_rejectsDuplicateLrn() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(studentRepository.existsByLrn("123456789012")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class,
                () -> authService.registerStudent(studentRequest("ABC1234")));
        assertEquals(409, ex.getStatus().value());
    }

    @Test
    void registerTeacher_createsAccountSuccessfully_noSectionInvolved() {
        RegisterTeacherRequest request = new RegisterTeacherRequest(
                "Ms. Santos", "santos@example.com", "password123");

        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(teacherRepository.save(any())).thenAnswer(inv -> {
            var t = inv.getArgument(0, com.apptitle.teacher.entity.Teacher.class);
            setId(t);
            return t;
        });

        AuthResponse response = authService.registerTeacher(request);

        assertEquals("Ms. Santos", response.name());
        assertEquals(Role.TEACHER, response.role());
        assertEquals(null, response.sectionName());
        assertEquals(null, response.joinRequestStatus());
    }

    @Test
    void login_isUnaffectedByClassroomCodeConcept() {
        User user = new User();
        setId(user);
        user.setEmail("juan@example.com");
        user.setPasswordHash("hashed");
        user.setRole(Role.STUDENT);
        user.setEnabled(true);

        Student student = new Student();
        student.setUser(user);
        student.setName("Juan Dela Cruz");
        student.setLrn("123456789012");

        when(userRepository.findByEmailIgnoreCase("juan@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(studentRepository.findByUserId(user.getId())).thenReturn(Optional.of(student));

        AuthResponse response = authService.login(new LoginRequest("juan@example.com", "password123"));

        assertEquals("Juan Dela Cruz", response.name());
        assertEquals("fake.jwt.token", response.token());
        assertEquals(null, response.sectionName());
    }

    @Test
    void login_throwsBadCredentialsForUnknownEmail() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () ->
                authService.login(new LoginRequest("nobody@example.com", "whatever123")));
    }

    @Test
    void login_throwsBadCredentialsForWrongPassword() {
        User user = new User();
        setId(user);
        user.setEmail("juan@example.com");
        user.setPasswordHash("hashed");
        user.setRole(Role.STUDENT);
        user.setEnabled(true);

        when(userRepository.findByEmailIgnoreCase("juan@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "hashed")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () ->
                authService.login(new LoginRequest("juan@example.com", "wrongpassword")));
    }
}
