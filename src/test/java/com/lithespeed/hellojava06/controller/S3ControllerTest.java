package com.lithespeed.hellojava06.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.lithespeed.hellojava06.service.S3Service;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(S3Controller.class)
@ActiveProfiles("test")
@DisplayName("S3Controller Tests")
class S3ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private S3Service s3Service;

    private MockMultipartFile testFile;

    @BeforeEach
    void setUp() {
        testFile = new MockMultipartFile(
                "file", 
                "test-document.txt", 
                "text/plain", 
                "This is test content for multipart upload".getBytes()
        );
    }

    @Test
    void testUploadFile() throws Exception {
        String fileName = "test.txt";
        doNothing().when(s3Service).uploadFile(fileName);

        mockMvc.perform(post("/s3/upload")
                .param("fileName", fileName)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("Uploaded: " + fileName));

        verify(s3Service, times(1)).uploadFile(fileName);
    }

    @Test
    void testListFiles() throws Exception {
        List<String> mockFiles = Arrays.asList("file1.txt", "file2.txt");
        when(s3Service.listFiles()).thenReturn(mockFiles);

        mockMvc.perform(get("/s3/list")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("file1.txt"))
                .andExpect(jsonPath("$[1]").value("file2.txt"));

        verify(s3Service, times(1)).listFiles();
    }

    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/s3/health")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Should successfully upload multipart file")
    void shouldSuccessfullyUploadMultipartFile() throws Exception {
        // Given
        String expectedEtag = "\"9bb58f26192e4ba00f01e2e7b136bbd8\"";
        when(s3Service.uploadFile(any(MockMultipartFile.class))).thenReturn(expectedEtag);

        // When & Then
        mockMvc.perform(multipart("/s3/upload-file")
                .file(testFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("File uploaded successfully"))
                .andExpect(jsonPath("$.originalFileName").value("test-document.txt"))
                .andExpect(jsonPath("$.contentType").value("text/plain"))
                .andExpect(jsonPath("$.size").value(41))
                .andExpect(jsonPath("$.etag").value(expectedEtag))
                .andExpect(jsonPath("$.key").value("test-document.txt"));
    }

    @Test
    @DisplayName("Should upload multipart file with custom key")
    void shouldUploadMultipartFileWithCustomKey() throws Exception {
        // Given
        String customKey = "documents/my-custom-file.txt";
        String expectedEtag = "\"custom-etag-value\"";
        when(s3Service.uploadFile(any(MockMultipartFile.class), eq(customKey))).thenReturn(expectedEtag);

        // When & Then
        mockMvc.perform(multipart("/s3/upload-file")
                .file(testFile)
                .param("customKey", customKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.key").value(customKey))
                .andExpect(jsonPath("$.etag").value(expectedEtag));
    }

    @Test
    @DisplayName("Should return bad request for empty file")
    void shouldReturnBadRequestForEmptyFile() throws Exception {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", 
                "empty.txt", 
                "text/plain", 
                new byte[0]
        );

        // When & Then
        mockMvc.perform(multipart("/s3/upload-file")
                .file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("File cannot be empty"));
    }

    @Test
    @DisplayName("Should handle service exception gracefully")
    void shouldHandleServiceExceptionGracefully() throws Exception {
        // Given
        when(s3Service.uploadFile(any(MockMultipartFile.class)))
                .thenThrow(new RuntimeException("S3 service unavailable"));

        // When & Then
        mockMvc.perform(multipart("/s3/upload-file")
                .file(testFile))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("Should handle missing file parameter")
    void shouldHandleMissingFileParameter() throws Exception {
        // When & Then - GlobalExceptionHandler returns 500 for missing required parameters
        mockMvc.perform(multipart("/s3/upload-file"))
                .andExpect(status().isInternalServerError());
    }
}
