package com.apptitle.joinrequest.dto;

import com.apptitle.joinrequest.entity.JoinRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record JoinRequestResponse(
        UUID id,
        UUID studentId,
        String studentName,
        String studentLrn,
        UUID sectionId,
        String sectionName,
        String subjectName,
        JoinRequestStatus status,
        Instant updatedAt
) {
}
