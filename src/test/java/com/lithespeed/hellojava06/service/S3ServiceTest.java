package com.lithespeed.hellojava06.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;


import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3AsyncClient s3AsyncClient;

    @Mock
    private MultipartFile multipartFile;

    private S3Service s3Service;
    private final String bucketName = "test-bucket";

    @BeforeEach
    void setUp() throws Exception {
        s3Service = new S3Service(s3AsyncClient);
        // Use reflection to set the bucket name since we removed the test constructor
        java.lang.reflect.Field bucketField = S3Service.class.getDeclaredField("bucketName");
        bucketField.setAccessible(true);
        bucketField.set(s3Service, bucketName);
    }

    @Test
    void constructor_ShouldCreateService() {
        // Act & Assert
        assertNotNull(s3Service);
    }

    // Upload File Tests
    @Test
    void uploadFileAsync_Success() throws IOException {
        // Arrange
        String entityType = "uploads";
        String entityId = "user123";
        byte[] content = "test file content".getBytes();
        
        when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
        when(multipartFile.getContentType()).thenReturn("text/plain");
        when(multipartFile.getSize()).thenReturn((long) content.length);
        when(multipartFile.getBytes()).thenReturn(content);

        PutObjectResponse response = PutObjectResponse.builder()
                .eTag("test-etag-12345")
                .build();
        
        when(s3AsyncClient.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        // Act
        CompletableFuture<String> result = s3Service.uploadFileAsync(entityType, entityId, multipartFile);

        // Assert
        assertNotNull(result);
        String key = result.join();
        
        assertNotNull(key);
        assertTrue(key.startsWith("uploads/user123/"));
        assertTrue(key.endsWith(".txt"));
        
        // Verify S3 client was called correctly
        verify(s3AsyncClient).putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class));
    }

    @Test
    void uploadFileAsync_EmptyFile() throws IOException {
        // Arrange
        String entityType = "uploads";
        String entityId = "user123";
        byte[] emptyContent = new byte[0];
        
        when(multipartFile.getOriginalFilename()).thenReturn("empty.txt");
        when(multipartFile.getContentType()).thenReturn("text/plain");
        when(multipartFile.getSize()).thenReturn(0L);
        when(multipartFile.getBytes()).thenReturn(emptyContent);

        PutObjectResponse response = PutObjectResponse.builder()
                .eTag("empty-file-etag")
                .build();
        
        when(s3AsyncClient.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        // Act
        CompletableFuture<String> result = s3Service.uploadFileAsync(entityType, entityId, multipartFile);

        // Assert - Empty files are now allowed to be uploaded
        assertNotNull(result);
        String key = result.join();
        assertNotNull(key);
        assertTrue(key.startsWith("uploads/user123/"));
        assertTrue(key.endsWith(".txt"));
        
        verify(s3AsyncClient).putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class));
    }

    @Test
    void uploadFileAsync_NoFileExtension() throws IOException {
        // Arrange
        String entityType = "uploads";
        String entityId = "user123";
        byte[] content = "test file content".getBytes();
        
        when(multipartFile.getOriginalFilename()).thenReturn("testfile"); // No extension
        when(multipartFile.getContentType()).thenReturn("application/octet-stream");
        when(multipartFile.getSize()).thenReturn((long) content.length);
        when(multipartFile.getBytes()).thenReturn(content);

        PutObjectResponse response = PutObjectResponse.builder()
                .eTag("test-etag-12345")
                .build();
        
        when(s3AsyncClient.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        // Act
        CompletableFuture<String> result = s3Service.uploadFileAsync(entityType, entityId, multipartFile);

        // Assert
        assertNotNull(result);
        String key = result.join();
        
        assertNotNull(key);
        assertTrue(key.startsWith("uploads/user123/"));
        // Should not have extension since original didn't have one
        assertFalse(key.endsWith("."));
        
        verify(s3AsyncClient).putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class));
    }

    @Test
    void uploadFileAsync_NullOriginalFilename() throws IOException {
        // Arrange
        String entityType = "uploads";
        String entityId = "user123";
        byte[] content = "test file content".getBytes();
        
        when(multipartFile.getOriginalFilename()).thenReturn(null);
        when(multipartFile.getContentType()).thenReturn("application/octet-stream");
        when(multipartFile.getSize()).thenReturn((long) content.length);
        when(multipartFile.getBytes()).thenReturn(content);

        PutObjectResponse response = PutObjectResponse.builder()
                .eTag("test-etag-12345")
                .build();
        
        when(s3AsyncClient.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        // Act
        CompletableFuture<String> result = s3Service.uploadFileAsync(entityType, entityId, multipartFile);

        // Assert
        assertNotNull(result);
        String key = result.join();
        
        assertNotNull(key);
        assertTrue(key.startsWith("uploads/user123/"));
        
        verify(s3AsyncClient).putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class));
    }

    @Test
    void uploadFileAsync_IOExceptionOnGetBytes() throws IOException {
        // Arrange
        String entityType = "uploads";
        String entityId = "user123";
        
        when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
        when(multipartFile.getContentType()).thenReturn("text/plain");
        when(multipartFile.getSize()).thenReturn(100L);
        when(multipartFile.getBytes()).thenThrow(new IOException("Cannot read file"));

        // Act & Assert
        CompletableFuture<String> result = s3Service.uploadFileAsync(entityType, entityId, multipartFile);
        
        assertThrows(RuntimeException.class, result::join);
        verify(s3AsyncClient, never()).putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class));
    }

    @Test
    void uploadFileAsync_S3Exception() throws IOException {
        // Arrange
        String entityType = "uploads";
        String entityId = "user123";
        byte[] content = "test file content".getBytes();
        
        when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
        when(multipartFile.getContentType()).thenReturn("text/plain");
        when(multipartFile.getSize()).thenReturn((long) content.length);
        when(multipartFile.getBytes()).thenReturn(content);

        CompletableFuture<PutObjectResponse> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(S3Exception.builder()
                .message("Access denied")
                .statusCode(403)
                .build());
        
        when(s3AsyncClient.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
                .thenReturn(failedFuture);

        // Act & Assert
        CompletableFuture<String> result = s3Service.uploadFileAsync(entityType, entityId, multipartFile);
        
        assertThrows(RuntimeException.class, result::join);
        verify(s3AsyncClient).putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class));
    }

    // List Files Tests
    @Test
    void listFilesAsync_Success() {
        // Arrange
        S3Object obj1 = S3Object.builder().key("uploads/file1.txt").build();
        S3Object obj2 = S3Object.builder().key("uploads/file2.jpg").build();
        S3Object obj3 = S3Object.builder().key("documents/file3.pdf").build();
        
        ListObjectsV2Response response = ListObjectsV2Response.builder()
                .contents(Arrays.asList(obj1, obj2, obj3))
                .build();
        
        when(s3AsyncClient.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        // Act
        CompletableFuture<List<String>> result = s3Service.listFilesAsync();

        // Assert
        assertNotNull(result);
        List<String> files = result.join();
        
        assertNotNull(files);
        assertEquals(3, files.size());
        assertTrue(files.contains("uploads/file1.txt"));
        assertTrue(files.contains("uploads/file2.jpg"));
        assertTrue(files.contains("documents/file3.pdf"));
        
        verify(s3AsyncClient).listObjectsV2(any(ListObjectsV2Request.class));
    }

    @Test
    void listFilesAsync_EmptyBucket() {
        // Arrange
        ListObjectsV2Response response = ListObjectsV2Response.builder()
                .contents(Arrays.asList()) // Empty list
                .build();
        
        when(s3AsyncClient.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        // Act
        CompletableFuture<List<String>> result = s3Service.listFilesAsync();

        // Assert
        assertNotNull(result);
        List<String> files = result.join();
        
        assertNotNull(files);
        assertTrue(files.isEmpty());
        
        verify(s3AsyncClient).listObjectsV2(any(ListObjectsV2Request.class));
    }

    @Test
    void listFilesAsync_S3Exception() {
        // Arrange
        CompletableFuture<ListObjectsV2Response> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(S3Exception.builder()
                .message("Bucket not found")
                .statusCode(404)
                .build());
        
        when(s3AsyncClient.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(failedFuture);

        // Act & Assert
        CompletableFuture<List<String>> result = s3Service.listFilesAsync();
        
        assertThrows(RuntimeException.class, result::join);
        verify(s3AsyncClient).listObjectsV2(any(ListObjectsV2Request.class));
    }

    @Test
    void listFilesAsync_VerifyBucketNameUsed() {
        // Arrange
        ListObjectsV2Response response = ListObjectsV2Response.builder()
                .contents(Arrays.asList())
                .build();
        
        when(s3AsyncClient.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        // Act
        s3Service.listFilesAsync().join();

        // Assert - Verify the correct bucket name was used
        verify(s3AsyncClient).listObjectsV2(argThat((ListObjectsV2Request request) -> 
            bucketName.equals(request.bucket())
        ));
    }

    // Business Logic Tests
    @Test
    void processFileUpload_Success() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", 
                "test.txt", 
                "text/plain", 
                "test content".getBytes());

        PutObjectResponse putResponse = PutObjectResponse.builder().build();
        when(s3AsyncClient.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
                .thenReturn(CompletableFuture.completedFuture(putResponse));

        // Act
        CompletableFuture<Map<String, Object>> result = s3Service.processFileUpload(file);

        // Assert
        assertNotNull(result);
        Map<String, Object> response = result.join();
        
        assertEquals(true, response.get("success"));
        assertEquals("File uploaded successfully", response.get("message"));
        assertEquals("test.txt", response.get("fileName"));
        assertEquals("text/plain", response.get("contentType"));
        assertEquals(12L, response.get("size")); // "test content".getBytes().length
        assertNotNull(response.get("etag"));
    }

    @Test
    void processFileUpload_Failure() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", 
                "test.txt", 
                "text/plain", 
                "test content".getBytes());

        CompletableFuture<PutObjectResponse> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("S3 upload failed"));
        
        when(s3AsyncClient.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
                .thenReturn(failedFuture);

        // Act
        CompletableFuture<Map<String, Object>> result = s3Service.processFileUpload(file);

        // Assert
        assertNotNull(result);
        Map<String, Object> response = result.join();
        
        assertEquals(false, response.get("success"));
        assertEquals("upload file to S3", response.get("operation"));
        assertNotNull(response.get("message"));
        assertNotNull(response.get("timestamp"));
    }

    @Test
    void processFileList_Success() throws Exception {
        // Arrange
        List<S3Object> s3Objects = Arrays.asList(
                S3Object.builder().key("file1.txt").build(),
                S3Object.builder().key("file2.txt").build(),
                S3Object.builder().key("file3.jpg").build()
        );
        
        ListObjectsV2Response listResponse = ListObjectsV2Response.builder()
                .contents(s3Objects)
                .build();
                
        when(s3AsyncClient.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(CompletableFuture.completedFuture(listResponse));

        // Act
        CompletableFuture<Map<String, Object>> result = s3Service.processFileList();

        // Assert
        assertNotNull(result);
        Map<String, Object> response = result.join();
        
        assertEquals(true, response.get("success"));
        assertEquals(3, response.get("count"));
        
        @SuppressWarnings("unchecked")
        List<String> files = (List<String>) response.get("files");
        assertEquals(3, files.size());
        assertTrue(files.contains("file1.txt"));
        assertTrue(files.contains("file2.txt"));
        assertTrue(files.contains("file3.jpg"));
    }

    @Test
    void processFileList_Failure() throws Exception {
        // Arrange
        CompletableFuture<ListObjectsV2Response> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("S3 list failed"));
        
        when(s3AsyncClient.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(failedFuture);

        // Act
        CompletableFuture<Map<String, Object>> result = s3Service.processFileList();

        // Assert
        assertNotNull(result);
        Map<String, Object> response = result.join();
        
        assertEquals(false, response.get("success"));
        assertEquals("list files from S3", response.get("operation"));
        assertNotNull(response.get("message"));
        assertNotNull(response.get("timestamp"));
    }

    @Test
    void generateFileId_ShouldReturnUniqueId() {
        // Act
        String id1 = s3Service.generateFileId();
        String id2 = s3Service.generateFileId();

        // Assert
        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals(id1, id2); // Should be unique
        assertTrue(id1.length() > 0);
        assertTrue(id2.length() > 0);
    }

    @Test
    void createErrorResponse_ShouldCreateDetailedError() {
        // Arrange
        Exception testException = new RuntimeException("Test error message");
        
        // Act
        Map<String, Object> response = s3Service.createErrorResponse("test operation", testException, "test context");

        // Assert
        assertNotNull(response);
        assertEquals(false, response.get("success"));
        assertEquals("test operation", response.get("operation"));
        assertEquals("RuntimeException", response.get("exceptionType"));
        assertEquals("Test error message", response.get("message"));
        assertEquals("test context", response.get("context"));
        assertNotNull(response.get("timestamp"));
        assertNotNull(response.get("stackTrace"));
    }
}
