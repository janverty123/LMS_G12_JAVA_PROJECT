package com.apptitle.material.dto;

import java.time.Instant;
import java.util.UUID;

public record MaterialDownloadResponse(
        UUID materialId,
        String fileName,
        String contentType,
        long fileSizeBytes,
        String url,
        Instant expiresAt
) {
}
