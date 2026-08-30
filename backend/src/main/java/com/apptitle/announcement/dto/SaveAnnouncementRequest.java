package com.apptitle.announcement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveAnnouncementRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 5000) String content
) {
}
