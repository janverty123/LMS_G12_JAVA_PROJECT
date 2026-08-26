package com.apptitle.classsection.dto;

import java.util.UUID;

/**
 * DTO for returning ClassSection information.
 */
public record ClassSectionResponse(
    UUID id,
    String gradeLevel,
    String section,
    String schoolYear,
    String classCode,
    String adviserName
) {}