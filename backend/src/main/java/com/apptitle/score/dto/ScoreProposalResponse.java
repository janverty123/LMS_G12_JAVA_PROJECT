package com.apptitle.score.dto;

import com.apptitle.activity.dto.ActivityFileResponse;
import com.apptitle.score.entity.ScoreProposalStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ScoreProposalResponse(
        UUID id,
        UUID activityId,
        UUID studentId,
        String studentName,
        String studentLrn,
        BigDecimal reportedScore,
        BigDecimal approvedScore,
        BigDecimal perfectScore,
        ScoreProposalStatus status,
        ActivityFileResponse proofFile,
        Instant submittedAt,
        Instant reviewedAt
) {
}
