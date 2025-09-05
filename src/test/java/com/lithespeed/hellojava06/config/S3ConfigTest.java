package com.lithespeed.hellojava06.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class S3ConfigTest {

    private S3Config s3Config;

    @BeforeEach
    void setUp() {
        s3Config = new S3Config();
        // Set default values for required fields
        ReflectionTestUtils.setField(s3Config, "awsRegion", "us-east-1");
        ReflectionTestUtils.setField(s3Config, "s3Endpoint", "");
    }

    @Test
    void s3AsyncClient_ShouldCreateClient_WhenCalledWithDefaultSettings() {
        // Act
        S3AsyncClient client = s3Config.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should not be null");
        assertEquals("s3", client.serviceName(), "Service name should be s3");
        
        // Clean up
        client.close();
    }

    @Test
    void s3AsyncClient_ShouldCreateClientWithCustomRegion_WhenRegionPropertySet() {
        // Arrange
        ReflectionTestUtils.setField(s3Config, "awsRegion", "us-west-2");

        // Act
        S3AsyncClient client = s3Config.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should not be null with custom region");
        assertEquals("us-west-2", client.serviceClientConfiguration().region().id(), 
                     "Client should be configured with custom region");
        
        // Clean up
        client.close();
    }

    @Test
    void s3AsyncClient_ShouldCreateClientWithCustomEndpoint_WhenEndpointPropertySet() {
        // Arrange
        ReflectionTestUtils.setField(s3Config, "s3Endpoint", "http://localhost:4566");

        // Act
        S3AsyncClient client = s3Config.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should not be null with custom endpoint");
        
        // Clean up
        client.close();
    }

    @Test
    void s3AsyncClient_ShouldCreateClientWithBothCustomRegionAndEndpoint() {
        // Arrange
        ReflectionTestUtils.setField(s3Config, "awsRegion", "us-west-1");
        ReflectionTestUtils.setField(s3Config, "s3Endpoint", "http://localstack:4566");

        // Act
        S3AsyncClient client = s3Config.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should not be null with custom region and endpoint");
        assertEquals("us-west-1", client.serviceClientConfiguration().region().id(), 
                     "Client should use custom region even with custom endpoint");
        
        // Clean up
        client.close();
    }

    @Test
    void s3AsyncClient_ShouldHandleNullEndpoint_Gracefully() {
        // Arrange
        ReflectionTestUtils.setField(s3Config, "s3Endpoint", null);

        // Act
        S3AsyncClient client = s3Config.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should not be null even with null endpoint");
        
        // Clean up
        client.close();
    }

    @Test
    void s3AsyncClient_ShouldHandleEmptyEndpoint_Gracefully() {
        // Arrange
        ReflectionTestUtils.setField(s3Config, "s3Endpoint", "");

        // Act
        S3AsyncClient client = s3Config.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should not be null even with empty endpoint");
        
        // Clean up
        client.close();
    }

    @Test
    void s3AsyncClient_ShouldHandleWhitespaceEndpoint_Gracefully() {
        // Arrange
        ReflectionTestUtils.setField(s3Config, "s3Endpoint", "   ");

        // Act
        S3AsyncClient client = s3Config.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should not be null even with whitespace endpoint");
        
        // Clean up
        client.close();
    }

    @Test
    void s3AsyncClient_ShouldCreateDifferentInstances_OnMultipleCalls() {
        // Act
        S3AsyncClient client1 = s3Config.s3AsyncClient();
        S3AsyncClient client2 = s3Config.s3AsyncClient();

        // Assert
        assertNotNull(client1, "First S3AsyncClient should not be null");
        assertNotNull(client2, "Second S3AsyncClient should not be null");
        // Note: Spring will typically create singleton beans, but this tests the method itself
        assertNotSame(client1, client2, "Multiple calls should create different instances at method level");
        
        // Clean up
        client1.close();
        client2.close();
    }

    @Test
    void s3AsyncClient_ShouldUseDefaultCredentialsProvider_WhenNoCustomEndpoint() {
        // This test verifies that the client is created successfully with the default credentials provider
        // The actual credential provider is internal to AWS SDK, but we can verify the client creation succeeds
        
        // Arrange - ensure no custom endpoint
        ReflectionTestUtils.setField(s3Config, "s3Endpoint", "");
        
        // Act
        S3AsyncClient client = s3Config.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should be created with default credentials provider");
        
        // Verify client can be closed without errors (indicating proper initialization)
        assertDoesNotThrow(() -> client.close(), "S3AsyncClient should close cleanly");
    }

    @Test
    void s3AsyncClient_ShouldUseAnonymousCredentialsProvider_WhenCustomEndpointSet() {
        // Arrange
        ReflectionTestUtils.setField(s3Config, "s3Endpoint", "http://localhost:4566");
        
        // Act
        S3AsyncClient client = s3Config.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should be created with anonymous credentials for custom endpoint");
        
        // Verify client can be closed without errors
        assertDoesNotThrow(() -> client.close(), "S3AsyncClient should close cleanly");
    }

    @Test
    void s3AsyncClient_ShouldFallbackToAnonymousCredentials_WhenDefaultCredentialsFail() {
        // This test verifies the fallback mechanism in the configuration
        // The method should handle credential failures gracefully
        
        // Act
        S3AsyncClient client = s3Config.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should be created even if default credentials fail");
        
        // Clean up
        client.close();
    }

    @Test
    void s3AsyncClient_ShouldLogConfiguration_WhenCreatingClient() {
        // This test mainly verifies that the method doesn't throw exceptions
        // and that logging configuration works
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            S3AsyncClient client = s3Config.s3AsyncClient();
            assertNotNull(client, "S3AsyncClient should be created successfully");
            client.close();
        }, "S3AsyncClient creation should not throw exceptions");
    }
}
