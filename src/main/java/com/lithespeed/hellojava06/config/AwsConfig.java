package com.lithespeed.hellojava06.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@Configuration
@Profile("!test")
public class AwsConfig {

    @Value("${aws.s3.retry-count:3}")
    private int numRetries;

    @Bean
    public S3AsyncClient s3AsyncClient() {
        return S3AsyncClient.crtBuilder()
                .retryConfiguration(rcb -> rcb.numRetries(numRetries))
                .region(software.amazon.awssdk.regions.Region.US_EAST_1) // Default region for template
                .build();
    }
}
