package com.apptitle.classsection.dto;

import com.apptitle.joinrequest.entity.JoinRequestStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for returning ClassEnrollmentRequest information.
 * Replaces the old JoinRequestResponse.
 */
public record ClassEnrollmentRequestResponse(
    UUID id,
    UUID studentId,
    String studentName,
    String studentLrn,
    UUID classSectionId,
    String classSectionName,
    String schoolYear,
    JoinRequestStatus status,
    Instant updatedAt
) {}