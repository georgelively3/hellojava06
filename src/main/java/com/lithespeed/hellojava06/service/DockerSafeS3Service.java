package com.lithespeed.hellojava06.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DockerSafeS3Service {

    private static final Logger logger = LoggerFactory.getLogger(DockerSafeS3Service.class);

    @Value("${aws.s3.bucket-name:test-bucket}")
    private String bucketName;

    @Value("${aws.s3.region:us-east-1}")
    private String region;

    @Value("${aws.s3.enabled:false}")
    private boolean s3Enabled;

    private final S3Client s3Client;

    public DockerSafeS3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String uploadFile(String key, MultipartFile file) throws IOException {
        logger.info("Upload file request: key={}, enabled={}", key, s3Enabled);

        if (!s3Enabled) {
            logger.warn("S3 is disabled - returning mock URL");
            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
        }

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
            logger.info("File uploaded successfully: {}", url);
            return url;

        } catch (Exception e) {
            logger.error("Failed to upload file to S3: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage(), e);
        }
    }

    public List<String> listFiles(String prefix) {
        logger.info("List files request: prefix={}, enabled={}", prefix, s3Enabled);

        if (!s3Enabled) {
            logger.warn("S3 is disabled - returning mock file list");
            // Return mock data when S3 is disabled
            List<String> mockFiles = new ArrayList<>();
            mockFiles.add("mock-file-1.txt");
            mockFiles.add("mock-file-2.txt");
            if (prefix != null && !prefix.isEmpty()) {
                mockFiles.add(prefix + "/mock-file-3.txt");
            }
            return mockFiles;
        }

        try {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucketName);

            if (prefix != null && !prefix.isEmpty()) {
                requestBuilder.prefix(prefix);
            }

            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());

            List<String> files = response.contents().stream()
                    .map(S3Object::key)
                    .collect(Collectors.toList());

            logger.info("Listed {} files from S3", files.size());
            return files;

        } catch (Exception e) {
            logger.error("Failed to list files from S3: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to list files from S3: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String key) {
        logger.info("Delete file request: key={}, enabled={}", key, s3Enabled);

        if (!s3Enabled) {
            logger.warn("S3 is disabled - simulating file deletion");
            return;
        }

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            logger.info("File deleted successfully: {}", key);

        } catch (Exception e) {
            logger.error("Failed to delete file from S3: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete file from S3: " + e.getMessage(), e);
        }
    }

    public boolean isHealthy() {
        if (!s3Enabled) {
            logger.info("S3 is disabled - health check returns false");
            return false;
        }

        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

            s3Client.headBucket(headBucketRequest);
            logger.info("S3 health check passed");
            return true;

        } catch (Exception e) {
            logger.warn("S3 health check failed: {}", e.getMessage());
            return false;
        }
    }
}
