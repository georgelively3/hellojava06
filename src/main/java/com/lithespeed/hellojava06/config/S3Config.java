package com.lithespeed.hellojava06.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.net.URI;

@Configuration
public class S3Config {

    private static final Logger logger = LoggerFactory.getLogger(S3Config.class);

    @Value("${aws.s3.region:us-east-1}")
    private String awsRegion;

    @Value("${aws.s3.endpoint:}")
    private String s3Endpoint;

    /**
     * Default S3AsyncClient for default profile (containers, non-preprod, non-test environments)
     * Automatically detects local development and uses mock credentials to prevent crashes
     */
    @Bean
    @Profile("default")
    public S3AsyncClient s3AsyncClient() {
        logger.info("Creating S3AsyncClient for default profile");
        
        // Check if we're in local development environment
        if (isLocalDevelopment()) {
            logger.info("Local development environment detected, creating mock S3AsyncClient");
            return createLocalDevelopmentClient();
        }
        
        // Real AWS environment - try to create proper client
        logger.info("AWS environment detected, creating real S3AsyncClient");
        return createAwsClient();
    }
    
    private boolean isLocalDevelopment() {
        // If we're running tests, don't use local development mode
        if (isRunningTests()) {
            return false;
        }
        
        // Check for AWS environment indicators
        String awsRegion = System.getenv("AWS_REGION");
        String awsProfile = System.getenv("AWS_PROFILE");
        String awsAccessKey = System.getenv("AWS_ACCESS_KEY_ID");
        String awsRole = System.getenv("AWS_ROLE_ARN");
        
        // If none of the typical AWS environment variables are set, assume local development
        boolean hasAwsEnv = (awsRegion != null && !awsRegion.isEmpty()) ||
                           (awsProfile != null && !awsProfile.isEmpty()) ||
                           (awsAccessKey != null && !awsAccessKey.isEmpty()) ||
                           (awsRole != null && !awsRole.isEmpty());
        
        if (!hasAwsEnv) {
            logger.info("No AWS environment variables detected - assuming local development");
            return true;
        }
        
        return false;
    }
    
    private boolean isRunningTests() {
        // Check if we're running in test mode
        try {
            Class.forName("org.junit.jupiter.api.Test");
            // Check for test-specific system properties
            String testProperty = System.getProperty("java.class.path");
            return testProperty != null && testProperty.contains("test");
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private S3AsyncClient createLocalDevelopmentClient() {
        try {
            // Use configured region or default to US_EAST_1 for local development
            Region region;
            try {
                region = Region.of(awsRegion);
            } catch (Exception e) {
                logger.warn("Invalid region '{}', using us-east-1 for local development", awsRegion);
                region = Region.US_EAST_1;
            }
            
            var builder = S3AsyncClient.builder()
                    .credentialsProvider(AnonymousCredentialsProvider.create())
                    .region(region);
            
            // If custom endpoint provided (LocalStack, etc.)
            if (s3Endpoint != null && !s3Endpoint.trim().isEmpty()) {
                logger.info("Using custom S3 endpoint for local development: {}", s3Endpoint);
                builder.endpointOverride(URI.create(s3Endpoint));
            }
            
            S3AsyncClient client = builder.build();
            logger.info("Mock S3AsyncClient created successfully for local development with region: {}", region.id());
            return client;
            
        } catch (Exception e) {
            logger.error("Failed to create local development S3AsyncClient, using minimal fallback", e);
            return S3AsyncClient.builder()
                    .credentialsProvider(AnonymousCredentialsProvider.create())
                    .region(Region.US_EAST_1)
                    .build();
        }
    }
    
    private S3AsyncClient createAwsClient() {
        try {
            var builder = S3AsyncClient.builder()
                    .region(Region.of(awsRegion));

            // If custom endpoint is provided
            if (s3Endpoint != null && !s3Endpoint.trim().isEmpty()) {
                logger.info("Using custom S3 endpoint: {}", s3Endpoint);
                builder.endpointOverride(URI.create(s3Endpoint));
                builder.credentialsProvider(AnonymousCredentialsProvider.create());
            } else {
                // Try to use default credentials
                try {
                    DefaultCredentialsProvider credProvider = DefaultCredentialsProvider.create();
                    credProvider.resolveCredentials(); // Test if credentials work
                    builder.credentialsProvider(credProvider);
                    logger.info("Using AWS Default Credentials Provider");
                } catch (Exception e) {
                    logger.warn("AWS Default Credentials not available, falling back to anonymous: {}", e.getMessage());
                    builder.credentialsProvider(AnonymousCredentialsProvider.create());
                }
            }

            S3AsyncClient client = builder.build();
            logger.info("S3AsyncClient initialized successfully for region: {}", awsRegion);
            return client;

        } catch (Exception e) {
            logger.error("Failed to create AWS S3AsyncClient, using fallback", e);
            return S3AsyncClient.builder()
                    .region(Region.US_EAST_1)
                    .credentialsProvider(AnonymousCredentialsProvider.create())
                    .build();
        }
    }
}