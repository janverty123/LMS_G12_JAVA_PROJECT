package com.apptitle.notification.service;

import com.apptitle.classsection.entity.ClassEnrollmentRequestStatus;
import com.apptitle.classsection.entity.ClassSection;
import com.apptitle.classsection.repository.ClassEnrollmentRequestRepository;
import com.apptitle.common.exception.ApiException;
import com.apptitle.notification.dto.NotificationResponse;
import com.apptitle.notification.entity.Notification;
import com.apptitle.notification.entity.NotificationType;
import com.apptitle.notification.repository.NotificationRepository;
import com.apptitle.user.entity.User;
import com.apptitle.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ClassEnrollmentRequestRepository enrollmentRepository;

    public NotificationService(NotificationRepository notificationRepository,
            UserRepository userRepository,
            ClassEnrollmentRequestRepository enrollmentRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional
    public void notifyUser(User recipient, NotificationType type,
            String message, String referenceKey) {
        if (notificationRepository.existsByRecipientIdAndTypeAndReferenceKey(
                recipient.getId(), type, referenceKey)) return;
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setMessage(message);
        notification.setReferenceKey(referenceKey);
        notificationRepository.save(notification);
    }

    @Transactional
    public void notifyApprovedStudents(ClassSection section, NotificationType type,
            String message, String referenceKey) {
        enrollmentRepository.findByClassSectionIdAndStatus(
                        section.getId(), ClassEnrollmentRequestStatus.APPROVED)
                .forEach(enrollment -> notifyUser(enrollment.getStudent().getUser(),
                        type, message, referenceKey));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(String email) {
        User user = resolveUser(email);
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public NotificationResponse markRead(String email, UUID notificationId) {
        User user = resolveUser(email);
        Notification notification = notificationRepository
                .findByIdAndRecipientId(notificationId, user.getId())
                .orElseThrow(() -> ApiException.notFound("Notification not found."));
        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllRead(String email) {
        User user = resolveUser(email);
        notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId())
                .stream().filter(notification -> !notification.isRead())
                .forEach(notification -> notification.setRead(true));
    }

    private User resolveUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Account not found."));
    }

    private NotificationResponse toResponse(Notification value) {
        return new NotificationResponse(value.getId(), value.getType(), value.getMessage(),
                value.getReferenceKey(), value.isRead(), value.getCreatedAt());
    }
}
