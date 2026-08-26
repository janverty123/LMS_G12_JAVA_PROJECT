package com.apptitle.subject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating a new ClassSection-Subject link request.
 */
public record CreateClassSubjectLinkRequest(
    @NotBlank(message = "Subject code is required")
    @Size(min = 7, max = 7, message = "Subject code must be 7 characters")
    String subjectCode
) {}