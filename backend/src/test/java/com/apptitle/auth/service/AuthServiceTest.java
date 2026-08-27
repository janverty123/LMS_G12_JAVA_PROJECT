package com.apptitle.auth.service;

import com.apptitle.auth.dto.AuthResponse;
import com.apptitle.auth.dto.LoginRequest;
import com.apptitle.auth.dto.RegisterStudentRequest;
import com.apptitle.auth.dto.RegisterTeacherRequest;
import com.apptitle.common.entity.BaseEntity;
import com.apptitle.common.exception.ApiException;
import com.apptitle.config.JwtService;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(
                userRepository, teacherRepository, studentRepository, passwordEncoder, jwtService);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) setId(user);
            return user;
        });
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> {
            Student student = invocation.getArgument(0);
            if (student.getId() == null) setId(student);
            return student;
        });
        when(jwtService.generateToken(any(), anyString(), any())).thenReturn("fake.jwt.token");
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
    }

    @Test
    void registerStudent_succeedsWithoutClassCode() {
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(studentRepository.existsByLrn(anyString())).thenReturn(false);

        AuthResponse response = authService.registerStudent(studentRequest());

        assertEquals("Juan Dela Cruz", response.name());
        assertEquals(Role.STUDENT, response.role());
        verify(passwordEncoder).encode("password123");
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    void registerStudent_rejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("juan@example.com")).thenReturn(true);

        ApiException exception = assertThrows(
                ApiException.class, () -> authService.registerStudent(studentRequest()));

        assertEquals(409, exception.getStatus().value());
    }

    @Test
    void registerStudent_rejectsDuplicateLrn() {
        when(studentRepository.existsByLrn("123456789012")).thenReturn(true);

        ApiException exception = assertThrows(
                ApiException.class, () -> authService.registerStudent(studentRequest()));

        assertEquals(409, exception.getStatus().value());
    }

    @Test
    void registerTeacher_createsAccountSuccessfully() {
        RegisterTeacherRequest request = new RegisterTeacherRequest(
                "Ms. Santos", "santos@example.com", "password123");
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(invocation -> {
            Teacher teacher = invocation.getArgument(0);
            setId(teacher);
            return teacher;
        });

        AuthResponse response = authService.registerTeacher(request);

        assertEquals("Ms. Santos", response.name());
        assertEquals(Role.TEACHER, response.role());
    }

    @Test
    void login_returnsAuthenticatedStudent() {
        User user = user("juan@example.com", Role.STUDENT);
        user.setPasswordHash("hashed");
        user.setEnabled(true);
        Student student = new Student();
        student.setUser(user);
        student.setName("Juan Dela Cruz");
        student.setLrn("123456789012");
        when(userRepository.findByEmailIgnoreCase("juan@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(studentRepository.findByUserId(user.getId())).thenReturn(Optional.of(student));

        AuthResponse response = authService.login(
                new LoginRequest("juan@example.com", "password123"));

        assertEquals("Juan Dela Cruz", response.name());
        assertEquals("fake.jwt.token", response.token());
    }

    @Test
    void login_rejectsWrongPassword() {
        User user = user("juan@example.com", Role.STUDENT);
        user.setPasswordHash("hashed");
        user.setEnabled(true);
        when(userRepository.findByEmailIgnoreCase("juan@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "hashed")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(
                new LoginRequest("juan@example.com", "wrongpassword")));
    }

    private RegisterStudentRequest studentRequest() {
        return new RegisterStudentRequest(
                "Juan Dela Cruz", "123456789012", "juan@example.com", "password123");
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
