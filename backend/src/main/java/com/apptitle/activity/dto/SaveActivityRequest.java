package com.apptitle.activity.dto;

import com.apptitle.activity.entity.ActivityType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record SaveActivityRequest(
        @NotNull(message = "Activity type is required")
        ActivityType type,

        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must be at most 150 characters")
        String title,

        @NotNull(message = "Perfect score is required")
        @DecimalMin(value = "0.01", message = "Perfect score must be greater than zero")
        @Digits(integer = 8, fraction = 2, message = "Perfect score must have at most two decimal places")
        BigDecimal perfectScore,

        @NotNull(message = "Deadline is required")
        @Future(message = "Deadline must be in the future")
        Instant deadline,

        @NotBlank(message = "Instructions are required")
        @Size(max = 5000, message = "Instructions must be at most 5000 characters")
        String instructions,

        boolean allowStudentSelfSubmissionScore
) {
}
