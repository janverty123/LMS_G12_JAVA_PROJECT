package com.apptitle.section.dto;

import java.util.UUID;

public record SectionResponse(
        UUID id,
        String name,
        String subjectName,
        String classCode
) {
}
