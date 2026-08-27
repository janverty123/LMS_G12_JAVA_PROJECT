package com.apptitle.material.storage;

import com.apptitle.config.MinioProperties;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioAsyncClient;
import io.minio.http.Method;
import io.minio.messages.Part;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class MinioMultipartStorage implements MultipartStorage {

    private final MultipartMinioClient multipartClient;
    private final MinioProperties properties;

    public MinioMultipartStorage(MinioAsyncClient minioClient, MinioProperties properties) {
        this.multipartClient = new MultipartMinioClient(minioClient);
        this.properties = properties;
    }

    @Override
    public String initiateUpload(String storageKey) {
        try {
            return multipartClient.initiate(properties.bucket(), storageKey);
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
        try {
            for (int partNumber = 1; partNumber <= totalParts; partNumber++) {
                String url = multipartClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.PUT)
                                .bucket(properties.bucket())
                                .object(storageKey)
                                .expiry(properties.presignedExpirySeconds())
                                .extraQueryParams(Map.of(
                                        "uploadId", uploadId,
                                        "partNumber", Integer.toString(partNumber)
                                ))
                                .build()
                );
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
        Part[] parts = completedParts.stream()
                .sorted(Comparator.comparingInt(CompletedPart::partNumber))
                .map(part -> new Part(part.partNumber(), part.eTag()))
                .toArray(Part[]::new);
        try {
            multipartClient.complete(properties.bucket(), storageKey, uploadId, parts);
        } catch (Exception exception) {
            throw new ObjectStorageException("Unable to complete multipart upload.", exception);
        }
    }

    @Override
    public String presignDownload(String storageKey) {
        try {
            return multipartClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(properties.bucket())
                            .object(storageKey)
                            .expiry(properties.presignedExpirySeconds())
                            .build()
            );
        } catch (Exception exception) {
            throw new ObjectStorageException("Unable to create download URL.", exception);
        }
    }

    private static final class MultipartMinioClient extends MinioAsyncClient {

        private MultipartMinioClient(MinioAsyncClient minioClient) {
            super(minioClient);
        }

        private String initiate(String bucket, String storageKey) throws Exception {
            return createMultipartUploadAsync(bucket, null, storageKey, null, null)
                    .get()
                    .result()
                    .uploadId();
        }

        private void complete(
                String bucket,
                String storageKey,
                String uploadId,
                Part[] parts
        ) throws Exception {
            completeMultipartUploadAsync(
                    bucket, null, storageKey, uploadId, parts, null, null).get();
        }
    }
}
