package com.apptitle.user.service;

import com.apptitle.auth.dto.AuthResponse;
import com.apptitle.common.exception.ApiException;
import com.apptitle.config.JwtService;
import com.apptitle.student.repository.StudentRepository;
import com.apptitle.teacher.repository.TeacherRepository;
import com.apptitle.user.dto.UpdateProfileRequest;
import com.apptitle.user.entity.Role;
import com.apptitle.user.entity.User;
import com.apptitle.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;

@Service
public class ProfileService {
    private final UserRepository users;
    private final StudentRepository students;
    private final TeacherRepository teachers;
    private final JwtService jwtService;

    public ProfileService(UserRepository users, StudentRepository students,
                          TeacherRepository teachers, JwtService jwtService) {
        this.users = users;
        this.students = students;
        this.teachers = teachers;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public AuthResponse get(String email) {
        return response(currentUser(email));
    }

    @Transactional
    public AuthResponse update(String currentEmail, UpdateProfileRequest request) {
        User user = currentUser(currentEmail);
        String email = request.email().trim();
        users.findByEmailIgnoreCase(email).filter(other -> !other.getId().equals(user.getId()))
                .ifPresent(other -> { throw ApiException.conflict("An account with this email already exists."); });
        validatePicture(request.profilePicture());
        if (user.getRole() == Role.STUDENT) {
            var student = students.findByUserId(user.getId())
                    .orElseThrow(() -> ApiException.notFound("Student profile not found."));
            String lrn = request.lrn() == null ? "" : request.lrn().trim();
            if (!lrn.matches("[0-9]{12}")) {
                throw ApiException.badRequest("LRN must contain exactly 12 digits.");
            }
            if (!lrn.equals(student.getLrn()) && students.existsByLrn(lrn)) {
                throw ApiException.conflict("An account already exists for this LRN.");
            }
            student.setName(request.name().trim());
            student.setLrn(lrn);
        } else {
            var teacher = teachers.findByUserId(user.getId())
                    .orElseThrow(() -> ApiException.notFound("Teacher profile not found."));
            teacher.setName(request.name().trim());
        }
        user.setEmail(email);
        user.setProfilePicture(request.profilePicture());
        users.flush();
        return response(user);
    }

    private User currentUser(String email) {
        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.unauthorized("Account not found."));
        if (!user.isEnabled()) throw ApiException.forbidden("This account has been disabled.");
        return user;
    }

    private AuthResponse response(User user) {
        String name;
        String lrn = null;
        if (user.getRole() == Role.STUDENT) {
            var student = students.findByUserId(user.getId())
                    .orElseThrow(() -> ApiException.notFound("Student profile not found."));
            name = student.getName();
            lrn = student.getLrn();
        } else {
            name = teachers.findByUserId(user.getId())
                    .orElseThrow(() -> ApiException.notFound("Teacher profile not found.")).getName();
        }
        return new AuthResponse(jwtService.generateToken(user.getId(), user.getEmail(), user.getRole()),
                user.getId(), name, user.getEmail(), user.getRole(), lrn, user.getProfilePicture());
    }

    private void validatePicture(String picture) {
        if (picture == null) return;
        if (picture.length() > 350000 || !picture.startsWith("data:image/png;base64,")) {
            throw ApiException.badRequest("Choose a PNG profile picture smaller than 256 KB.");
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(picture.substring("data:image/png;base64,".length()));
            if (bytes.length > 262144) throw ApiException.badRequest("Profile picture must be smaller than 256 KB.");
            try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw ApiException.badRequest("Invalid profile picture.");
                var reader = readers.next();
                try {
                    reader.setInput(input);
                    if (!reader.getFormatName().equalsIgnoreCase("png") || reader.getWidth(0) > 512 || reader.getHeight(0) > 512) {
                        throw ApiException.badRequest("Profile picture must be a PNG no larger than 512 × 512 pixels.");
                    }
                    reader.read(0);
                } finally {
                    reader.dispose();
                }
            }
        } catch (IllegalArgumentException | IOException exception) {
            throw ApiException.badRequest("Invalid profile picture.");
        }
    }
}
