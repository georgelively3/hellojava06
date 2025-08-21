package com.lithespeed.hellojava06.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Profile({ "aws-s3", "prod", "production" })
public class AwsS3Service implements S3Service {

    private final S3Client s3Client;
    private final String bucketName;

    // For unit testing with mock S3Client
    public AwsS3Service(S3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    // Production constructor
    public AwsS3Service(@Value("${aws.s3.region}") String region,
            @Value("${aws.s3.bucket-name}") String bucketName) {
        this(
                S3Client.builder()
                        .region(Region.of(region))
                        .credentialsProvider(DefaultCredentialsProvider.create())
                        .build(),
                bucketName);
    }

    @Override
    public void uploadFile(String fileName) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();

        s3Client.putObject(request, RequestBody.fromString("dummy content"));
    }

    @Override
    public List<String> listFiles() {
        ListObjectsRequest request = ListObjectsRequest.builder()
                .bucket(bucketName)
                .build();

        ListObjectsResponse response = s3Client.listObjects(request);

        return response.contents()
                .stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }
}
