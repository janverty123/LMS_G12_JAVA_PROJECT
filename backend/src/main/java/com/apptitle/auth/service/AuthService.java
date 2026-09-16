package com.apptitle.auth.service;

import com.apptitle.auth.dto.AuthResponse;
import com.apptitle.auth.dto.LoginRequest;
import com.apptitle.auth.dto.RegisterStudentRequest;
import com.apptitle.auth.dto.RegisterTeacherRequest;
import com.apptitle.common.exception.ApiException;
import com.apptitle.config.JwtService;
import com.apptitle.student.entity.Student;
import com.apptitle.student.repository.StudentRepository;
import com.apptitle.teacher.entity.Teacher;
import com.apptitle.teacher.repository.TeacherRepository;
import com.apptitle.user.entity.Role;
import com.apptitle.user.entity.User;
import com.apptitle.user.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            TeacherRepository teacherRepository,
            StudentRepository studentRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse registerTeacher(RegisterTeacherRequest request) {
        String email = request.email().trim();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists.");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.TEACHER);
        user.setEnabled(true);
        user = userRepository.save(user);

        Teacher teacher = new Teacher();
        teacher.setUser(user);
        teacher.setName(request.name().trim());
        teacher = teacherRepository.save(teacher);

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(token, user.getId(), teacher.getName(), user.getEmail(), user.getRole());
    }

    /**
     * Student registration no longer requires a classroom code.
     * The student will join a class section in a separate step.
     */
    @Transactional
    public AuthResponse registerStudent(RegisterStudentRequest request) {
        String email = request.email().trim();
        String name = request.name().trim();
        String lrn = request.lrn().trim();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists.");
        }

        // One account per LRN — a real student shouldn't be able to create
        // multiple separate accounts.
        if (studentRepository.existsByLrn(lrn)) {
            throw ApiException.conflict("An account already exists for this LRN.");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.STUDENT);
        user.setEnabled(true);
        user = userRepository.save(user);

        Student student = new Student();
        student.setUser(user);
        student.setName(name);
        student.setLrn(lrn);
        student = studentRepository.save(student);

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(token, user.getId(), student.getName(), user.getEmail(), user.getRole(),
                student.getLrn(), user.getProfilePicture());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        if (!user.isEnabled()) {
            throw ApiException.forbidden("This account has been disabled.");
        }

        String name = resolveDisplayName(user);
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(token, user.getId(), name, user.getEmail(), user.getRole(),
                user.getRole() == Role.STUDENT ? studentRepository.findByUserId(user.getId()).orElseThrow().getLrn() : null,
                user.getProfilePicture());
    }

    private String resolveDisplayName(User user) {
        UUID userId = user.getId();
        return switch (user.getRole()) {
            case TEACHER -> teacherRepository.findByUserId(userId)
                    .map(Teacher::getName)
                    .orElseThrow(() -> ApiException.notFound("Teacher profile not found."));
            case STUDENT -> studentRepository.findByUserId(userId)
                    .map(Student::getName)
                    .orElseThrow(() -> ApiException.notFound("Student profile not found."));
        };
    }
}

