package com.lithespeed.hellojava06.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

@Component("s3")
public class S3HealthIndicator implements HealthIndicator {

    private final S3Client s3Client;
    private final String bucketName;

    public S3HealthIndicator(S3Client s3Client, 
                           @Value("${aws.s3.bucket-name}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @Override
    public Health health() {
        try {
            // Check if we can access the S3 bucket
            s3Client.headBucket(HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
            
            return Health.up()
                    .withDetail("bucket", bucketName)
                    .withDetail("status", "accessible")
                    .build();
                    
        } catch (Exception e) {
            return Health.down()
                    .withDetail("bucket", bucketName)
                    .withDetail("error", e.getMessage())
                    .withDetail("status", "not accessible")
                    .build();
        }
    }
}
