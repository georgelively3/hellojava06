package com.lithespeed.hellojava06.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

    @Value("${aws.s3.region:us-east-1}")
    private String region;

    @Value("${aws.s3.access-key:}")
    private String accessKey;

    @Value("${aws.s3.secret-key:}")
    private String secretKey;

    @Value("${aws.s3.endpoint-url:}")
    private String endpointUrl;

    @Value("${aws.s3.enabled:false}")
    private boolean s3Enabled;

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
     * Only created when explicitly enabled and in production profiles
     */
    @Bean
    @ConditionalOnProperty(name = "aws.s3.enabled", havingValue = "true")
    @Profile({ "prod", "production", "uat", "preprod" })
    public S3Client enterpriseS3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(false)
                        .build())
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(Duration.ofMillis(socketTimeout))
                        .apiCallAttemptTimeout(Duration.ofMillis(connectionTimeout))
                        .retryPolicy(RetryPolicy.builder()
                                .numRetries(maxErrorRetry)
                                .build())
                        .build());

        if (hasExplicitCredentials()) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }

    /**
     * LocalStack S3 Client for development and testing
     */
    @Bean
    @ConditionalOnProperty(name = "aws.s3.enabled", havingValue = "true")
    @Profile({ "localstack" })
    public S3Client localStackS3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create("test", "test");

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(Duration.ofMillis(socketTimeout))
                        .apiCallAttemptTimeout(Duration.ofMillis(connectionTimeout))
                        .retryPolicy(RetryPolicy.builder()
                                .numRetries(maxErrorRetry)
                                .build())
                        .build());

        if (endpointUrl != null && !endpointUrl.isEmpty()) {
            builder.endpointOverride(URI.create(endpointUrl));
        }

        return builder.build();
    }

    /**
     * Mock S3 Client for development when S3 is disabled
     * This prevents startup failures when S3 is not configured
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "aws.s3.enabled", havingValue = "false", matchIfMissing = true)
    public S3Client mockS3Client() {
        // Return a mock client that won't try to connect to AWS
        // This uses dummy credentials and endpoint
        AwsBasicCredentials credentials = AwsBasicCredentials.create("dummy", "dummy");

        return S3Client.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .endpointOverride(URI.create("http://localhost:9999")) // Dummy endpoint
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    /**
     * Credentials provider bean
     */
    @Bean
    @ConditionalOnProperty(name = "aws.s3.enabled", havingValue = "true")
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
