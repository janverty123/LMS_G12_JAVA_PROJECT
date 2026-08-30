package com.apptitle.score.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

public record ReviewScoreProposalRequest(
        @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal approvedScore
) {
}
