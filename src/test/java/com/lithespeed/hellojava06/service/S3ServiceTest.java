package com.lithespeed.hellojava06.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private MultipartFile multipartFile;

    private S3Service s3Service;

    private final String bucketName = "test-bucket";
    private final String region = "us-east-1";
    private final String testKey = "test-file.txt";
    private final byte[] testContent = "test content".getBytes();

    @BeforeEach
    void setUp() {
        s3Service = new S3Service(s3Client);
        
        // Use reflection to set the private fields
        setPrivateField(s3Service, "bucketName", bucketName);
        setPrivateField(s3Service, "region", region);
    }

    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

    @Test
    void uploadFile_WithValidFile_ShouldReturnFileUrl() throws IOException {
        // Given
        InputStream inputStream = new ByteArrayInputStream(testContent);
        when(multipartFile.getInputStream()).thenReturn(inputStream);
        when(multipartFile.getSize()).thenReturn((long) testContent.length);
        when(multipartFile.getContentType()).thenReturn("text/plain");

        // When
        String result = s3Service.uploadFile(testKey, multipartFile);

        // Then
        assertThat(result).isEqualTo("https://test-bucket.s3.us-east-1.amazonaws.com/test-file.txt");
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadFile_WithS3Exception_ShouldThrowRuntimeException() throws IOException {
        // Given
        InputStream inputStream = new ByteArrayInputStream(testContent);
        when(multipartFile.getInputStream()).thenReturn(inputStream);
        when(multipartFile.getSize()).thenReturn((long) testContent.length);
        when(multipartFile.getContentType()).thenReturn("text/plain");

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("Upload failed").build());

        // When & Then
        assertThatThrownBy(() -> s3Service.uploadFile(testKey, multipartFile))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to upload file to S3");
    }

    @Test
    void listFiles_WithPrefix_ShouldReturnFileList() {
        // Given
        String prefix = "uploads/";
        List<S3Object> s3Objects = Arrays.asList(
                S3Object.builder().key("uploads/file1.txt").build(),
                S3Object.builder().key("uploads/file2.txt").build());

        ListObjectsV2Response response = ListObjectsV2Response.builder()
                .contents(s3Objects)
                .build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(response);

        // When
        List<String> result = s3Service.listFiles(prefix);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder("uploads/file1.txt", "uploads/file2.txt");
        verify(s3Client, times(1)).listObjectsV2(any(ListObjectsV2Request.class));
    }

    @Test
    void listFiles_WithS3Exception_ShouldThrowRuntimeException() {
        // Given
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenThrow(S3Exception.builder().message("List failed").build());

        // When & Then
        assertThatThrownBy(() -> s3Service.listFiles("prefix"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to list files from S3");
    }
}
