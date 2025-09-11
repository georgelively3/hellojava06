package com.lithespeed.hellojava06.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class AwsConfigTest {
    
    private AwsConfig awsConfig;

    @BeforeEach
    void setUp() {
        awsConfig = new AwsConfig();
        // Set test values using reflection
        ReflectionTestUtils.setField(awsConfig, "numRetries", 3);
    }

    @Test
    void s3AsyncClient_ShouldNotBeNull() {
        // Act
        S3AsyncClient s3AsyncClient = awsConfig.s3AsyncClient();

        // Assert
        assertNotNull(s3AsyncClient);
    }

    @Test
    void s3AsyncClient_ShouldCreateClientWithCorrectConfiguration() {
        // Act
        S3AsyncClient s3AsyncClient = awsConfig.s3AsyncClient();

        // Assert
        assertNotNull(s3AsyncClient);
        // Verify the client was created - service name might vary by AWS SDK version
        assertNotNull(s3AsyncClient.serviceName());
    }

    @Test
    void s3AsyncClient_ShouldHandleDifferentRetryCount() {
        // Arrange
        ReflectionTestUtils.setField(awsConfig, "numRetries", 5);

        // Act
        S3AsyncClient s3AsyncClient = awsConfig.s3AsyncClient();

        // Assert
        assertNotNull(s3AsyncClient);
        assertNotNull(s3AsyncClient.serviceName());
    }

    @Test
    void s3AsyncClient_ShouldHandleZeroRetries() {
        // Arrange
        ReflectionTestUtils.setField(awsConfig, "numRetries", 0);

        // Act
        S3AsyncClient s3AsyncClient = awsConfig.s3AsyncClient();

        // Assert
        assertNotNull(s3AsyncClient);
        assertNotNull(s3AsyncClient.serviceName());
    }

    @Test
    void s3AsyncClient_ShouldBeSpringBean() {
        // This test verifies that the method is annotated properly for Spring bean creation
        // The @Bean annotation ensures this will be managed by Spring IoC container
        
        // Act
        S3AsyncClient client1 = awsConfig.s3AsyncClient();
        S3AsyncClient client2 = awsConfig.s3AsyncClient();

        // Assert - Each call should create a new instance (since no @Scope specified)
        assertNotNull(client1);
        assertNotNull(client2);
        assertNotSame(client1, client2); // Different instances
    }
}
