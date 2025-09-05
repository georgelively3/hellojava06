package com.lithespeed.hellojava06.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@Profile("preprod")
@Configuration
public class AwsConfig {

    @Value("${spring.cloud.aws.s3.crt.number-of-retries:5}")
    private Integer numRetries;

    @Bean
    public S3AsyncClient s3AsyncClient() {
        return S3AsyncClient.crtBuilder()
                .retryConfiguration(rcb -> rcb.numRetries(numRetries))
                .build();
    }
}
