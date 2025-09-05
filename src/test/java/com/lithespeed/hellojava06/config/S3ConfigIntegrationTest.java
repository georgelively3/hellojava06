package com.lithespeed.hellojava06.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for S3Config using a minimal Spring context
 * This avoids conflicts with other configuration classes
 */
class S3ConfigIntegrationTest {

    @Test
    void s3AsyncClientBean_ShouldBeAvailable_WhenDefaultProfileActive() {
        // Arrange - Create minimal Spring context with only S3Config
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        ConfigurableEnvironment environment = context.getEnvironment();
        environment.setActiveProfiles("default");
        
        // Set required properties
        System.setProperty("aws.s3.region", "us-east-1");
        System.setProperty("aws.s3.endpoint", "");
        
        context.register(S3Config.class);
        context.refresh();

        try {
            // Act & Assert
            assertTrue(context.containsBean("s3AsyncClient"), 
                       "s3AsyncClient bean should be available in default profile");
            
            S3AsyncClient client = context.getBean(S3AsyncClient.class);
            assertNotNull(client, "S3AsyncClient bean should not be null");
            assertEquals("s3", client.serviceName(), "S3AsyncClient should be for S3 service");
        } finally {
            // Clean up
            context.close();
            System.clearProperty("aws.s3.region");
            System.clearProperty("aws.s3.endpoint");
        }
    }

    @Test
    void s3AsyncClientBean_ShouldBeSingleton_InSpringContext() {
        // Arrange
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        ConfigurableEnvironment environment = context.getEnvironment();
        environment.setActiveProfiles("default");
        
        System.setProperty("aws.s3.region", "us-east-1");
        System.setProperty("aws.s3.endpoint", "");
        
        context.register(S3Config.class);
        context.refresh();

        try {
            // Act
            S3AsyncClient client1 = context.getBean(S3AsyncClient.class);
            S3AsyncClient client2 = context.getBean(S3AsyncClient.class);

            // Assert
            assertSame(client1, client2, "S3AsyncClient bean should be singleton in Spring context");
        } finally {
            // Clean up
            context.close();
            System.clearProperty("aws.s3.region");
            System.clearProperty("aws.s3.endpoint");
        }
    }

    @Test
    void s3AsyncClientBean_ShouldNotBeAvailable_WhenTestProfileActive() {
        // Arrange
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        ConfigurableEnvironment environment = context.getEnvironment();
        environment.setActiveProfiles("test");  // S3Config is only for "default" profile
        
        System.setProperty("aws.s3.region", "us-east-1");
        System.setProperty("aws.s3.endpoint", "");
        
        context.register(S3Config.class);
        context.refresh();

        try {
            // Act & Assert
            assertFalse(context.containsBean("s3AsyncClient"), 
                       "s3AsyncClient bean should NOT be available when test profile is active");
        } finally {
            // Clean up
            context.close();
            System.clearProperty("aws.s3.region");
            System.clearProperty("aws.s3.endpoint");
        }
    }
}
