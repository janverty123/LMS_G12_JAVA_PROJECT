package com.apptitle.joinrequest.dto;

import java.time.Instant;
import java.util.UUID;

public record SectionMemberResponse(
        UUID studentId,
        String studentName,
        String studentLrn,
        Instant joinedAt
) {
}
