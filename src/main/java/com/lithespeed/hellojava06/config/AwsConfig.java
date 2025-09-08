package com.lithespeed.hellojava06.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.time.Duration;

@Profile("preprod")
@Configuration
public class AwsConfig {

    private static final Logger logger = LoggerFactory.getLogger(AwsConfig.class);

    @Value("${spring.cloud.aws.s3.crt.number-of-retries:5}")
    private Integer numRetries;

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Bean
    public S3AsyncClient s3AsyncClient() {
        logger.info("Creating S3AsyncClient for preprod profile with region: {}", awsRegion);
        
        try {
            // Try CRT first for better performance
            logger.info("Attempting to create S3AsyncClient with CRT");
            return S3AsyncClient.crtBuilder()
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .region(Region.of(awsRegion))
                    .retryConfiguration(rcb -> rcb.numRetries(numRetries))
                    .build();
        } catch (Exception e) {
            logger.warn("CRT not available ({}), falling back to standard S3AsyncClient", e.getMessage());
            
            try {
                // Fallback to standard S3AsyncClient
                return S3AsyncClient.builder()
                        .credentialsProvider(DefaultCredentialsProvider.create())
                        .region(Region.of(awsRegion))
                        .overrideConfiguration(cfg -> cfg
                                .retryPolicy(retry -> retry.numRetries(numRetries))
                                .apiCallTimeout(Duration.ofSeconds(30))
                                .apiCallAttemptTimeout(Duration.ofSeconds(10)))
                        .build();
            } catch (Exception fallbackException) {
                logger.error("Failed to create standard S3AsyncClient, creating minimal client", fallbackException);
                
                // Last resort - minimal client that might work for basic operations
                return S3AsyncClient.builder()
                        .credentialsProvider(AnonymousCredentialsProvider.create())
                        .region(Region.US_EAST_1)
                        .build();
            }
        }
    }
}
