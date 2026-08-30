package com.apptitle.activity.dto;

import java.time.Instant;
import java.util.UUID;

public record ActivityFileDownloadResponse(
        UUID fileId,
        String fileName,
        String contentType,
        long fileSizeBytes,
        String url,
        Instant expiresAt
) {
}
