package com.apptitle.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class S3Config {

    @Bean
    public S3Client s3Client(MinioProperties properties) {
        return S3Client.builder()
                .endpointOverride(URI.create(properties.endpoint()))
                .region(Region.of(properties.region()))
                .credentialsProvider(credentials(properties))
                .serviceConfiguration(s3Configuration())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(MinioProperties properties) {
        return S3Presigner.builder()
                .endpointOverride(URI.create(properties.endpoint()))
                .region(Region.of(properties.region()))
                .credentialsProvider(credentials(properties))
                .serviceConfiguration(s3Configuration())
                .build();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "minio",
            name = "initialize-bucket",
            havingValue = "true",
            matchIfMissing = true
    )
    public ApplicationRunner initializeBucket(S3Client s3Client, MinioProperties properties) {
        return args -> {
            try {
                s3Client.headBucket(request -> request.bucket(properties.bucket()));
            } catch (NoSuchBucketException exception) {
                s3Client.createBucket(CreateBucketRequest.builder()
                        .bucket(properties.bucket())
                        .build());
            } catch (S3Exception exception) {
                if (exception.statusCode() == 404) {
                    s3Client.createBucket(CreateBucketRequest.builder()
                            .bucket(properties.bucket())
                            .build());
                } else {
                    throw exception;
                }
            }
        };
    }

    private StaticCredentialsProvider credentials(MinioProperties properties) {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(
                properties.accessKey(), properties.secretKey()));
    }

    private S3Configuration s3Configuration() {
        return S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .checksumValidationEnabled(false)
                .build();
    }
}
