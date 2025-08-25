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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic LocalStack integration test to verify the setup works.
 * This is a simpler test that just verifies LocalStack is running
 * and we can connect to it properly.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("localstack")
@Import(LocalStackS3Config.class)
@DisplayName("LocalStack Basic Integration Test")
class LocalStackBasicTest {

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
    @DisplayName("LocalStack container should be running")
    void localStackContainerShouldBeRunning() {
        assertTrue(localStack.isRunning(), "LocalStack container should be running");
        assertNotNull(localStack.getEndpointOverride(LocalStackContainer.Service.S3), 
                "S3 endpoint should be available");
    }

    @Test
    @DisplayName("S3Service should be autowired correctly")
    void s3ServiceShouldBeAutowiredCorrectly() {
        assertNotNull(s3Service, "S3Service should be autowired");
    }

    @Test
    @DisplayName("S3Client should be autowired correctly")
    void s3ClientShouldBeAutowiredCorrectly() {
        assertNotNull(s3Client, "S3Client should be autowired");
    }

    @Test
    @DisplayName("Should be able to upload a simple file")
    void shouldBeAbleToUploadSimpleFile() {
        // Given
        String fileName = "basic-test-file.txt";

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> s3Service.uploadFile(fileName), 
                "Should be able to upload file to LocalStack");
    }

    @Test
    @DisplayName("Should be able to list files")
    void shouldBeAbleToListFiles() {
        // Given
        s3Service.uploadFile("test-file-1.txt");
        s3Service.uploadFile("test-file-2.txt");

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> {
            var files = s3Service.listFiles();
            assertEquals(2, files.size(), "Should have 2 files");
        }, "Should be able to list files from LocalStack");
    }
}
