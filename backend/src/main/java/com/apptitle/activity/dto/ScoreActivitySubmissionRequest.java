package com.apptitle.activity.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ScoreActivitySubmissionRequest(
        @NotNull @DecimalMin("0.0") @Digits(integer = 8, fraction = 2) BigDecimal score
) {
}
