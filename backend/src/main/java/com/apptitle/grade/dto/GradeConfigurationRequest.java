package com.apptitle.grade.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record GradeConfigurationRequest(
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal writtenActivityWeight,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal performanceTaskWeight,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal testWeight
) {
}
