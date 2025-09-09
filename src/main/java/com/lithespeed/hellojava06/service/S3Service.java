package com.lithespeed.hellojava06.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class S3Service {

    private final S3AsyncClient s3AsyncClient;
    
    @Value("${aws.s3.bucket-name:test-bucket}")
    private String bucketName;

    public S3Service(S3AsyncClient s3AsyncClient) {
        this.s3AsyncClient = s3AsyncClient;
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

            AsyncRequestBody requestBody = AsyncRequestBody.fromBytes(file.getBytes());

            return s3AsyncClient.putObject(putRequest, requestBody)
                    .thenApply(response -> key);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(new RuntimeException("Failed to read file content", e));
        }
    }

    /**
     * List all files in the S3 bucket
     */
    public CompletableFuture<List<String>> listFilesAsync() {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();

        return s3AsyncClient.listObjectsV2(request)
                .thenApply(response -> response.contents()
                        .stream()
                        .map(S3Object::key)
                        .collect(Collectors.toList()));
    }
}
