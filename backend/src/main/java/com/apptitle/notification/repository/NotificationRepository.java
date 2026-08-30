package com.apptitle.notification.repository;

import com.apptitle.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId);
    Optional<Notification> findByIdAndRecipientId(UUID id, UUID recipientId);
    boolean existsByRecipientIdAndTypeAndReferenceKey(
            UUID recipientId, com.apptitle.notification.entity.NotificationType type,
            String referenceKey);
}
