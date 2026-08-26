package com.apptitle.auth.service;

import com.apptitle.auth.dto.AuthResponse;
import com.apptitle.auth.dto.LoginRequest;
import com.apptitle.auth.dto.RegisterStudentRequest;
import com.apptitle.auth.dto.RegisterTeacherRequest;
import com.apptitle.common.exception.ApiException;
import com.apptitle.config.JwtService;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Registration no longer verifies students against any roster. Student
 * registration DOES require a valid classroom code (typed, not browsed/
 * picked) identifying exactly one Section — but the code only has to
 * correspond to a real classroom; it verifies nothing about the student.
 * Providing a valid code creates a PENDING JoinRequest, not membership —
 * actual access is granted only once a teacher approves it (Phase 3).
 *
 * Login is unchanged from the original implementation — no section/code
 * concept applies to it at all.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final SectionRepository sectionRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            TeacherRepository teacherRepository,
            StudentRepository studentRepository,
            SectionRepository sectionRepository,
            JoinRequestRepository joinRequestRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.sectionRepository = sectionRepository;
        this.joinRequestRepository = joinRequestRepository;
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
        return AuthResponse.withoutSection(token, user.getId(), teacher.getName(), user.getEmail(), user.getRole());
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
        return AuthResponse.withoutSection(token, user.getId(), student.getName(), user.getEmail(), user.getRole());
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
        return AuthResponse.withoutSection(token, user.getId(), name, user.getEmail(), user.getRole());
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

