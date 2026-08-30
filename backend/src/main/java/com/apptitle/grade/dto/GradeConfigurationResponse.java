package com.apptitle.grade.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record GradeConfigurationResponse(
        UUID classSectionId,
        UUID subjectId,
        BigDecimal writtenActivityWeight,
        BigDecimal performanceTaskWeight,
        BigDecimal testWeight
) {
}
