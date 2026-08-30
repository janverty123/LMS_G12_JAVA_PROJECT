package com.apptitle.activity.dto;

import com.apptitle.activity.entity.ActivityType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ActivityResponse(
        UUID id,
        UUID classSectionId,
        UUID subjectId,
        String subjectName,
        ActivityType type,
        String typeLabel,
        String title,
        BigDecimal perfectScore,
        Instant deadline,
        String instructions,
        boolean allowStudentSelfSubmissionScore,
        Instant createdAt,
        Instant updatedAt
) {
}
