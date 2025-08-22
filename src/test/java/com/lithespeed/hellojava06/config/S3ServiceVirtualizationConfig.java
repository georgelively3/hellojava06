package com.lithespeed.hellojava06.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * Test configuration for S3 service virtualization.
 * When running integration tests with service virtualization,
 * this configuration provides an S3Client that points to WireMock
 * instead of real AWS, allowing us to test the actual AwsS3Service
 * implementation against mocked responses.
 */
@TestConfiguration
@Profile("service-virtualization")
public class S3ServiceVirtualizationConfig {

    /**
     * Creates an S3Client configured to point to WireMock server
     * instead of real AWS S3. This allows us to test the real
     * AwsS3Service implementation against mocked S3 responses.
     */
    @Bean
    @Primary
    public S3Client mockS3Client() {
        return S3Client.builder()
                .region(Region.US_EAST_1)
                .endpointOverride(URI.create("http://localhost:9999"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test-access-key", "test-secret-key")))
                .forcePathStyle(true) // Required for S3-compatible services like WireMock
                .build();
    }
}
