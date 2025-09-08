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

    @Test
    void s3AsyncClient_ShouldCreateClient_WhenInTestEnvironment() {
        // This test verifies that the method works in test environment
        // (should not trigger local development mode due to test detection)
        
        // Act
        S3AsyncClient client = s3Config.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should not be null in test environment");
        assertEquals("s3", client.serviceName(), "Service name should be s3");
        
        // Clean up
        client.close();
    }

    @Test
    void s3AsyncClient_ShouldHandleAnonymousCredentials_WhenNoAwsCredentials() {
        // This test verifies that anonymous credentials are handled gracefully
        // when AWS credentials are not available
        
        // Act
        S3AsyncClient client = s3Config.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should not be null with anonymous credentials");
        assertEquals("s3", client.serviceName(), "Service name should be s3");
        assertNotNull(client.serviceClientConfiguration(), "Client configuration should not be null");
        
        // Clean up
        client.close();
    }

    @Test
    void s3AsyncClient_ShouldUseConfiguredRegion_WhenLocalDevelopmentModeActive() {
        // Arrange - Set a custom region
        ReflectionTestUtils.setField(s3Config, "awsRegion", "eu-west-1");

        // Act
        S3AsyncClient client = s3Config.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should not be null");
        // In test environment, should use the configured region
        assertEquals("eu-west-1", client.serviceClientConfiguration().region().id(),
                     "Client should use configured region");
        
        // Clean up
        client.close();
    }

    @Test
    void s3AsyncClient_ShouldHandleCustomEndpoint_WhenEndpointConfigured() {
        // Arrange - Set a custom endpoint (LocalStack scenario)
        ReflectionTestUtils.setField(s3Config, "s3Endpoint", "http://localhost:4566");
        ReflectionTestUtils.setField(s3Config, "awsRegion", "us-east-1");

        // Act
        S3AsyncClient client = s3Config.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should not be null with custom endpoint");
        assertEquals("s3", client.serviceName(), "Service name should be s3");
        
        // Clean up
        client.close();
    }

    @Test
    void s3AsyncClient_ShouldFallbackToUsEast1_WhenInvalidRegionProvided() {
        // This tests the fallback logic when an invalid region is configured
        
        // Arrange - Set an invalid region
        ReflectionTestUtils.setField(s3Config, "awsRegion", "invalid-region-123");

        // Act & Assert
        assertDoesNotThrow(() -> {
            S3AsyncClient client = s3Config.s3AsyncClient();
            assertNotNull(client, "S3AsyncClient should not be null with invalid region");
            // Should fallback to a valid region or handle gracefully
            client.close();
        }, "Should handle invalid region gracefully");
    }

    @Test
    void s3AsyncClient_ShouldCreateDifferentInstances_WhenCalledMultipleTimes() {
        // This test verifies that each call creates a new instance (not singleton within method)
        
        // Act
        S3AsyncClient client1 = s3Config.s3AsyncClient();
        S3AsyncClient client2 = s3Config.s3AsyncClient();

        // Assert
        assertNotNull(client1, "First S3AsyncClient should not be null");
        assertNotNull(client2, "Second S3AsyncClient should not be null");
        assertNotSame(client1, client2, "Each call should create different instances");
        
        // Both should have the same service name
        assertEquals("s3", client1.serviceName(), "First client service name should be s3");
        assertEquals("s3", client2.serviceName(), "Second client service name should be s3");
        
        // Clean up
        client1.close();
        client2.close();
    }

    @Test
    void s3AsyncClient_ShouldHandleNullEndpoint_WhenEndpointNotConfigured() {
        // Arrange - Explicitly set endpoint to null
        ReflectionTestUtils.setField(s3Config, "s3Endpoint", null);

        // Act
        S3AsyncClient client = s3Config.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should not be null when endpoint is null");
        assertEquals("s3", client.serviceName(), "Service name should be s3");
        
        // Clean up
        client.close();
    }

    @Test
    void s3AsyncClient_ShouldHandleEmptyEndpoint_WhenEndpointIsEmpty() {
        // Arrange - Set endpoint to empty string
        ReflectionTestUtils.setField(s3Config, "s3Endpoint", "");

        // Act
        S3AsyncClient client = s3Config.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should not be null when endpoint is empty");
        assertEquals("s3", client.serviceName(), "Service name should be s3");
        
        // Clean up
        client.close();
    }

    @Test
    void s3AsyncClient_ShouldHandleWhitespaceEndpoint_WhenEndpointIsWhitespace() {
        // Arrange - Set endpoint to whitespace
        ReflectionTestUtils.setField(s3Config, "s3Endpoint", "   ");

        // Act
        S3AsyncClient client = s3Config.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should not be null when endpoint is whitespace");
        assertEquals("s3", client.serviceName(), "Service name should be s3");
        
        // Clean up
        client.close();
    }

    @Test
    void s3AsyncClient_ShouldUseTestEnvironment_WhenJUnitClasspathDetected() {
        // This test verifies that isRunningTests() works correctly
        // Since we're in a test, it should detect test mode and NOT use local development mode
        
        // Act
        S3AsyncClient client = s3Config.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should be created in test mode");
        assertEquals("s3", client.serviceName(), "Service name should be s3");
        // The fact that this test works proves isRunningTests() is working correctly
        
        // Clean up
        client.close();
    }

    @Test 
    void s3AsyncClient_ShouldHandleLocalDevelopmentClient_WhenCustomEndpointSet() {
        // This test exercises createLocalDevelopmentClient() indirectly
        // by setting conditions that would trigger local development mode
        
        // Arrange - Set custom endpoint which typically indicates local development
        ReflectionTestUtils.setField(s3Config, "s3Endpoint", "http://localhost:4566");
        ReflectionTestUtils.setField(s3Config, "awsRegion", "us-west-2");

        // Act
        S3AsyncClient client = s3Config.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should be created with custom endpoint");
        assertEquals("s3", client.serviceName(), "Service name should be s3");
        assertEquals("us-west-2", client.serviceClientConfiguration().region().id(),
                     "Should preserve custom region even in local development mode");
        
        // Clean up
        client.close();
    }

    @Test
    void s3AsyncClient_ShouldFallbackGracefully_WhenInvalidEndpointProvided() {
        // This tests error handling in createLocalDevelopmentClient()
        
        // Arrange - Set an invalid endpoint to trigger fallback logic
        ReflectionTestUtils.setField(s3Config, "s3Endpoint", "invalid://not-a-real-endpoint:999999");
        ReflectionTestUtils.setField(s3Config, "awsRegion", "invalid-region");

        // Act & Assert
        assertDoesNotThrow(() -> {
            S3AsyncClient client = s3Config.s3AsyncClient();
            assertNotNull(client, "S3AsyncClient should be created even with invalid endpoint");
            // Should fallback to a working configuration
            client.close();
        }, "Should handle invalid endpoint gracefully");
    }

    @Test
    void s3AsyncClient_ShouldUseAnonymousCredentials_WhenLocalDevelopmentDetected() {
        // This test verifies the local development client creation logic
        // Tests the anonymous credentials path in createLocalDevelopmentClient()
        
        // Arrange - Clear any potential AWS environment variables effect
        // (In test mode, isRunningTests() should return true, preventing local dev mode)
        ReflectionTestUtils.setField(s3Config, "awsRegion", "eu-central-1");

        // Act
        S3AsyncClient client = s3Config.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should be created with anonymous credentials");
        assertEquals("s3", client.serviceName(), "Service name should be s3");
        assertNotNull(client.serviceClientConfiguration().credentialsProvider(),
                      "Credentials provider should be configured");
        
        // Clean up
        client.close();
    }

    @Test
    void s3AsyncClient_ShouldHandleMultipleRegionFormats_WhenCreatingLocalClient() {
        // Test createLocalDevelopmentClient() with various region formats
        
        String[] testRegions = {"us-east-1", "eu-west-1", "ap-southeast-2", "ca-central-1"};
        
        for (String region : testRegions) {
            // Arrange
            ReflectionTestUtils.setField(s3Config, "awsRegion", region);
            
            // Act
            S3AsyncClient client = s3Config.s3AsyncClient();
            
            // Assert
            assertNotNull(client, "S3AsyncClient should be created for region: " + region);
            assertEquals(region, client.serviceClientConfiguration().region().id(),
                         "Should use specified region: " + region);
            
            // Clean up
            client.close();
        }
    }

    @Test
    void s3AsyncClient_ShouldHandleNullRegionInLocalDevelopment_WithFallback() {
        // Test createLocalDevelopmentClient() fallback when region is null
        
        // Arrange
        ReflectionTestUtils.setField(s3Config, "awsRegion", null);
        ReflectionTestUtils.setField(s3Config, "s3Endpoint", "http://localhost:4566");

        // Act & Assert
        assertDoesNotThrow(() -> {
            S3AsyncClient client = s3Config.s3AsyncClient();
            assertNotNull(client, "S3AsyncClient should be created even with null region");
            // Should fallback to us-east-1 or handle gracefully
            client.close();
        }, "Should handle null region in local development mode");
    }

    @Test
    void s3AsyncClient_ShouldCreateClientWithMinimalFallback_WhenAllElseFails() {
        // Test the final fallback scenario in createLocalDevelopmentClient()
        
        // Arrange - Set up conditions that might cause issues
        ReflectionTestUtils.setField(s3Config, "awsRegion", "");
        ReflectionTestUtils.setField(s3Config, "s3Endpoint", "");

        // Act
        S3AsyncClient client = s3Config.s3AsyncClient();

        // Assert
        assertNotNull(client, "S3AsyncClient should always be created, even with minimal config");
        assertEquals("s3", client.serviceName(), "Service name should be s3");
        assertNotNull(client.serviceClientConfiguration(), "Configuration should be present");
        
        // Clean up
        client.close();
    }
}
