package com.apptitle.grade.dto;

import com.apptitle.activity.entity.ActivityType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record GradebookResponse(
        GradeConfigurationResponse configuration,
        List<ActivityColumn> activities,
        List<StudentGrade> students
) {
    public record ActivityColumn(UUID id, String title, ActivityType type, BigDecimal perfectScore) {
    }

    public record ActivityScore(UUID activityId, BigDecimal score, BigDecimal perfectScore) {
    }

    public record StudentGrade(
            UUID studentId,
            String studentName,
            String studentLrn,
            List<ActivityScore> scores,
            BigDecimal writtenActivityAverage,
            BigDecimal performanceTaskAverage,
            BigDecimal testAverage,
            BigDecimal finalGrade
    ) {
    }
}
