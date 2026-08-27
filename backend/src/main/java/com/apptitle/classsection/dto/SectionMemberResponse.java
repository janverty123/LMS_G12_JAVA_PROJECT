package com.apptitle.classsection.dto;

import com.apptitle.classsection.entity.ClassEnrollmentRequestStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for returning class section member information.
 */
public record SectionMemberResponse(
    UUID studentId,
    String studentName,
    String studentLrn,
    String studentEmail,
    ClassEnrollmentRequestStatus status,
    Instant enrollmentDate
) {}
