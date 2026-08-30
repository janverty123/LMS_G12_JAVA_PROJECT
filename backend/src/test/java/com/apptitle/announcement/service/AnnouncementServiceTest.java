package com.apptitle.announcement.service;

import com.apptitle.announcement.entity.Announcement;
import com.apptitle.announcement.repository.AnnouncementRepository;
import com.apptitle.classsection.entity.ClassEnrollmentRequest;
import com.apptitle.classsection.entity.ClassEnrollmentRequestStatus;
import com.apptitle.classsection.entity.ClassSection;
import com.apptitle.classsection.repository.ClassEnrollmentRequestRepository;
import com.apptitle.classsection.repository.ClassSectionRepository;
import com.apptitle.common.exception.ApiException;
import com.apptitle.notification.service.NotificationService;
import com.apptitle.student.entity.Student;
import com.apptitle.student.repository.StudentRepository;
import com.apptitle.teacher.entity.Teacher;
import com.apptitle.teacher.repository.TeacherRepository;
import com.apptitle.user.entity.User;
import com.apptitle.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class AnnouncementServiceTest {
    @Mock private AnnouncementRepository repository;
    @Mock private ClassSectionRepository sectionRepository;
    @Mock private ClassEnrollmentRequestRepository enrollmentRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    private AnnouncementService service;
    private User user;
    private Student student;
    private ClassSection section;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AnnouncementService(repository, sectionRepository, enrollmentRepository,
                teacherRepository, studentRepository, userRepository, notificationService);
        user = new User(); user.setId(UUID.randomUUID()); user.setEmail("student@example.com");
        student = new Student(); student.setId(UUID.randomUUID()); student.setUser(user);
        section = new ClassSection(); section.setId(UUID.randomUUID());
        when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(studentRepository.findByUserId(user.getId())).thenReturn(Optional.of(student));
    }

    @Test
    void studentListRequiresApprovedMembership() {
        when(enrollmentRepository.findByStudentIdAndStatus(student.getId(),
                ClassEnrollmentRequestStatus.APPROVED)).thenReturn(List.of());
        assertEquals(403, assertThrows(ApiException.class, () ->
                service.studentList(user.getEmail())).getStatus().value());
    }

    @Test
    void studentListUsesOnlyApprovedStudentsSection() {
        ClassEnrollmentRequest enrollment = new ClassEnrollmentRequest();
        enrollment.setStudent(student); enrollment.setClassSection(section);
        enrollment.setStatus(ClassEnrollmentRequestStatus.APPROVED);
        Announcement announcement = new Announcement(); announcement.setId(UUID.randomUUID());
        announcement.setClassSection(section); announcement.setAuthor(new Teacher());
        announcement.getAuthor().setName("Adviser"); announcement.setTitle("Notice");
        announcement.setContent("Class only");
        when(enrollmentRepository.findByStudentIdAndStatus(student.getId(),
                ClassEnrollmentRequestStatus.APPROVED)).thenReturn(List.of(enrollment));
        when(repository.findByClassSectionIdOrderByCreatedAtDesc(section.getId()))
                .thenReturn(List.of(announcement));
        assertEquals(1, service.studentList(user.getEmail()).size());
    }
}
