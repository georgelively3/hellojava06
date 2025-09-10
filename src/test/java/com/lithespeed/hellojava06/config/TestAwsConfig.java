package com.lithespeed.hellojava06.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@TestConfiguration
@Profile("test")
public class TestAwsConfig {

    @Bean
    @Primary
    public S3AsyncClient s3AsyncClient() {
        return Mockito.mock(S3AsyncClient.class);
    }
}
