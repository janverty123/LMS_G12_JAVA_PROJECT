package com.apptitle.material.storage;

import java.util.List;

public interface MultipartStorage {

    String initiateUpload(String storageKey);

    List<PresignedPart> presignUploadParts(
            String storageKey,
            String uploadId,
            int totalParts
    );

    void completeUpload(
            String storageKey,
            String uploadId,
            List<CompletedPart> parts
    );

    String presignDownload(String storageKey);

    record PresignedPart(int partNumber, String url) {
    }

    record CompletedPart(int partNumber, String eTag) {
    }
}
