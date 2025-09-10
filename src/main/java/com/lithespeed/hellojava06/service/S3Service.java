package com.lithespeed.hellojava06.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
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
     * Business logic method to handle complete file upload process
     */
    public CompletableFuture<Map<String, Object>> processFileUpload(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        logger.info("Starting async upload for file: {} (size: {} bytes)", fileName, file.getSize());

        return uploadFileAsync("uploads", generateFileId(), file)
            .thenApply(etag -> {
                logger.info("File uploaded successfully using async S3Service");
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "File uploaded successfully");
                response.put("fileName", fileName);
                response.put("contentType", file.getContentType());
                response.put("size", file.getSize());
                response.put("etag", etag);

                logger.info("Successfully uploaded file: {} (size: {} bytes)", fileName, file.getSize());
                return response;
            })
            .exceptionally(e -> {
                logger.error("Failed to upload file: {}", fileName, e);
                Exception exception = (e instanceof Exception) ? (Exception) e : new RuntimeException(e);
                return createErrorResponse(
                        "upload file to S3",
                        exception,
                        "fileName: " + fileName);
            });
    }

    /**
     * Business logic method to handle complete file listing process  
     */
    public CompletableFuture<Map<String, Object>> processFileList() {
        logger.info("Starting async file listing using S3Service");
        
        return listFilesAsync()
            .thenApply(files -> {
                logger.info("Files listed successfully using async S3Service: {} files", files.size());
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("files", files);
                response.put("count", files.size());
                return response;
            })
            .exceptionally(e -> {
                logger.error("Failed to list files", e);
                Exception exception = (e instanceof Exception) ? (Exception) e : new RuntimeException(e);
                return createErrorResponse(
                        "list files from S3",
                        exception,
                        "Attempting to retrieve S3 file list");
            });
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

    /**
     * Creates a detailed error response with stack trace and context information
     */
    public Map<String, Object> createErrorResponse(String operation, Exception e, String context) {
        logger.error("Error during {}: {}", operation, e.getMessage(), e);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("operation", operation);
        errorResponse.put("exceptionType", e.getClass().getSimpleName());
        errorResponse.put("message", e.getMessage());
        errorResponse.put("context", context);

        // Add stack trace for detailed debugging
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        errorResponse.put("stackTrace", sw.toString());

        // Add cause information if available
        if (e.getCause() != null) {
            errorResponse.put("causeType", e.getCause().getClass().getSimpleName());
            errorResponse.put("causeMessage", e.getCause().getMessage());
        }

        return errorResponse;
    }

    /**
     * Generates a unique file ID for organizing uploads
     */
    public String generateFileId() {
        return UUID.randomUUID().toString();
    }
}
