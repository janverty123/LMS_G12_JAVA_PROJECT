package com.apptitle.joinrequest.dto;

import java.util.UUID;

public record StudentSectionResponse(
        UUID sectionId,
        String sectionName,
        String subjectName,
        String teacherName
) {
}
