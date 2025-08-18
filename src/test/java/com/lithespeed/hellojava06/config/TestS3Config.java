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
 * Test configuration for S3Client to resolve dependency injection issues in
 * unit tests.
 * This provides a minimal S3Client configuration that doesn't require external
 * dependencies.
 */
@TestConfiguration
public class TestS3Config {

    /**
     * Provides a minimal S3Client for unit tests.
     * This bean has higher priority than the main S3Config beans to avoid
     * dependency issues.
     */
    @Bean
    @Primary
    @Profile("test")
    public S3Client testS3Client() {
        // Minimal configuration for unit tests - uses dummy credentials
        AwsBasicCredentials credentials = AwsBasicCredentials.create("test", "test");

        return S3Client.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .endpointOverride(URI.create("http://localhost:4566")) // LocalStack default
                .build();
    }
}
