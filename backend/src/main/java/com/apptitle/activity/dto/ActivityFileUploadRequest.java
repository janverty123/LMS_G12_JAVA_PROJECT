package com.apptitle.activity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ActivityFileUploadRequest(
        @NotBlank @Size(max = 255) String fileName,
        @Positive long totalSizeBytes,
        @NotBlank @Size(max = 150) String contentType
) {
}
