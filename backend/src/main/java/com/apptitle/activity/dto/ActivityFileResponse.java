package com.apptitle.activity.dto;

import java.time.Instant;
import java.util.UUID;

public record ActivityFileResponse(
        UUID id,
        String fileName,
        String contentType,
        long fileSizeBytes,
        Instant completedAt
) {
}
