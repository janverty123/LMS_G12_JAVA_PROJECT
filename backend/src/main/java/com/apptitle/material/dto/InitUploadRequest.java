package com.apptitle.material.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record InitUploadRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must be at most 150 characters")
        String title,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @NotBlank(message = "File name is required")
        @Size(max = 255, message = "File name must be at most 255 characters")
        String fileName,

        @Positive(message = "File size must be greater than zero")
        long totalSizeBytes,

        @NotBlank(message = "Content type is required")
        @Size(max = 150, message = "Content type must be at most 150 characters")
        String contentType
) {
}
