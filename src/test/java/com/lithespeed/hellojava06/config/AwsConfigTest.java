package com.lithespeed.hellojava06.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AwsConfigTest {

    private AwsConfig awsConfig;

    @BeforeEach
    void setUp() {
        awsConfig = new AwsConfig();
        // Set default values for required fields
        ReflectionTestUtils.setField(awsConfig, "numRetries", 5);
        ReflectionTestUtils.setField(awsConfig, "awsRegion", "us-east-1");
    }

    @Test
    void s3AsyncClient_ShouldCreateClient_WhenCalledWithDefaultSettings() {
        // Act
        S3AsyncClient client = awsConfig.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should not be null");
        assertEquals("s3", client.serviceName(), "Service name should be s3");
        
        // Clean up
        client.close();
    }

    @Test
    void s3AsyncClient_ShouldCreateClientWithCustomRegion_WhenRegionPropertySet() {
        // Arrange
        ReflectionTestUtils.setField(awsConfig, "awsRegion", "us-west-2");

        // Act
        S3AsyncClient client = awsConfig.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should not be null with custom region");
        assertEquals("us-west-2", client.serviceClientConfiguration().region().id(), 
                     "Client should be configured with custom region");
        
        // Clean up
        client.close();
    }

    @Test
    void s3AsyncClient_ShouldCreateClientWithCustomRetries_WhenRetriesPropertySet() {
        // Arrange
        ReflectionTestUtils.setField(awsConfig, "numRetries", 3);

        // Act
        S3AsyncClient client = awsConfig.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should not be null with custom retries");
        assertEquals("s3", client.serviceName(), "Service name should be s3");
        
        // Clean up
        client.close();
    }

    @Test
    void s3AsyncClient_ShouldHandleInvalidRegion_WhenBadRegionProvided() {
        // Arrange
        ReflectionTestUtils.setField(awsConfig, "awsRegion", "invalid-region");

        // Act & Assert
        // Should not throw exception, should handle gracefully
        assertDoesNotThrow(() -> {
            S3AsyncClient client = awsConfig.s3AsyncClient();
            assertNotNull(client, "S3AsyncClient should not be null even with invalid region");
            client.close();
        });
    }

    @Test
    void s3AsyncClient_ShouldHandleNullRegion_WhenRegionIsNull() {
        // Arrange
        ReflectionTestUtils.setField(awsConfig, "awsRegion", null);

        // Act
        S3AsyncClient client = awsConfig.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should not be null when region is null");
        assertEquals("s3", client.serviceName(), "Service name should be s3");
        
        // Clean up
        client.close();
    }

    @Test
    void s3AsyncClient_ShouldHandleEmptyRegion_WhenRegionIsEmpty() {
        // Arrange
        ReflectionTestUtils.setField(awsConfig, "awsRegion", "");

        // Act
        S3AsyncClient client = awsConfig.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should not be null when region is empty");
        assertEquals("s3", client.serviceName(), "Service name should be s3");
        
        // Clean up
        client.close();
    }

    @Test
    void s3AsyncClient_ShouldCreateMultipleInstances_WhenCalledMultipleTimes() {
        // Act
        S3AsyncClient client1 = awsConfig.s3AsyncClient();
        S3AsyncClient client2 = awsConfig.s3AsyncClient();

        // Assert
        assertNotNull(client1, "First S3AsyncClient should not be null");
        assertNotNull(client2, "Second S3AsyncClient should not be null");
        assertNotSame(client1, client2, "Multiple calls should create different instances");
        assertEquals("s3", client1.serviceName(), "First client service name should be s3");
        assertEquals("s3", client2.serviceName(), "Second client service name should be s3");
        
        // Clean up
        client1.close();
        client2.close();
    }

    @Test
    void s3AsyncClient_ShouldHandleZeroRetries_WhenRetriesSetToZero() {
        // Arrange
        ReflectionTestUtils.setField(awsConfig, "numRetries", 0);

        // Act
        S3AsyncClient client = awsConfig.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should not be null with zero retries");
        assertEquals("s3", client.serviceName(), "Service name should be s3");
        
        // Clean up
        client.close();
    }

    @Test
    void s3AsyncClient_ShouldHandleHighRetries_WhenRetriesSetToHighValue() {
        // Arrange
        ReflectionTestUtils.setField(awsConfig, "numRetries", 100);

        // Act
        S3AsyncClient client = awsConfig.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should not be null with high retries");
        assertEquals("s3", client.serviceName(), "Service name should be s3");
        
        // Clean up
        client.close();
    }

    @Test
    void s3AsyncClient_ShouldHandleNullRetries_WhenRetriesIsNull() {
        // Arrange
        ReflectionTestUtils.setField(awsConfig, "numRetries", null);

        // Act & Assert
        // Should handle null retries gracefully (might use default or skip retry config)
        assertDoesNotThrow(() -> {
            S3AsyncClient client = awsConfig.s3AsyncClient();
            assertNotNull(client, "S3AsyncClient should not be null with null retries");
            client.close();
        });
    }

    @Test
    void s3AsyncClient_ShouldCreateValidClientConfiguration_WhenDefaultSettingsUsed() {
        // Act
        S3AsyncClient client = awsConfig.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should not be null");
        assertNotNull(client.serviceClientConfiguration(), "Client configuration should not be null");
        assertNotNull(client.serviceClientConfiguration().region(), "Region should be configured");
        assertEquals("us-east-1", client.serviceClientConfiguration().region().id(), 
                     "Default region should be us-east-1");
        
        // Clean up
        client.close();
    }

    @Test
    void s3AsyncClient_ShouldUseEuWest1Region_WhenEuWest1RegionConfigured() {
        // Arrange
        ReflectionTestUtils.setField(awsConfig, "awsRegion", "eu-west-1");

        // Act
        S3AsyncClient client = awsConfig.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should not be null");
        assertEquals("eu-west-1", client.serviceClientConfiguration().region().id(),
                     "Client should use eu-west-1 region");
        
        // Clean up
        client.close();
    }
}
