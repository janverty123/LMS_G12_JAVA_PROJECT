package com.apptitle.progress.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProgressConfigurationRequest(
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal onTrackMinimum,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal needsAttentionMinimum
) {
}
