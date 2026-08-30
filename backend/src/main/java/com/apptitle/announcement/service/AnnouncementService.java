package com.apptitle.announcement.service;

import com.apptitle.announcement.dto.AnnouncementResponse;
import com.apptitle.announcement.dto.SaveAnnouncementRequest;
import com.apptitle.announcement.entity.Announcement;
import com.apptitle.announcement.repository.AnnouncementRepository;
import com.apptitle.classsection.entity.ClassEnrollmentRequestStatus;
import com.apptitle.classsection.entity.ClassSection;
import com.apptitle.classsection.repository.ClassEnrollmentRequestRepository;
import com.apptitle.classsection.repository.ClassSectionRepository;
import com.apptitle.common.exception.ApiException;
import com.apptitle.notification.entity.NotificationType;
import com.apptitle.notification.service.NotificationService;
import com.apptitle.student.repository.StudentRepository;
import com.apptitle.teacher.entity.Teacher;
import com.apptitle.teacher.repository.TeacherRepository;
import com.apptitle.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AnnouncementService {
    private final AnnouncementRepository announcementRepository;
    private final ClassSectionRepository classSectionRepository;
    private final ClassEnrollmentRequestRepository enrollmentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public AnnouncementService(AnnouncementRepository announcementRepository,
            ClassSectionRepository classSectionRepository,
            ClassEnrollmentRequestRepository enrollmentRepository,
            TeacherRepository teacherRepository, StudentRepository studentRepository,
            UserRepository userRepository, NotificationService notificationService) {
        this.announcementRepository = announcementRepository;
        this.classSectionRepository = classSectionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public AnnouncementResponse create(String email, UUID classId,
            SaveAnnouncementRequest request) {
        Teacher teacher = resolveTeacher(email);
        ClassSection section = ownedSection(teacher, classId);
        Announcement value = new Announcement();
        value.setClassSection(section);
        value.setAuthor(teacher);
        apply(value, request);
        value = announcementRepository.save(value);
        notificationService.notifyApprovedStudents(section, NotificationType.NEW_ANNOUNCEMENT,
                "New announcement: " + value.getTitle(), value.getId().toString());
        return response(value);
    }

    @Transactional
    public AnnouncementResponse update(String email, UUID id, SaveAnnouncementRequest request) {
        Teacher teacher = resolveTeacher(email);
        Announcement value = resolve(id);
        ownedSection(teacher, value.getClassSection().getId());
        apply(value, request);
        return response(announcementRepository.save(value));
    }

    @Transactional
    public void delete(String email, UUID id) {
        Teacher teacher = resolveTeacher(email);
        Announcement value = resolve(id);
        ownedSection(teacher, value.getClassSection().getId());
        announcementRepository.delete(value);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> teacherList(String email, UUID classId) {
        Teacher teacher = resolveTeacher(email);
        ownedSection(teacher, classId);
        return list(classId);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> studentList(String email) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
        var student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> ApiException.forbidden("Only students can view student announcements."));
        var enrollment = enrollmentRepository.findByStudentIdAndStatus(
                        student.getId(), ClassEnrollmentRequestStatus.APPROVED)
                .stream().findFirst().orElseThrow(() -> ApiException.forbidden(
                        "Approved class enrollment is required."));
        return list(enrollment.getClassSection().getId());
    }

    private List<AnnouncementResponse> list(UUID classId) {
        return announcementRepository.findByClassSectionIdOrderByCreatedAtDesc(classId)
                .stream().map(this::response).toList();
    }

    private ClassSection ownedSection(Teacher teacher, UUID id) {
        ClassSection section = classSectionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Class section not found."));
        if (!section.getAdviser().getId().equals(teacher.getId())) {
            throw ApiException.forbidden("Only the class adviser can manage announcements.");
        }
        return section;
    }

    private Teacher resolveTeacher(String email) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
        return teacherRepository.findByUserId(user.getId())
                .orElseThrow(() -> ApiException.forbidden("Only teachers can manage announcements."));
    }

    private Announcement resolve(UUID id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Announcement not found."));
    }

    private void apply(Announcement value, SaveAnnouncementRequest request) {
        value.setTitle(request.title().trim());
        value.setContent(request.content().trim());
    }

    private AnnouncementResponse response(Announcement value) {
        return new AnnouncementResponse(value.getId(), value.getClassSection().getId(),
                value.getTitle(), value.getContent(), value.getAuthor().getName(),
                value.getCreatedAt(), value.getUpdatedAt());
    }
}
