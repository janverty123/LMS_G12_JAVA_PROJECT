package com.apptitle.progress.dto;

import com.apptitle.progress.entity.ProgressStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record StudentProgressResponse(
        UUID studentId,
        String studentName,
        BigDecimal completionPercentage,
        int missingActivityCount,
        List<String> missingActivities,
        BigDecimal currentGrade,
        ProgressStatus status,
        String statusLabel
) {
}
