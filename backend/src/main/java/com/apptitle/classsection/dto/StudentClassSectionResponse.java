package com.apptitle.classsection.dto;

import java.util.UUID;

/**
 * DTO for returning a student's approved class section information.
 */
public record StudentClassSectionResponse(
    UUID classSectionId,
    String classSectionName,
    String schoolYear,
    String adviserName
) {}