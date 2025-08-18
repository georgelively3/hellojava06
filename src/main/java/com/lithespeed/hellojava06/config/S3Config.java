package com.lithespeed.hellojava06.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.time.Duration;

@Configuration
public class S3Config {

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.access-key:}")
    private String accessKey;

    @Value("${aws.s3.secret-key:}")
    private String secretKey;

    @Value("${aws.s3.endpoint-url:}")
    private String endpointUrl;

    // Enterprise configuration values
    @Value("${aws.s3.connection-timeout:10000}")
    private Integer connectionTimeout;

    @Value("${aws.s3.socket-timeout:50000}")
    private Integer socketTimeout;

    @Value("${aws.s3.max-connections:50}")
    private Integer maxConnections;

    @Value("${aws.s3.max-error-retry:3}")
    private Integer maxErrorRetry;

    /**
     * Production S3 Client - Uses IAM roles or explicit credentials
     * Configured for enterprise workloads with proper timeouts and retry policies
     */
    @Bean
    @Profile({ "prod", "uat", "preprod", "!localstack", "!test" })
    public S3Client enterpriseS3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(false) // Use virtual-hosted style for AWS S3
                        .build())
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(Duration.ofMillis(socketTimeout))
                        .apiCallAttemptTimeout(Duration.ofMillis(connectionTimeout))
                        .retryPolicy(RetryPolicy.builder()
                                .numRetries(maxErrorRetry)
                                .build())
                        .build());

        // Use explicit credentials if provided, otherwise use default credential chain
        // In enterprise environments, prefer IAM roles over explicit credentials
        if (hasExplicitCredentials()) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        } else {
            // Default credential provider chain:
            // 1. Java system properties
            // 2. Environment variables
            // 3. IAM instance/container credentials
            // 4. IAM roles for service accounts (in Kubernetes)
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }

    /**
     * LocalStack S3 Client for development and testing
     */
    @Bean
    @Primary
    @Profile({ "localstack", "!test" })
    public S3Client localStackS3Client() {
        // LocalStack credentials (these are dummy values for LocalStack)
        AwsBasicCredentials credentials = AwsBasicCredentials.create("test", "test");

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true) // LocalStack requires path-style access
                        .build())
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(Duration.ofMillis(socketTimeout))
                        .apiCallAttemptTimeout(Duration.ofMillis(connectionTimeout))
                        .retryPolicy(RetryPolicy.builder()
                                .numRetries(maxErrorRetry)
                                .build())
                        .build());

        // Use custom endpoint if provided (for LocalStack)
        if (endpointUrl != null && !endpointUrl.isEmpty()) {
            builder.endpointOverride(URI.create(endpointUrl));
        }

        return builder.build();
    }

    /**
     * Credentials provider bean for use by other components if needed
     */
    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        if (hasExplicitCredentials()) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        } else {
            return DefaultCredentialsProvider.create();
        }
    }

    private boolean hasExplicitCredentials() {
        return accessKey != null && !accessKey.isEmpty() &&
                secretKey != null && !secretKey.isEmpty();
    }
}
