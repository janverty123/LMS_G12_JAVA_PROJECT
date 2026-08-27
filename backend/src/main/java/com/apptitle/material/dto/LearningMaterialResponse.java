package com.apptitle.material.dto;

import java.time.Instant;
import java.util.UUID;

public record LearningMaterialResponse(
        UUID id,
        UUID classSectionId,
        UUID subjectId,
        String subjectName,
        String title,
        String description,
        String fileName,
        String contentType,
        long fileSizeBytes,
        String uploadedBy,
        Instant createdAt,
        Instant completedAt
) {
}
