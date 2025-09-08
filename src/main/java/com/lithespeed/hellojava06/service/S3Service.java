package com.lithespeed.hellojava06.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import javax.annotation.processing.Generated;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class S3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);
    
    private final S3AsyncClient s3AsyncClient;
    
    @Value("${aws.s3.bucket-name:test-bucket}")
    private String bucketName;
    
    @Value("${aws.s3.presign-duration:PT15M}")
    private Duration presignDuration;

    @Autowired
    public S3Service(S3AsyncClient s3AsyncClient) {
        this.s3AsyncClient = s3AsyncClient;
        logger.info("S3Service initialized with S3AsyncClient CRT");
    }

    // For testing with injected S3AsyncClient
    @Generated("test-only-constructor")
    public S3Service(S3AsyncClient s3AsyncClient, String bucketName) {
        this.s3AsyncClient = s3AsyncClient;
        this.bucketName = bucketName;
    }

    /**
     * Uploads provided {@link MultipartFile} to configured S3 Bucket using key {@code
     * entityType/entityId/UUID.extension}.
     *
     * @param entityId ID of the entity for which this document is being uploaded
     * @param entityType Type of the entity for which this document is being uploaded
     * @param file Actual file to be uploaded
     * @return {@code entityType/entityId/UUID.extension}
     */
    public CompletableFuture<String> uploadFileAsync(String entityType, String entityId, MultipartFile file) {
        if (file.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("File cannot be empty"));
        }

        try {
            String originalFileName = file.getOriginalFilename();
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            
            String key = String.format("%s/%s/%s%s", entityType, entityId, UUID.randomUUID().toString(), extension);
            
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            AsyncRequestBody requestBody = AsyncRequestBody.fromInputStream(
                file.getInputStream(), file.getSize(), 
                java.util.concurrent.ForkJoinPool.commonPool());

            logger.info("Starting async upload for key: {} to bucket: {}", key, bucketName);

            return s3AsyncClient.putObject(putRequest, requestBody)
                    .thenApply(response -> {
                        logger.info("Successfully uploaded file with key: {}, ETag: {}", key, response.eTag());
                        return key;
                    })
                    .exceptionally(throwable -> {
                        logger.error("Failed to upload file with key: {}", key, throwable);
                        throw new RuntimeException("Failed to upload file to S3", throwable);
                    });

        } catch (IOException e) {
            return CompletableFuture.failedFuture(new RuntimeException("Failed to read file content", e));
        }
    }

    /**
     * Async version of listFiles
     */
    public CompletableFuture<List<String>> listFilesAsync() {
        logger.info("Async listing files from bucket: {}", bucketName);

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();

        return s3AsyncClient.listObjectsV2(request)
                .thenApply(response -> {
                    List<String> fileKeys = response.contents()
                            .stream()
                            .map(S3Object::key)
                            .collect(Collectors.toList());
                    
                    logger.info("Successfully async listed {} files from bucket: {}", fileKeys.size(), bucketName);
                    return fileKeys;
                })
                .exceptionally(throwable -> {
                    logger.error("Failed to async list files from bucket: {}", bucketName, throwable);
                    throw new RuntimeException("Failed to list files from S3", throwable);
                });
    }
}
