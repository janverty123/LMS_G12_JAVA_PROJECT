package com.apptitle.subject.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for updating a Subject.
 */
public record UpdateSubjectRequest(
    @NotBlank(message = "Subject name is required")
    String name
) {}