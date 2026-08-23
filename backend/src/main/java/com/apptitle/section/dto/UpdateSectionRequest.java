package com.apptitle.section.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateSectionRequest(

        @NotBlank(message = "Section name is required")
        String name,

        @NotBlank(message = "Subject name is required")
        String subjectName
) {
}
