package com.lithespeed.hellojava06.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Profile({ "preprod", "prod" })  // K8s environments only
public class AwsS3Service implements S3Service {

    private final S3Client s3Client;
    private final String bucketName;

    // For unit testing with mock S3Client
    public AwsS3Service(S3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    // Production constructor for K8s/BOM integration
    public AwsS3Service(@Value("${aws.s3.region}") String region,
                       @Value("${aws.s3.bucket-name}") String bucketName,
                       @Value("${aws.s3.use-iam-role:true}") boolean useIamRole,
                       @Value("${aws.s3.connection-timeout:10000}") int connectionTimeout,
                       @Value("${aws.s3.socket-timeout:30000}") int socketTimeout,
                       @Value("${aws.s3.max-connections:25}") int maxConnections) {
        this.bucketName = bucketName;
        
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region));
        
        if (useIamRole) {
            // Use DefaultCredentialsProvider for IAM roles (K8s IRSA)
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        
        // Apply K8s-appropriate timeouts and connection settings
        builder.overrideConfiguration(config -> config
                .apiCallTimeout(Duration.ofMillis(socketTimeout))
                .apiCallAttemptTimeout(Duration.ofMillis(connectionTimeout)));
        
        this.s3Client = builder.build();
    }

    // Main implementation methods (matching interface)
    @Override
    public void uploadFile(String fileName) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(this.bucketName)  // Use BOM-provisioned bucket
                .key(fileName)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromString("dummy content"));
    }

    @Override
    public List<String> listFiles() {
        ListObjectsRequest request = ListObjectsRequest.builder()
                .bucket(this.bucketName)
                .build();

        ListObjectsResponse response = s3Client.listObjects(request);

        return response.contents()
                .stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    // Additional utility methods (not in interface)
    public String uploadFileWithContent(String key, String content) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(this.bucketName)
                .key(key)
                .build();

        PutObjectResponse response = s3Client.putObject(putRequest, 
                RequestBody.fromString(content));
        return response.eTag();
    }

    public String downloadFile(String key) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(this.bucketName)
                .key(key)
                .build();

        return s3Client.getObjectAsBytes(getRequest).asUtf8String();
    }

    public boolean deleteFile(String key) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(this.bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
