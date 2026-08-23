package com.apptitle.joinrequest.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateJoinRequestRequest(

        @NotBlank(message = "Classroom code is required")
        String classroomCode
) {
}
