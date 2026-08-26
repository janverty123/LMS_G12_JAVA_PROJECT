package com.apptitle.subject.dto;

import java.util.UUID;

/**
 * DTO for returning Subject information.
 */
public record SubjectResponse(
    UUID id,
    String name,
    String subjectCode,
    String subjectTeacherName
) {}