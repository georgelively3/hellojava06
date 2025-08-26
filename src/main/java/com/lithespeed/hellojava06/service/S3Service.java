package com.lithespeed.hellojava06.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Profile("!localstack") // Exclude this bean when localstack profile is active
public class S3Service {

    private final S3Client s3Client;
    private final String bucketName;

    // For testing with injected S3Client (e.g., LocalStack)
    public S3Service(S3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    // Production constructor for K8s/BOM integration + LocalStack support
    @Autowired
    public S3Service(@Value("${aws.s3.region}") String region,
            @Value("${aws.s3.bucket-name}") String bucketName,
            @Value("${aws.s3.use-iam-role:true}") boolean useIamRole,
            @Value("${aws.s3.endpoint-url:}") String endpointUrl, // ✅ ADD THIS
            @Value("${aws.s3.access-key:}") String accessKey, // ✅ ADD THIS
            @Value("${aws.s3.secret-key:}") String secretKey, // ✅ ADD THIS
            @Value("${aws.s3.connection-timeout:10000}") int connectionTimeout,
            @Value("${aws.s3.socket-timeout:30000}") int socketTimeout,
            @Value("${aws.s3.max-connections:25}") int maxConnections) {

        this.bucketName = bucketName;

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region));

        // Configure credentials
        if (useIamRole) {
            // Use DefaultCredentialsProvider for IAM roles (K8s IRSA)
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        } else if (!accessKey.isEmpty() && !secretKey.isEmpty()) {
            // Use static credentials for LocalStack/dev environments
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)));
        }

        // Configure endpoint for LocalStack
        if (!endpointUrl.isEmpty()) {
            builder.endpointOverride(URI.create(endpointUrl));
            builder.forcePathStyle(true); // Required for LocalStack
        }

        // Apply timeouts
        builder.overrideConfiguration(config -> config
                .apiCallTimeout(Duration.ofMillis(socketTimeout))
                .apiCallAttemptTimeout(Duration.ofMillis(connectionTimeout)));

        this.s3Client = builder.build();
    }

    // Main implementation methods
    public void uploadFile(String fileName) {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(this.bucketName)
                    .key(fileName)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromString("dummy content"));
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to upload file: " + fileName, e);
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

    // Additional utility methods
    public String uploadFileWithContent(String key, String content) {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(this.bucketName)
                    .key(key)
                    .build();

            PutObjectResponse response = s3Client.putObject(putRequest,
                    RequestBody.fromString(content));
            return response.eTag();
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to upload file with content: " + key, e);
        }
    }

    public String downloadFile(String key) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(this.bucketName)
                    .key(key)
                    .build();

            return s3Client.getObjectAsBytes(getRequest).asUtf8String();
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to download file: " + key, e);
        }
    }

    public boolean deleteFile(String key) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(this.bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            return true;
        } catch (S3Exception e) {
            return false;
        }
    }
}
