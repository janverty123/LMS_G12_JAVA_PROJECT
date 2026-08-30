package com.apptitle.announcement.dto;

import java.time.Instant;
import java.util.UUID;

public record AnnouncementResponse(
        UUID id,
        UUID classSectionId,
        String title,
        String content,
        String authorName,
        Instant createdAt,
        Instant updatedAt
) {
}
