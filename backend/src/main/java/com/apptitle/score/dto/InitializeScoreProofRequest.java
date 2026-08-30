package com.apptitle.score.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record InitializeScoreProofRequest(
        @NotNull @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal reportedScore,
        @NotBlank @Size(max = 255) String fileName,
        @Positive long totalSizeBytes,
        @NotBlank @Size(max = 150) String contentType
) {
}
