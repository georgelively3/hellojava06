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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

                Map<String, Object> serviceResponse = new HashMap<>();
                serviceResponse.put("success", true);
                serviceResponse.put("message", "File uploaded successfully");
                serviceResponse.put("fileName", "test.txt");
                serviceResponse.put("etag", "uploads/some-uuid/test.txt");

                when(s3Service.processFileUpload(any()))
                                .thenReturn(CompletableFuture.completedFuture(serviceResponse));

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

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Upload failed");

                when(s3Service.processFileUpload(any()))
                                .thenReturn(CompletableFuture.completedFuture(errorResponse));

                // Act
                CompletableFuture<ResponseEntity<Map<String, Object>>> result = s3Controller.uploadMultipartFile(file);

                // Assert
                assertNotNull(result);
                ResponseEntity<Map<String, Object>> response = result.join();
                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode()); // Internal Server Error

                // Just check that we got a response body - error handling details may vary
                Map<String, Object> body = response.getBody();
                assertNotNull(body);
                assertEquals(false, body.get("success"));
        }

        @Test
        void listFiles_Success() throws Exception {
                // Arrange
                List<String> files = Arrays.asList("file1.txt", "file2.txt", "file3.jpg");
                Map<String, Object> serviceResponse = new HashMap<>();
                serviceResponse.put("success", true);
                serviceResponse.put("files", files);
                serviceResponse.put("count", files.size());

                when(s3Service.processFileList())
                                .thenReturn(CompletableFuture.completedFuture(serviceResponse));

                // Act
                CompletableFuture<ResponseEntity<Object>> result = s3Controller.listFiles();

                // Assert
                assertNotNull(result);
                ResponseEntity<Object> response = result.join();
                assertEquals(HttpStatus.OK, response.getStatusCode());

                // Check that we got a successful response
                Object body = response.getBody();
                assertNotNull(body);
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = (Map<String, Object>) body;
                assertEquals(true, responseMap.get("success"));
        }

        @Test
        void listFiles_ServiceFailure() throws Exception {
                // Arrange
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "List failed");

                when(s3Service.processFileList()).thenReturn(CompletableFuture.completedFuture(errorResponse));

                // Act
                CompletableFuture<ResponseEntity<Object>> result = s3Controller.listFiles();

                // Assert
                assertNotNull(result);
                ResponseEntity<Object> response = result.join();
                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode()); // Internal Server Error

                // Check that we got an error response
                Object body = response.getBody();
                assertNotNull(body);
                @SuppressWarnings("unchecked")
                Map<String, Object> responseMap = (Map<String, Object>) body;
                assertEquals(false, responseMap.get("success"));
        }

        @Test
        void healthCheck_ShouldReturnUpStatus() {
                // Act
                Map<String, String> result = s3Controller.healthCheck();

                // Assert
                assertNotNull(result);
                assertEquals("UP", result.get("status"));
                assertTrue(result.containsKey("status"));
                assertEquals(1, result.size()); // Should only contain status field
        }

        @Test
        void deleteFile_Success() throws Exception {
                // Arrange
                String key = "test-file.txt";
                when(s3Service.deleteFileAsync(key))
                                .thenReturn(CompletableFuture.completedFuture(key));

                // Act
                CompletableFuture<ResponseEntity<String>> result = s3Controller.deleteFile(key);

                // Assert
                assertNotNull(result);
                ResponseEntity<String> response = result.join();
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertEquals("File deleted successfully: " + key, response.getBody());
        }

        @Test
        void deleteFile_ServiceFailure() throws Exception {
                // Arrange
                String key = "test-file.txt";
                CompletableFuture<String> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(new RuntimeException("Delete failed"));

                when(s3Service.deleteFileAsync(key)).thenReturn(failedFuture);

                // Act
                CompletableFuture<ResponseEntity<String>> result = s3Controller.deleteFile(key);

                // Assert
                assertNotNull(result);
                ResponseEntity<String> response = result.join();
                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                assertNotNull(response.getBody());
                assertTrue(response.getBody().contains("Failed to delete file"));
        }

        @Test
        void fileExists_FileExists() throws Exception {
                // Arrange
                String key = "existing-file.txt";
                when(s3Service.fileExistsAsync(key))
                                .thenReturn(CompletableFuture.completedFuture(true));

                // Act
                CompletableFuture<ResponseEntity<Boolean>> result = s3Controller.fileExists(key);

                // Assert
                assertNotNull(result);
                ResponseEntity<Boolean> response = result.join();
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals(Boolean.TRUE, response.getBody());
        }

        @Test
        void fileExists_FileDoesNotExist() throws Exception {
                // Arrange
                String key = "nonexistent-file.txt";
                when(s3Service.fileExistsAsync(key))
                                .thenReturn(CompletableFuture.completedFuture(false));

                // Act
                CompletableFuture<ResponseEntity<Boolean>> result = s3Controller.fileExists(key);

                // Assert
                assertNotNull(result);
                ResponseEntity<Boolean> response = result.join();
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());
                assertEquals(Boolean.FALSE, response.getBody());
        }

        @Test
        void uploadFile_Success() throws Exception {
                // Arrange
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test.txt",
                                "text/plain",
                                "test content".getBytes());
                String key = "uploads/test.txt";

                Map<String, Object> serviceResponse = new HashMap<>();
                serviceResponse.put("success", true);
                serviceResponse.put("message", "File uploaded successfully");

                when(s3Service.processFileUpload(any()))
                                .thenReturn(CompletableFuture.completedFuture(serviceResponse));

                // Act
                CompletableFuture<ResponseEntity<String>> result = s3Controller.uploadFile(file, key);

                // Assert
                assertNotNull(result);
                ResponseEntity<String> response = result.join();
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertEquals("File uploaded successfully", response.getBody());
        }

        @Test
        void uploadFile_EmptyFile() throws Exception {
                // Arrange
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test.txt",
                                "text/plain",
                                "".getBytes()); // Empty file
                String key = "uploads/test.txt";

                // Act
                CompletableFuture<ResponseEntity<String>> result = s3Controller.uploadFile(file, key);

                // Assert
                assertNotNull(result);
                ResponseEntity<String> response = result.join();
                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                assertEquals("Please select a file to upload", response.getBody());
        }

        @Test
        void uploadFile_ServiceFailure() throws Exception {
                // Arrange
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test.txt",
                                "text/plain",
                                "test content".getBytes());
                String key = "uploads/test.txt";

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Upload failed");

                when(s3Service.processFileUpload(any()))
                                .thenReturn(CompletableFuture.completedFuture(errorResponse));

                // Act
                CompletableFuture<ResponseEntity<String>> result = s3Controller.uploadFile(file, key);

                // Assert
                assertNotNull(result);
                ResponseEntity<String> response = result.join();
                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                String responseBody = response.getBody();
                assertNotNull(responseBody);
                assertTrue(responseBody.contains("Failed to upload file"));
        }

}