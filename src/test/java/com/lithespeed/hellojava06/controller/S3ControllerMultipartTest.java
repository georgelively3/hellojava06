package com.lithespeed.hellojava06.controller;

import com.lithespeed.hellojava06.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for S3Controller multipart file upload functionality
 */
@WebMvcTest(S3Controller.class)
@ActiveProfiles("test")
@DisplayName("S3Controller Multipart File Upload Tests")
class S3ControllerMultipartTest {

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
                "This is test content for multipart upload".getBytes());
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
                new byte[0]);

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
        // When & Then - GlobalExceptionHandler returns 500 for missing required
        // parameters
        mockMvc.perform(multipart("/s3/upload-file"))
                .andExpect(status().isInternalServerError());
    }
}
