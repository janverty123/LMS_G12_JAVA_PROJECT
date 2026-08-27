package com.apptitle.material.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CompleteUploadRequest(
        @NotBlank(message = "Upload ID is required")
        String uploadId,

        @NotBlank(message = "File key is required")
        String fileKey,

        @NotEmpty(message = "At least one uploaded part is required")
        List<@Valid CompletedPart> parts
) {
    public record CompletedPart(
            @Positive(message = "Part number must be positive")
            int partNumber,

            @NotBlank(message = "ETag is required")
            String eTag
    ) {
    }
}
