package com.lithespeed.hellojava06.extension;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * JUnit 5 extension that manages LocalStack container lifecycle for S3 testing.
 * This extension starts a LocalStack container before all tests and stops it after all tests.
 * 
 * Usage:
 * @ExtendWith(S3LocalStackExtension.class)
 * class MyS3Test {
 *   // Test methods can use LocalStack S3 service
 * }
 */
public class S3LocalStackExtension implements BeforeAllCallback, AfterAllCallback {

    private static final String LOCALSTACK_IMAGE = "localstack/localstack:3.0";
    private static LocalStackContainer localStackContainer;
    
    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (localStackContainer == null || !localStackContainer.isRunning()) {
            startLocalStackContainer();
        }
    }
    
    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (localStackContainer != null && localStackContainer.isRunning()) {
            stopLocalStackContainer();
        }
    }
    
    private void startLocalStackContainer() {
        try {
            localStackContainer = new LocalStackContainer(DockerImageName.parse(LOCALSTACK_IMAGE))
                    .withServices(LocalStackContainer.Service.S3)
                    .withEnv("DEBUG", "1")
                    .withEnv("PERSISTENCE", "0"); // Disable persistence for faster startup
                    
            localStackContainer.start();
            
            System.out.println("LocalStack S3 service started");
            System.out.println("S3 endpoint: " + localStackContainer.getEndpointOverride(LocalStackContainer.Service.S3));
            System.out.println("Region: " + localStackContainer.getRegion());
            System.out.println("Access Key: " + localStackContainer.getAccessKey());
            System.out.println("Secret Key: " + localStackContainer.getSecretKey());
        } catch (Exception e) {
            System.err.println("Failed to start LocalStack container: " + e.getMessage());
            throw new RuntimeException("Could not start LocalStack container", e);
        }
    }
    
    private void stopLocalStackContainer() {
        try {
            if (localStackContainer != null) {
                localStackContainer.stop();
                System.out.println("LocalStack container stopped");
            }
        } catch (Exception e) {
            System.err.println("Error stopping LocalStack container: " + e.getMessage());
        }
    }
    
    /**
     * Get the running LocalStack container instance.
     * This can be used by tests to configure AWS SDK clients.
     */
    public static LocalStackContainer getLocalStackContainer() {
        return localStackContainer;
    }
    
    /**
     * Get the S3 endpoint URL for the running LocalStack container.
     */
    public static String getS3EndpointUrl() {
        return localStackContainer != null ? 
            localStackContainer.getEndpointOverride(LocalStackContainer.Service.S3).toString() : null;
    }
    
    /**
     * Get the AWS region for LocalStack.
     */
    public static String getAwsRegion() {
        return localStackContainer != null ? localStackContainer.getRegion() : "us-east-1";
    }
    
    /**
     * Get the AWS access key for LocalStack.
     */
    public static String getAwsAccessKey() {
        return localStackContainer != null ? localStackContainer.getAccessKey() : "test";
    }
    
    /**
     * Get the AWS secret key for LocalStack.
     */
    public static String getAwsSecretKey() {
        return localStackContainer != null ? localStackContainer.getSecretKey() : "test";
    }
}
