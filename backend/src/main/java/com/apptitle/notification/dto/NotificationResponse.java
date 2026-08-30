package com.apptitle.notification.dto;

import com.apptitle.notification.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String message,
        String referenceKey,
        boolean read,
        Instant createdAt
) {
}
