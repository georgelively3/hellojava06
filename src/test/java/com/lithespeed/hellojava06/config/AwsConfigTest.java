package com.lithespeed.hellojava06.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import static org.junit.jupiter.api.Assertions.*;

class AwsConfigTest {

    private AwsConfig awsConfig;

    @BeforeEach
    void setUp() {
        awsConfig = new AwsConfig();
    }

    @Test
    void s3AsyncClient_ShouldCreateClient_WhenCalledWithValidConfiguration() {
        // Arrange
        ReflectionTestUtils.setField(awsConfig, "numRetries", 3);

        // Act & Assert
        assertDoesNotThrow(() -> {
            S3AsyncClient client = awsConfig.s3AsyncClient();
            assertNotNull(client);
        });
    }

    @Test
    void s3AsyncClient_ShouldCreateClient_WithDifferentRetryValues() {
        // Arrange
        ReflectionTestUtils.setField(awsConfig, "numRetries", 5);

        // Act & Assert
        assertDoesNotThrow(() -> {
            S3AsyncClient client = awsConfig.s3AsyncClient();
            assertNotNull(client);
        });
    }

    @Test
    void s3AsyncClient_ShouldCreateDifferentInstances_WhenCalledMultipleTimes() {
        // Arrange
        ReflectionTestUtils.setField(awsConfig, "numRetries", 3);

        // Act
        S3AsyncClient client1 = awsConfig.s3AsyncClient();
        S3AsyncClient client2 = awsConfig.s3AsyncClient();

        // Assert
        assertNotNull(client1);
        assertNotNull(client2);
        assertNotSame(client1, client2); // Should be different instances since @Bean creates new instances
    }

    @Test
    void s3AsyncClient_ShouldHandleZeroRetries_WhenRetriesSetToZero() {
        // Arrange
        ReflectionTestUtils.setField(awsConfig, "numRetries", 0);

        // Act & Assert
        assertDoesNotThrow(() -> {
            S3AsyncClient client = awsConfig.s3AsyncClient();
            assertNotNull(client);
        });
    }
}
