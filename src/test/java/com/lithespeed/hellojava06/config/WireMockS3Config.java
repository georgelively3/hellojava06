package com.lithespeed.hellojava06.config;

import com.lithespeed.hellojava06.service.S3Service;
import org.springframework.beans.factory.annotation.Qualifier;
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
 * Test configuration for S3 service with WireMock.
 * Creates S3Service with a mocked S3Client pointing to WireMock
 * instead of real AWS, allowing testing of production code
 * against mocked S3 responses.
 */
@TestConfiguration
@Profile("wiremock")
public class WireMockS3Config {

    /**
     * Creates an S3Client configured to point to WireMock server
     * instead of real AWS S3.
     */
    @Bean("testS3Client")
    @Primary
    public S3Client wireMockS3Client() {
        return S3Client.builder()
                .region(Region.US_EAST_1)
                .endpointOverride(URI.create("http://localhost:8089"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test-access-key", "test-secret-key")))
                .forcePathStyle(true) // Required for S3-compatible services like WireMock
                .build();
    }

    /**
     * Creates S3Service using the WireMock S3Client
     */
    @Bean("testS3Service")
    @Primary
    public S3Service wireMockS3Service(@Qualifier("testS3Client") S3Client wireMockS3Client) {
        return new S3Service(wireMockS3Client, "test-bucket");
    }
}
