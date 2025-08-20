package com.lithespeed.hellojava06.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * Test configuration for S3Client - provides a minimal working S3Client for
 * tests
 * without external dependencies
 */
@TestConfiguration
public class TestS3Config {

    /**
     * Provides a minimal S3Client for integration tests.
     * This uses dummy credentials and endpoint but won't actually connect to S3.
     */
    @Bean
    @Primary
    @Profile("test")
    public S3Client testS3Client() {
        return S3Client.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test-access-key", "test-secret-key")))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .endpointOverride(URI.create("http://localhost:9999")) // Non-existent endpoint - will cause controlled
                                                                       // failure
                .build();
    }
}
