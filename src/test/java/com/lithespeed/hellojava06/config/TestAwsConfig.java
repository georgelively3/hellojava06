package com.lithespeed.hellojava06.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import static org.mockito.Mockito.mock;

@Profile("!preprod")
@Configuration
public class TestAwsConfig {

    @Bean
    @Primary
    public S3AsyncClient s3AsyncClient() {
        // Return a mock S3AsyncClient for tests
        return mock(S3AsyncClient.class);
    }
}
