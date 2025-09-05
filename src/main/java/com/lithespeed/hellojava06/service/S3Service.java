package com.lithespeed.hellojava06.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import javax.annotation.processing.Generated;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * Debug method to show what credentials the S3AsyncClient is actually using
     * Useful for debugging AWS authentication issues in different environments
     */
    public Map<String, String> debugCredentials() {
        Map<String, String> result = new HashMap<>();
        
        try {
            // Get the credentials provider from the S3AsyncClient
            var credentialsProvider = s3AsyncClient.serviceClientConfiguration().credentialsProvider();
            result.put("credentials-provider-type", credentialsProvider.getClass().getSimpleName());
            
            // Try to resolve actual credentials (don't log the actual keys for security)
            try {
                // Use DefaultCredentialsProvider directly to get the same credentials the S3AsyncClient would use
                DefaultCredentialsProvider defaultProvider = DefaultCredentialsProvider.create();
                AwsCredentials creds = defaultProvider.resolveCredentials();
                result.put("credential-type", creds.getClass().getSimpleName());
                result.put("has-access-key", creds.accessKeyId() != null && !creds.accessKeyId().isEmpty() ? "true" : "false");
                result.put("access-key-prefix", creds.accessKeyId() != null && creds.accessKeyId().length() > 4 
                    ? creds.accessKeyId().substring(0, 4) + "****" : "null");
                
                // Check if it's a session credentials (indicates IAM role/STS)
                if (creds instanceof software.amazon.awssdk.auth.credentials.AwsSessionCredentials) {
                    software.amazon.awssdk.auth.credentials.AwsSessionCredentials sessionCreds = 
                        (software.amazon.awssdk.auth.credentials.AwsSessionCredentials) creds;
                    result.put("has-session-token", sessionCreds.sessionToken() != null ? "true" : "false");
                    result.put("session-token-prefix", sessionCreds.sessionToken() != null && sessionCreds.sessionToken().length() > 10
                        ? sessionCreds.sessionToken().substring(0, 10) + "****" : "null");
                    result.put("is-session-credentials", "true");
                } else {
                    result.put("is-session-credentials", "false");
                }
                
            } catch (Exception credErr) {
                result.put("credential-resolution-error", credErr.getMessage());
            }
            
            // Additional debug info
            result.put("bucket-name", this.bucketName);
            result.put("s3-client-region", s3AsyncClient.serviceClientConfiguration().region().id());
            
        } catch (Exception e) {
            result.put("debug-error", e.getMessage());
            result.put("error-type", e.getClass().getSimpleName());
        }
        
        logger.info("S3Service credentials debug: {}", result);
        return result;
    }
}
