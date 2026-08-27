package com.apptitle.subject.dto;

import com.apptitle.subject.entity.ClassSubjectLinkStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for returning ClassSection-Subject link information.
 */
public record ClassSubjectLinkResponse(
    UUID id,
    UUID classSectionId,
    String classSectionName,
    String schoolYear,
    String requestingAdviserName,
    UUID subjectId,
    String subjectName,
    ClassSubjectLinkStatus status,
    Instant createdAt,
    Instant updatedAt
) {}
