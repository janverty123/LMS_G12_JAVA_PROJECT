package com.apptitle.user.service;

import com.apptitle.common.exception.ApiException;
import com.apptitle.config.JwtService;
import com.apptitle.student.entity.Student;
import com.apptitle.student.repository.StudentRepository;
import com.apptitle.teacher.entity.Teacher;
import com.apptitle.teacher.repository.TeacherRepository;
import com.apptitle.user.dto.UpdateProfileRequest;
import com.apptitle.user.entity.Role;
import com.apptitle.user.entity.User;
import com.apptitle.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfileServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final StudentRepository students = mock(StudentRepository.class);
    private final TeacherRepository teachers = mock(TeacherRepository.class);
    private final JwtService jwt = mock(JwtService.class);
    private final ProfileService service = new ProfileService(users, students, teachers, jwt);
    private User user;
    private Student student;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("old@example.com");
        user.setRole(Role.STUDENT);
        student = new Student();
        student.setUser(user);
        student.setName("Old Name");
        student.setLrn("123456789012");
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(students.findByUserId(user.getId())).thenReturn(Optional.of(student));
    }

    @Test
    void updateStudent_savesDetailsAndIssuesSessionForNewEmail() {
        when(jwt.generateToken(user.getId(), "new@example.com", Role.STUDENT)).thenReturn("new-token");
        var result = service.update(user.getEmail(), new UpdateProfileRequest(" New Name ", "new@example.com", "123456789013", null));
        assertEquals("New Name", student.getName());
        assertEquals("123456789013", result.lrn());
        assertEquals("new@example.com", user.getEmail());
        assertEquals("new-token", result.token());
        verify(users).flush();
    }

    @Test
    void update_rejectsDuplicateEmailWithoutChangingProfile() {
        User other = new User();
        other.setId(UUID.randomUUID());
        when(users.findByEmailIgnoreCase("taken@example.com")).thenReturn(Optional.of(other));
        assertThrows(ApiException.class, () -> service.update(user.getEmail(), new UpdateProfileRequest("New Name", "taken@example.com", student.getLrn(), null)));
        assertEquals("Old Name", student.getName());
    }

    @Test
    void update_rejectsDuplicateLrn() {
        when(students.existsByLrn("123456789013")).thenReturn(true);
        assertThrows(ApiException.class, () -> service.update(user.getEmail(), new UpdateProfileRequest("New", user.getEmail(), "123456789013", null)));
        assertEquals("123456789012", student.getLrn());
    }

    @Test
    void update_allowsUnchangedLrnAndEmail() {
        assertEquals(student.getLrn(), service.update(user.getEmail(), new UpdateProfileRequest("New", user.getEmail(), student.getLrn(), null)).lrn());
        verify(students, never()).existsByLrn(anyString());
    }

    @Test
    void update_rejectsInvalidLrn() {
        assertThrows(ApiException.class, () -> service.update(user.getEmail(), new UpdateProfileRequest("New", user.getEmail(), "bad", null)));
    }

    @Test
    void update_rejectsInvalidPicture() {
        assertThrows(ApiException.class, () -> service.update(user.getEmail(), new UpdateProfileRequest("New", user.getEmail(), student.getLrn(), "data:image/png;base64,invalid")));
    }

    @Test
    void update_savesAndRemovesRasterPicture() {
        String picture = null;
        try {
            var output = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_ARGB), "png", output);
            picture = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (java.io.IOException exception) { fail(exception); }
        assertEquals(picture, service.update(user.getEmail(), new UpdateProfileRequest("New", user.getEmail(), student.getLrn(), picture)).profilePicture());
        assertNull(service.update(user.getEmail(), new UpdateProfileRequest("New", user.getEmail(), student.getLrn(), null)).profilePicture());
    }

    @Test
    void get_rejectsDisabledAccount() {
        user.setEnabled(false);
        assertThrows(ApiException.class, () -> service.get(user.getEmail()));
    }

    @Test
    void get_rejectsMissingAccount() {
        assertThrows(ApiException.class, () -> service.get("missing@example.com"));
    }

    @Test
    void updateTeacher_doesNotRequireLrn() {
        user.setRole(Role.TEACHER);
        Teacher teacher = new Teacher();
        teacher.setName("Teacher");
        when(teachers.findByUserId(user.getId())).thenReturn(Optional.of(teacher));
        var result = service.update(user.getEmail(), new UpdateProfileRequest("Updated Teacher", user.getEmail(), null, null));
        assertEquals("Updated Teacher", result.name());
        assertNull(result.lrn());
        verifyNoInteractions(students);
    }
}
