package com.lithespeed.hellojava06.config;

import com.lithespeed.hellojava06.service.S3Service;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * Test configuration for S3 service with LocalStack.
 * Creates S3Service with a real S3Client pointing to LocalStack
 * instead of real AWS, allowing testing of production code
 * against real S3 behavior in a containerized environment.
 */
@TestConfiguration
@Profile("localstack")
public class LocalStackS3Config {

    /**
     * Creates an S3Client configured to point to LocalStack container
     * instead of real AWS S3.
     */
    @Bean("localStackS3Client")
    @Primary
    public S3Client localStackS3Client() {
        // These will be dynamically configured by the test
        String endpoint = System.getProperty("aws.s3.endpoint", "http://localhost:4566");
        String region = System.getProperty("aws.s3.region", "us-east-1");
        String accessKey = System.getProperty("aws.accessKeyId", "test");
        String secretKey = System.getProperty("aws.secretAccessKey", "test");
        
        System.out.println("Creating LocalStack S3Client:");
        System.out.println("  Endpoint: " + endpoint);
        System.out.println("  Region: " + region);
        
        return S3Client.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .forcePathStyle(true) // Required for LocalStack
                .build();
    }

    /**
     * Creates S3Service using the LocalStack S3Client
     */
    @Bean("localStackS3Service")
    @Primary
    public S3Service localStackS3Service() {
        String bucketName = System.getProperty("aws.s3.bucket-name", "test-bucket");
        System.out.println("Creating LocalStack S3Service with bucket: " + bucketName);
        return new S3Service(localStackS3Client(), bucketName);
    }
    
    /**
     * Configure dynamic properties for LocalStack tests.
     * This method will be called by individual test classes.
     */
    public static void configureProperties(DynamicPropertyRegistry registry, LocalStackContainer localStack) {
        registry.add("aws.s3.endpoint", () -> localStack.getEndpointOverride(LocalStackContainer.Service.S3).toString());
        registry.add("aws.s3.region", localStack::getRegion);
        registry.add("aws.accessKeyId", localStack::getAccessKey);
        registry.add("aws.secretAccessKey", localStack::getSecretKey);
        registry.add("aws.s3.bucket-name", () -> "test-bucket");
    }
}
