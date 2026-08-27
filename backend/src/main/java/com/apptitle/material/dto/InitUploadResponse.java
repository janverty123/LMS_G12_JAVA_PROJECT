package com.apptitle.material.dto;

import java.util.List;
import java.util.UUID;

public record InitUploadResponse(
        UUID materialId,
        String uploadId,
        String fileKey,
        long chunkSize,
        int totalParts,
        List<PresignedPartUrl> presignedUrls
) {
    public record PresignedPartUrl(int partNumber, String url) {
    }
}
