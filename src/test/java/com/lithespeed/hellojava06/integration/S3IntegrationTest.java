package com.lithespeed.hellojava06.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.lithespeed.hellojava06.config.MountebankS3Config;
import com.lithespeed.hellojava06.service.S3Service;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for S3Service with WireMock service virtualization
 * This replaces LocalStack with lighter-weight HTTP mocks
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("mountebank")
class S3IntegrationTest {

    @Autowired
    private S3Service s3Service;

    @Autowired
    private MountebankS3Config mountebankConfig;

    @Autowired
    private WireMockServer wireMockServer;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Properties are configured in application-mountebank.yml
        // This method is here for consistency with other integration tests
    }

    @Test
    void testUploadFile() throws Exception {
        assertTrue(wireMockServer.isRunning(), "WireMock server should be running");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-file.txt",
                "text/plain",
                "Hello, World!".getBytes());

        String fileName = "test-upload.txt";

        // Upload file - WireMock will return success response
        String result = s3Service.uploadFile(fileName, file);

        assertNotNull(result);
        assertTrue(result.contains(mountebankConfig.getBucketName()));
        assertTrue(result.contains(fileName));
    }

    @Test
    void testListFiles() {
        assertTrue(wireMockServer.isRunning(), "WireMock server should be running");

        // List files - WireMock will return mock file list
        List<String> files = s3Service.listFiles("test/");

        assertNotNull(files);
        // WireMock mock returns one test file
        assertFalse(files.isEmpty());
    }

    @Test
    void testBasicWireMockSetup() {
        // Simple test to verify WireMock configuration is working
        assertNotNull(mountebankConfig);
        assertNotNull(mountebankConfig.getBucketName());
        assertEquals("test-bucket", mountebankConfig.getBucketName());
    }
}
