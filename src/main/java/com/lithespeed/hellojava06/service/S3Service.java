package com.lithespeed.hellojava06.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.processing.Generated;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class S3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);
    private final S3Client s3Client;
    private final String bucketName;

    // For testing with injected S3Client (e.g., LocalStack)
    // @Generated annotation excludes this constructor from JaCoCo coverage since
    // it's only used in tests
    @Generated("test-only-constructor")
    public S3Service(S3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    // Production constructor for K8s/BOM integration with flexible endpoint support
    @Autowired
    public S3Service(@Value("${aws.s3.region:us-east-1}") String region,
            @Value("${aws.s3.bucket-name:test-bucket}") String bucketName,
            @Value("${aws.s3.endpoint-url:}") String endpointUrl,
            @Value("${aws.s3.connection-timeout:10000}") int connectionTimeout,
            @Value("${aws.s3.socket-timeout:30000}") int socketTimeout,
            @Value("${aws.s3.max-connections:25}") int maxConnections) {

        this.bucketName = bucketName;

        // Comprehensive debug logging to understand configuration
        logger.info("=== S3Service Constructor Debug ===");
        logger.info("region: '{}'", region);
        logger.info("bucketName: '{}'", bucketName);
        logger.info("endpointUrl: '{}'", endpointUrl);
        logger.info("endpointUrl isEmpty: {}", endpointUrl.isEmpty());
        logger.info("connectionTimeout: {}", connectionTimeout);
        logger.info("socketTimeout: {}", socketTimeout);
        logger.info("maxConnections: {}", maxConnections);

        // Log the bucket configuration for debugging
        logger.info("S3Service configured with bucket: {}, region: {}, endpoint: {}",
                bucketName, region, endpointUrl.isEmpty() ? "default" : endpointUrl);

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region));

        // Always use DefaultCredentialsProvider for K8s IRSA authentication
        logger.info("Using DefaultCredentialsProvider for IRSA authentication in K8s");
        builder.credentialsProvider(DefaultCredentialsProvider.create());

        // Configure endpoint override if provided
        if (!endpointUrl.isEmpty()) {
            builder.endpointOverride(URI.create(endpointUrl));
            builder.forcePathStyle(true); // Required for non-AWS S3 endpoints
        }

        // Apply timeouts
        builder.overrideConfiguration(config -> config
                .apiCallTimeout(Duration.ofMillis(socketTimeout))
                .apiCallAttemptTimeout(Duration.ofMillis(connectionTimeout)));

        this.s3Client = builder.build();
    }

    // Main implementation method for multipart file uploads
    public String uploadFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        try {
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.trim().isEmpty()) {
                fileName = "uploaded-file-" + System.currentTimeMillis();
            }

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(this.bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            PutObjectResponse response = s3Client.putObject(putRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return response.eTag();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file content", e);
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    public List<String> listFiles() {
        try {
            ListObjectsRequest request = ListObjectsRequest.builder()
                    .bucket(this.bucketName)
                    .build();

            ListObjectsResponse response = s3Client.listObjects(request);

            return response.contents()
                    .stream()
                    .map(S3Object::key)
                    .collect(Collectors.toList());
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to list files", e);
        }
    }

    /**
     * Debug method to show what credentials the S3Client is actually using
     * DELETE THIS METHOD once debugging is complete!
     */
    public Map<String, String> debugCredentials() {
        Map<String, String> result = new HashMap<>();
        
        try {
            // Get the credentials provider from the S3Client
            var credentialsProvider = s3Client.serviceClientConfiguration().credentialsProvider();
            result.put("credentials-provider-type", credentialsProvider.getClass().getSimpleName());
            
            // Try to resolve actual credentials (don't log the actual keys for security)
            try {
                // Use DefaultCredentialsProvider directly to get the same credentials the S3Client would use
                DefaultCredentialsProvider defaultProvider = DefaultCredentialsProvider.create();
                AwsCredentials creds = defaultProvider.resolveCredentials();
                result.put("credential-type", creds.getClass().getSimpleName());
                result.put("has-access-key", creds.accessKeyId() != null && !creds.accessKeyId().isEmpty() ? "true" : "false");
                result.put("access-key-prefix", creds.accessKeyId() != null && creds.accessKeyId().length() > 4 
                    ? creds.accessKeyId().substring(0, 4) + "****" : "null");
                
                // Check if it's a session credentials (indicates IAM role/STS)
                if (creds instanceof software.amazon.awssdk.auth.credentials.AwsSessionCredentials) {
                    software.amazon.awssdk.auth.credentials.AwsSessionCredentials sessionCreds = 
                        (software.amazon.awssdk.auth.credentials.AwsSessionCredentials) creds;
                    result.put("has-session-token", sessionCreds.sessionToken() != null ? "true" : "false");
                    result.put("session-token-prefix", sessionCreds.sessionToken() != null && sessionCreds.sessionToken().length() > 10
                        ? sessionCreds.sessionToken().substring(0, 10) + "****" : "null");
                    result.put("is-session-credentials", "true");
                } else {
                    result.put("is-session-credentials", "false");
                }
                
            } catch (Exception credErr) {
                result.put("credential-resolution-error", credErr.getMessage());
            }
            
            // Additional debug info
            result.put("bucket-name", this.bucketName);
            result.put("s3-client-region", s3Client.serviceClientConfiguration().region().id());
            
        } catch (Exception e) {
            result.put("debug-error", e.getMessage());
            result.put("error-type", e.getClass().getSimpleName());
        }
        
        logger.info("S3Service credentials debug: {}", result);
        return result;
    }
}
