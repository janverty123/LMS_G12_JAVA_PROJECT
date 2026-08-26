package com.apptitle.subject.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for creating a new Subject.
 */
public record CreateSubjectRequest(
    @NotBlank(message = "Subject name is required")
    String name
) {}