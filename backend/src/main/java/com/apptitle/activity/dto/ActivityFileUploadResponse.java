package com.apptitle.activity.dto;

import java.util.List;
import java.util.UUID;

public record ActivityFileUploadResponse(
        UUID fileId,
        String uploadId,
        String fileKey,
        long chunkSize,
        int totalParts,
        List<PresignedPartUrl> presignedUrls
) {
    public record PresignedPartUrl(int partNumber, String url) {
    }
}
