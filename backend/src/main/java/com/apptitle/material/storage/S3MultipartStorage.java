package com.apptitle.material.storage;

import com.apptitle.config.MinioProperties;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class S3MultipartStorage implements MultipartStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final MinioProperties properties;

    public S3MultipartStorage(
            S3Client s3Client,
            S3Presigner s3Presigner,
            MinioProperties properties
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    @Override
    public String initiateUpload(String storageKey) {
        try {
            return s3Client.createMultipartUpload(CreateMultipartUploadRequest.builder()
                            .bucket(properties.bucket())
                            .key(storageKey)
                            .build())
                    .uploadId();
        } catch (Exception exception) {
            throw new ObjectStorageException("Unable to initialize multipart upload.", exception);
        }
    }

    @Override
    public List<PresignedPart> presignUploadParts(
            String storageKey,
            String uploadId,
            int totalParts
    ) {
        List<PresignedPart> urls = new ArrayList<>(totalParts);
        Duration expiry = Duration.ofSeconds(properties.presignedExpirySeconds());
        try {
            for (int partNumber = 1; partNumber <= totalParts; partNumber++) {
                UploadPartRequest request = UploadPartRequest.builder()
                        .bucket(properties.bucket())
                        .key(storageKey)
                        .uploadId(uploadId)
                        .partNumber(partNumber)
                        .build();
                String url = s3Presigner.presignUploadPart(UploadPartPresignRequest.builder()
                                .signatureDuration(expiry)
                                .uploadPartRequest(request)
                                .build())
                        .url()
                        .toString();
                urls.add(new PresignedPart(partNumber, url));
            }
            return List.copyOf(urls);
        } catch (Exception exception) {
            throw new ObjectStorageException("Unable to create upload URLs.", exception);
        }
    }

    @Override
    public void completeUpload(
            String storageKey,
            String uploadId,
            List<CompletedPart> completedParts
    ) {
        var parts = completedParts.stream()
                .sorted(Comparator.comparingInt(CompletedPart::partNumber))
                .map(part -> software.amazon.awssdk.services.s3.model.CompletedPart.builder()
                        .partNumber(part.partNumber())
                        .eTag(part.eTag())
                        .build())
                .toList();
        try {
            s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                    .bucket(properties.bucket())
                    .key(storageKey)
                    .uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder().parts(parts).build())
                    .build());
        } catch (Exception exception) {
            throw new ObjectStorageException("Unable to complete multipart upload.", exception);
        }
    }

    @Override
    public String presignDownload(String storageKey) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(storageKey)
                    .build();
            return s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                            .signatureDuration(Duration.ofSeconds(properties.presignedExpirySeconds()))
                            .getObjectRequest(request)
                            .build())
                    .url()
                    .toString();
        } catch (Exception exception) {
            throw new ObjectStorageException("Unable to create download URL.", exception);
        }
    }
}
