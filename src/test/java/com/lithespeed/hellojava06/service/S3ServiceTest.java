package com.lithespeed.hellojava06.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    private S3Service s3Service;
    private final String testBucket = "test-bucket";

    @BeforeEach
    void setUp() {
        s3Service = new S3Service(s3Client, testBucket);
    }

    @Test
    void uploadFile_Success() {
        // Given
        String fileName = "test-file.txt";
        PutObjectResponse mockResponse = PutObjectResponse.builder()
                .eTag("test-etag")
                .build();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(mockResponse);

        // When & Then
        assertDoesNotThrow(() -> s3Service.uploadFile(fileName));

        // Verify S3Client was called correctly
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadFile_S3Exception_ThrowsRuntimeException() {
        // Given
        String fileName = "test-file.txt";
        S3Exception s3Exception = (S3Exception) S3Exception.builder()
                .message("Access denied")
                .build();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(s3Exception);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> s3Service.uploadFile(fileName));

        assertTrue(exception.getMessage().contains("Failed to upload file"));
        assertTrue(exception.getCause() instanceof S3Exception);
    }

    @Test
    void listFiles_Success() {
        // Given
        S3Object obj1 = S3Object.builder().key("file1.txt").build();
        S3Object obj2 = S3Object.builder().key("file2.txt").build();

        ListObjectsResponse mockResponse = ListObjectsResponse.builder()
                .contents(Arrays.asList(obj1, obj2))
                .build();

        when(s3Client.listObjects(any(ListObjectsRequest.class)))
                .thenReturn(mockResponse);

        // When
        List<String> result = s3Service.listFiles();

        // Then
        assertEquals(2, result.size());
        assertTrue(result.contains("file1.txt"));
        assertTrue(result.contains("file2.txt"));
        verify(s3Client).listObjects(any(ListObjectsRequest.class));
    }

    @Test
    void listFiles_EmptyBucket_ReturnsEmptyList() {
        // Given
        ListObjectsResponse mockResponse = ListObjectsResponse.builder()
                .contents(Collections.emptyList())
                .build();

        when(s3Client.listObjects(any(ListObjectsRequest.class)))
                .thenReturn(mockResponse);

        // When
        List<String> result = s3Service.listFiles();

        // Then
        assertTrue(result.isEmpty());
        verify(s3Client).listObjects(any(ListObjectsRequest.class));
    }

    @Test
    void listFiles_S3Exception_ThrowsRuntimeException() {
        // Given
        S3Exception s3Exception = (S3Exception) S3Exception.builder()
                .message("Bucket not found")
                .build();

        when(s3Client.listObjects(any(ListObjectsRequest.class)))
                .thenThrow(s3Exception);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> s3Service.listFiles());

        assertTrue(exception.getMessage().contains("Failed to list files"));
        assertTrue(exception.getCause() instanceof S3Exception);
    }
}
