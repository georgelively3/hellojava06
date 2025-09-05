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
     * Uses default credential chain with fallback to anonymous for local development
     */
    @Bean
    @Profile("default")
    public S3AsyncClient s3AsyncClient() {
        try {
            var builder = S3AsyncClient.builder()
                    .region(Region.of(awsRegion));

            // If custom endpoint is provided (for LocalStack, etc.)
            if (s3Endpoint != null && !s3Endpoint.trim().isEmpty()) {
                logger.info("Using custom S3 endpoint: {}", s3Endpoint);
                builder.endpointOverride(URI.create(s3Endpoint));
                builder.credentialsProvider(AnonymousCredentialsProvider.create());
            } else {
                // Try to use default credentials, with fallback to anonymous
                try {
                    DefaultCredentialsProvider.create().resolveCredentials();
                    builder.credentialsProvider(DefaultCredentialsProvider.create());
                    logger.info("Using AWS Default Credentials Provider");
                } catch (SdkClientException e) {
                    logger.warn("AWS Default Credentials not available, using anonymous credentials for local development: {}", e.getMessage());
                    builder.credentialsProvider(AnonymousCredentialsProvider.create());
                }
            }

            S3AsyncClient client = builder.build();
            logger.info("S3AsyncClient initialized successfully for region: {}", awsRegion);
            return client;

        } catch (Exception e) {
            logger.error("Failed to create S3AsyncClient", e);
            // Create a minimal client as last resort
            return S3AsyncClient.builder()
                    .region(Region.of(awsRegion))
                    .credentialsProvider(AnonymousCredentialsProvider.create())
                    .build();
        }
    }
}