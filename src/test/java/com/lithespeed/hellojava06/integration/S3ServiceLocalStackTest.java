package com.lithespeed.hellojava06.integration;

import com.lithespeed.hellojava06.config.LocalStackS3Config;
import com.lithespeed.hellojava06.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for S3Service using LocalStack.
 * These tests run the actual S3Service implementation against
 * a real LocalStack container, providing true integration testing
 * without the complexity of WireMock HTTP mocking.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("localstack")
@Import(LocalStackS3Config.class)
@DisplayName("S3Service Integration Tests with LocalStack")
class S3ServiceLocalStackTest {

    private static final String BUCKET_NAME = "test-bucket";
    
    @Container
    static LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.0"))
            .withServices(LocalStackContainer.Service.S3);

    @Autowired
    private S3Service s3Service;
    
    @Autowired
    private S3Client s3Client;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        LocalStackS3Config.configureProperties(registry, localStack);
    }

    @BeforeEach
    void setUp() {
        // Create the test bucket for each test
        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
        } catch (Exception e) {
            // Bucket may already exist, verify it's there
            s3Client.headBucket(HeadBucketRequest.builder().bucket(BUCKET_NAME).build());
        }
    }

    @Test
    @DisplayName("Should upload file to S3 bucket")
    void shouldUploadFileToS3Bucket() {
        // Given
        String fileName = "test-upload.txt";

        // When
        s3Service.uploadFile(fileName);

        // Then - Verify file was actually uploaded
        List<String> files = s3Service.listFiles();
        assertThat(files).contains(fileName);
        
        ListObjectsV2Response objects = s3Client.listObjectsV2(
                ListObjectsV2Request.builder().bucket(BUCKET_NAME).build());
        assertThat(objects.contents())
                .hasSize(1)
                .extracting(obj -> obj.key())
                .contains(fileName);
    }

    @Test
    @DisplayName("Should upload file with content to S3 bucket")
    void shouldUploadFileWithContentToS3Bucket() {
        // Given
        String fileName = "test-content-upload.txt";
        String fileContent = "Hello LocalStack S3!";

        // When
        String etag = s3Service.uploadFileWithContent(fileName, fileContent);

        // Then
        assertThat(etag).isNotNull().isNotEmpty();
        List<String> files = s3Service.listFiles();
        assertThat(files).contains(fileName);
    }

    @Test
    @DisplayName("Should download file from S3 bucket")
    void shouldDownloadFileFromS3Bucket() {
        // Given - upload a file first
        String fileName = "test-download.txt";
        String expectedContent = "Content for downloading from LocalStack!";
        s3Service.uploadFileWithContent(fileName, expectedContent);

        // When
        String actualContent = s3Service.downloadFile(fileName);

        // Then
        assertThat(actualContent).isEqualTo(expectedContent);
    }

    @Test
    @DisplayName("Should list all files in S3 bucket")
    void shouldListAllFilesInS3Bucket() {
        // Given - upload multiple files
        s3Service.uploadFileWithContent("file1.txt", "Content 1");
        s3Service.uploadFileWithContent("file2.txt", "Content 2");
        s3Service.uploadFileWithContent("file3.txt", "Content 3");

        // When
        List<String> fileNames = s3Service.listFiles();

        // Then
        assertThat(fileNames)
                .hasSize(3)
                .containsExactlyInAnyOrder("file1.txt", "file2.txt", "file3.txt");
    }

    @Test
    @DisplayName("Should delete file from S3 bucket")
    void shouldDeleteFileFromS3Bucket() {
        // Given - upload a file first
        String fileName = "file-to-delete.txt";
        s3Service.uploadFileWithContent(fileName, "This file will be deleted");
        
        // Verify file exists
        List<String> filesBeforeDelete = s3Service.listFiles();
        assertThat(filesBeforeDelete).contains(fileName);

        // When
        boolean deleted = s3Service.deleteFile(fileName);

        // Then
        assertTrue(deleted, "File deletion should return true");
        List<String> filesAfterDelete = s3Service.listFiles();
        assertThat(filesAfterDelete).doesNotContain(fileName);
    }

    @Test
    @DisplayName("Should handle round-trip upload and download")
    void shouldHandleRoundTripUploadAndDownload() {
        // Given
        String fileName = "round-trip-file.txt";
        String originalContent = "This content will be uploaded and downloaded";

        // When
        String etag = s3Service.uploadFileWithContent(fileName, originalContent);
        String downloadedContent = s3Service.downloadFile(fileName);

        // Then
        assertThat(etag).isNotNull();
        assertThat(downloadedContent).isEqualTo(originalContent);
        
        List<String> files = s3Service.listFiles();
        assertThat(files).contains(fileName);
    }

    /**
     * Helper method to upload a test file - not needed anymore since we use S3Service methods
     */
}
