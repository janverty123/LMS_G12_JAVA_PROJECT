package com.apptitle.activity.service;

import com.apptitle.activity.dto.ActivityResponse;
import com.apptitle.activity.dto.SaveActivityRequest;
import com.apptitle.activity.entity.Activity;
import com.apptitle.activity.repository.ActivityRepository;
import com.apptitle.classsection.entity.ClassEnrollmentRequestStatus;
import com.apptitle.classsection.repository.ClassEnrollmentRequestRepository;
import com.apptitle.common.exception.ApiException;
import com.apptitle.student.entity.Student;
import com.apptitle.student.repository.StudentRepository;
import com.apptitle.subject.entity.ClassSubjectLink;
import com.apptitle.subject.entity.ClassSubjectLinkStatus;
import com.apptitle.subject.repository.ClassSubjectLinkRepository;
import com.apptitle.teacher.entity.Teacher;
import com.apptitle.teacher.repository.TeacherRepository;
import com.apptitle.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.apptitle.notification.entity.NotificationType;
import com.apptitle.notification.service.NotificationService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ClassSubjectLinkRepository classSubjectLinkRepository;
    private final ClassEnrollmentRequestRepository enrollmentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private NotificationService notificationService;

    @Autowired(required = false)
    void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public ActivityService(
            ActivityRepository activityRepository,
            ClassSubjectLinkRepository classSubjectLinkRepository,
            ClassEnrollmentRequestRepository enrollmentRepository,
            TeacherRepository teacherRepository,
            StudentRepository studentRepository,
            UserRepository userRepository
    ) {
        this.activityRepository = activityRepository;
        this.classSubjectLinkRepository = classSubjectLinkRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ActivityResponse create(
            String email,
            UUID classSectionId,
            UUID subjectId,
            SaveActivityRequest request
    ) {
        Teacher teacher = resolveTeacher(email);
        ClassSubjectLink link = resolveApprovedLink(classSectionId, subjectId);
        requireSubjectOwner(teacher, link);

        Activity activity = new Activity();
        activity.setClassSubjectLink(link);
        activity.setCreatedBy(teacher);
        apply(activity, request);
        activity = activityRepository.save(activity);
        if (notificationService != null) {
            notificationService.notifyApprovedStudents(link.getClassSection(),
                    NotificationType.NEW_ACTIVITY, "New activity: " + activity.getTitle(),
                    activity.getId().toString());
            if (activity.isAllowStudentSelfSubmissionScore()) {
                notificationService.notifyApprovedStudents(link.getClassSection(),
                        NotificationType.SCORE_SUBMISSION_REQUEST,
                        "Score submission enabled for: " + activity.getTitle(),
                        activity.getId().toString());
            }
        }
        return toResponse(activity);
    }

    @Transactional
    public ActivityResponse update(String email, UUID activityId, SaveActivityRequest request) {
        Teacher teacher = resolveTeacher(email);
        Activity activity = resolveActivity(activityId);
        requireSubjectOwner(teacher, activity.getClassSubjectLink());
        apply(activity, request);
        return toResponse(activityRepository.save(activity));
    }

    @Transactional
    public void delete(String email, UUID activityId) {
        Teacher teacher = resolveTeacher(email);
        Activity activity = resolveActivity(activityId);
        requireSubjectOwner(teacher, activity.getClassSubjectLink());
        activityRepository.delete(activity);
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> listForTeacher(
            String email,
            UUID classSectionId,
            UUID subjectId
    ) {
        Teacher teacher = resolveTeacher(email);
        ClassSubjectLink link = resolveApprovedLink(classSectionId, subjectId);
        requireSubjectOwner(teacher, link);
        return list(link);
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> listForStudent(String email, UUID subjectId) {
        Student student = resolveStudent(email);
        UUID classSectionId = enrollmentRepository
                .findByStudentIdAndStatus(student.getId(), ClassEnrollmentRequestStatus.APPROVED)
                .stream()
                .findFirst()
                .orElseThrow(() -> ApiException.forbidden(
                        "You need an approved class enrollment to access activities."))
                .getClassSection()
                .getId();
        return list(resolveApprovedLink(classSectionId, subjectId));
    }

    private List<ActivityResponse> list(ClassSubjectLink link) {
        return activityRepository.findByClassSubjectLinkIdOrderByDeadlineAsc(link.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void apply(Activity activity, SaveActivityRequest request) {
        activity.setType(request.type());
        activity.setTitle(request.title().trim());
        activity.setPerfectScore(request.perfectScore());
        activity.setDeadline(request.deadline());
        activity.setInstructions(request.instructions().trim());
        activity.setAllowStudentSelfSubmissionScore(
                request.allowStudentSelfSubmissionScore());
    }

    private ClassSubjectLink resolveApprovedLink(UUID classSectionId, UUID subjectId) {
        ClassSubjectLink link = classSubjectLinkRepository
                .findByClassSectionIdAndSubjectId(classSectionId, subjectId)
                .orElseThrow(() -> ApiException.notFound("Class-subject link not found."));
        if (link.getStatus() != ClassSubjectLinkStatus.APPROVED) {
            throw ApiException.forbidden("The subject link is not approved.");
        }
        return link;
    }

    private Activity resolveActivity(UUID id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Activity not found."));
    }

    private Teacher resolveTeacher(String email) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
        return teacherRepository.findByUserId(user.getId())
                .orElseThrow(() -> ApiException.forbidden("Only teachers can manage activities."));
    }

    private Student resolveStudent(String email) {
        var user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
        return studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> ApiException.forbidden("Only students can access student activities."));
    }

    private void requireSubjectOwner(Teacher teacher, ClassSubjectLink link) {
        if (!link.getSubject().getSubjectTeacher().getId().equals(teacher.getId())) {
            throw ApiException.forbidden("You do not own this subject.");
        }
    }

    private ActivityResponse toResponse(Activity activity) {
        ClassSubjectLink link = activity.getClassSubjectLink();
        return new ActivityResponse(
                activity.getId(),
                link.getClassSection().getId(),
                link.getSubject().getId(),
                link.getSubject().getName(),
                activity.getType(),
                activity.getType().getLabel(),
                activity.getTitle(),
                activity.getPerfectScore(),
                activity.getDeadline(),
                activity.getInstructions(),
                activity.isAllowStudentSelfSubmissionScore(),
                activity.getCreatedAt(),
                activity.getUpdatedAt()
        );
    }
}
