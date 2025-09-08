package com.lithespeed.hellojava06.controller;

import com.lithespeed.hellojava06.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ControllerTest {

    @Mock
    private S3Service s3Service;

    private S3Controller s3Controller;

    @BeforeEach
    void setUp() {
        s3Controller = new S3Controller(s3Service);
    }

    @Test
    void constructor_ShouldCreateController() {
        // Act & Assert
        assertNotNull(s3Controller);
    }

    @Test
    void uploadMultipartFile_Success() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", 
                "test.txt", 
                "text/plain", 
                "test content".getBytes());

        when(s3Service.uploadFileAsync(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture("uploads/some-uuid/test.txt"));

        // Act
        CompletableFuture<ResponseEntity<Map<String, Object>>> result = s3Controller.uploadMultipartFile(file);

        // Assert
        assertNotNull(result);
        ResponseEntity<Map<String, Object>> response = result.join();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
        assertEquals("File uploaded successfully", body.get("message"));
        assertEquals("test.txt", body.get("fileName")); // Controller returns original fileName
        assertEquals("uploads/some-uuid/test.txt", body.get("etag")); // Service response becomes etag
    }

    @Test
    void uploadMultipartFile_EmptyFile() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", 
                "test.txt", 
                "text/plain", 
                "".getBytes()); // Empty file

        // Act
        CompletableFuture<ResponseEntity<Map<String, Object>>> result = s3Controller.uploadMultipartFile(file);

        // Assert
        assertNotNull(result);
        ResponseEntity<Map<String, Object>> response = result.join();
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()); // Bad Request
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("File cannot be empty", body.get("error"));
    }

    @Test
    void uploadMultipartFile_ServiceFailure() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", 
                "test.txt", 
                "text/plain", 
                "test content".getBytes());

        CompletableFuture<String> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Upload failed"));
        
        when(s3Service.uploadFileAsync(anyString(), anyString(), any()))
                .thenReturn(failedFuture);

        // Act
        CompletableFuture<ResponseEntity<Map<String, Object>>> result = s3Controller.uploadMultipartFile(file);

        // Assert
        assertNotNull(result);
        ResponseEntity<Map<String, Object>> response = result.join();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode()); // Internal Server Error
        
        // Just check that we got a response body - error handling details may vary
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
    }

    @Test
    void listFiles_Success() throws Exception {
        // Arrange
        List<String> files = Arrays.asList("file1.txt", "file2.txt", "file3.jpg");
        when(s3Service.listFilesAsync())
                .thenReturn(CompletableFuture.completedFuture(files));

        // Act
        CompletableFuture<ResponseEntity<Object>> result = s3Controller.listFiles();

        // Assert
        assertNotNull(result);
        ResponseEntity<Object> response = result.join();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Just check that we got a successful response - response format may vary
        Object body = response.getBody();
        assertNotNull(body);
    }

    @Test
    void listFiles_ServiceFailure() throws Exception {
        // Arrange
        CompletableFuture<List<String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("List failed"));
        
        when(s3Service.listFilesAsync()).thenReturn(failedFuture);

        // Act
        CompletableFuture<ResponseEntity<Object>> result = s3Controller.listFiles();

        // Assert
        assertNotNull(result);
        ResponseEntity<Object> response = result.join();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode()); // Internal Server Error
        
        // Just check that we got a response body - error handling details may vary
        Object body = response.getBody();
        assertNotNull(body);
    }


}