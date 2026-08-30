package com.apptitle.activity.dto;

import com.apptitle.activity.entity.SubmissionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ActivitySubmissionResponse(
        UUID submissionId,
        UUID studentId,
        String studentName,
        String studentLrn,
        SubmissionStatus status,
        Instant submittedAt,
        ActivityFileResponse file,
        BigDecimal score,
        BigDecimal perfectScore
) {
}
