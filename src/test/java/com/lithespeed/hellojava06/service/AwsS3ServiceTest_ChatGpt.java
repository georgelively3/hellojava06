package com.example.hellojava.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import static org.mockito.Mockito.*;

class AwsS3ServiceTest {

    private S3Client s3Client;
    private AwsS3Service awsS3Service;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        awsS3Service = new AwsS3Service(s3Client, "my-bucket");
    }

    @Test
    void uploadFileCallsS3PutObject() {
        PutObjectResponse putResponse = PutObjectResponse.builder().build();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(putResponse);

        awsS3Service.uploadFile("file1.txt");

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void listFilesCallsS3ListObjects() {
        ListObjectsResponse response = ListObjectsResponse.builder()
                .contents(S3Object.builder().key("file1.txt").build())
                .build();
        when(s3Client.listObjects(any(ListObjectsRequest.class))).thenReturn(response);

        awsS3Service.listFiles();

        verify(s3Client).listObjects(any(ListObjectsRequest.class));
    }
}
