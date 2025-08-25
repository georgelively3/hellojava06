package com.lithespeed.hellojava06.integration;

import com.lithespeed.hellojava06.extension.S3WireMockExtension;
import com.lithespeed.hellojava06.service.S3Service;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Simple focused test to verify S3Service + WireMock integration
 * using the S3WireMockExtension for consistent server management
 */
@ExtendWith(S3WireMockExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "logging.level.com.lithespeed=DEBUG",
        "logging.level.software.amazon.awssdk=DEBUG"
})
class S3ServiceWireMockDirectTest {

    private S3Service s3Service;
    private S3Client s3Client;

    @BeforeEach
    void setUp() {
        // Create S3Client pointing to WireMock (managed by extension)
        s3Client = S3Client.builder()
                .region(Region.US_EAST_1)
                .endpointOverride(URI.create("http://localhost:" + S3WireMockExtension.getWireMockPort()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test-key", "test-secret")))
                .forcePathStyle(true)
                .serviceConfiguration(S3Configuration.builder()
                        .checksumValidationEnabled(false)
                        .build())
                .build();

        // Create S3Service with WireMock client
        s3Service = new S3Service(s3Client, "test-bucket");
    }

    @AfterEach
    void tearDown() {
        if (s3Client != null) {
            s3Client.close();
        }
    }

    @Test
    @DisplayName("Direct S3Service should list files with WireMock")
    void testListFiles() throws Exception {
        // Test list files
        var files = s3Service.listFiles();

        // Verify WireMock received the request
        verify(getRequestedFor(urlMatching("/test-bucket.*")));

        System.out.println("List files test passed");
        Assertions.assertFalse(files.isEmpty(), "Should return list with test files");
        Assertions.assertTrue(files.contains("list-test-file.txt"), "Should contain list-test-file.txt");
        Assertions.assertTrue(files.contains("test-file.txt"), "Should contain test-file.txt");
    }

    @Test
    @DisplayName("Direct S3Service should upload file with WireMock")
    void testUploadFile() throws Exception {
        try {
            // Test upload file
            s3Service.uploadFile("test-file.txt");

            // Log upload completion
            System.out.println("Upload operation completed successfully");

            // Verify WireMock received the upload request (path-style URL)
            verify(putRequestedFor(urlEqualTo("/test-bucket/test-file.txt")));

            System.out.println("Upload file test passed");
        } catch (Exception e) {
            System.err
                    .println("Upload file test failed with: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    @DisplayName("Direct S3Service should upload file with content to WireMock")
    void testUploadFileWithContent() throws Exception {
        try {
            // Test upload file with content
            s3Service.uploadFileWithContent("test-content.txt", "Hello WireMock!");

            // Verify WireMock received the upload request (path-style URL)
            verify(putRequestedFor(urlEqualTo("/test-bucket/test-content.txt")));

            System.out.println("Upload file with content test passed");
        } catch (Exception e) {
            System.err.println("Upload file with content test failed with: " + e.getClass().getSimpleName() + " - "
                    + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
