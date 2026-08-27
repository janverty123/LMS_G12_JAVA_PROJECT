package com.apptitle.classsection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClassJoinRequestRequest(
        @NotBlank(message = "Class code is required")
        @Size(min = 6, max = 6, message = "Class code must be 6 characters")
        String classCode
) {
}
