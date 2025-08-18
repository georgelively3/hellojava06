package com.lithespeed.hellojava06.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;

/**
 * Minimal test configuration that provides mocked beans to avoid dependency
 * injection issues
 * in unit tests that don't need the full application context.
 */
@TestConfiguration
public class UnitTestConfig {

    /**
     * Provides a mocked S3Service for unit tests that don't need real S3
     * functionality
     */
    @Bean
    @Primary
    @Profile("unit-test")
    public com.lithespeed.hellojava06.service.S3Service mockS3Service() {
        return mock(com.lithespeed.hellojava06.service.S3Service.class);
    }
}
