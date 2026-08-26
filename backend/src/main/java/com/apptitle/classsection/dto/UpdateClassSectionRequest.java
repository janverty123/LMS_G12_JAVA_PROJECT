package com.apptitle.classsection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for updating a ClassSection.
 * Note: classCode is intentionally not editable.
 */
public record UpdateClassSectionRequest(
    @NotBlank(message = "Grade level is required")
    String gradeLevel,
    
    @NotBlank(message = "Section is required")
    String section,
    
    @NotBlank(message = "School year is required")
    @Size(min = 9, max = 9, message = "School year must be in format YYYY-YYYY")
    String schoolYear
) {}