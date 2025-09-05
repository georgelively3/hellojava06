package com.lithespeed.hellojava06.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that S3Config respects profile conditions
 */
class S3ConfigProfileExclusionTest {

    @Test
    void s3AsyncClientBean_ShouldNotBeCreated_WhenPreprodProfileActive() {
        // Arrange
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        ConfigurableEnvironment environment = context.getEnvironment();
        environment.setActiveProfiles("preprod");  // S3Config is only for "default" profile
        
        System.setProperty("aws.s3.region", "us-east-1");
        System.setProperty("aws.s3.endpoint", "");
        
        context.register(S3Config.class);
        context.refresh();

        try {
            // Act & Assert
            assertFalse(context.containsBean("s3AsyncClient"), 
                       "s3AsyncClient bean should NOT be available when preprod profile is active");
        } finally {
            // Clean up
            context.close();
            System.clearProperty("aws.s3.region");
            System.clearProperty("aws.s3.endpoint");
        }
    }

    @Test
    void s3AsyncClientBean_ShouldNotBeCreated_WhenMultipleNonDefaultProfilesActive() {
        // Arrange
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        ConfigurableEnvironment environment = context.getEnvironment();
        environment.setActiveProfiles("test", "preprod");  // Multiple non-default profiles
        
        System.setProperty("aws.s3.region", "us-east-1");
        System.setProperty("aws.s3.endpoint", "");
        
        context.register(S3Config.class);
        context.refresh();

        try {
            // Act & Assert
            assertFalse(context.containsBean("s3AsyncClient"), 
                       "s3AsyncClient bean should NOT be available when non-default profiles are active");
        } finally {
            // Clean up
            context.close();
            System.clearProperty("aws.s3.region");
            System.clearProperty("aws.s3.endpoint");
        }
    }

    @Test
    void s3AsyncClientBean_ShouldBeCreated_WhenDefaultAndOtherProfilesActive() {
        // This test verifies that the bean IS created when default profile is among the active profiles
        
        // Arrange
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        ConfigurableEnvironment environment = context.getEnvironment();
        environment.setActiveProfiles("default", "integration");  // default + other profile
        
        System.setProperty("aws.s3.region", "us-east-1");
        System.setProperty("aws.s3.endpoint", "");
        
        context.register(S3Config.class);
        context.refresh();

        try {
            // Act & Assert
            assertTrue(context.containsBean("s3AsyncClient"), 
                      "s3AsyncClient bean SHOULD be available when default profile is among active profiles");
        } finally {
            // Clean up
            context.close();
            System.clearProperty("aws.s3.region");
            System.clearProperty("aws.s3.endpoint");
        }
    }
}
