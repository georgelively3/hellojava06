package com.lithespeed.hellojava06.controller;

import com.lithespeed.hellojava06.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = S3Controller.class)
@ActiveProfiles("test")
class S3ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private S3Service s3Service;

    // Mock the S3Client bean to prevent dependency injection issues
    @MockBean
    private software.amazon.awssdk.services.s3.S3Client s3Client;

    @BeforeEach
    void setUp() {
        // Test setup can be added here if needed
    }

    @Test
    void uploadFile_WithValidFile_ShouldReturnSuccessResponse() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "test content".getBytes());
        String expectedUrl = "https://bucket.s3.region.amazonaws.com/test.txt";
        when(s3Service.uploadFile(anyString(), any())).thenReturn(expectedUrl);

        // When & Then
        mockMvc.perform(multipart("/api/s3/upload")
                .file(file)
                .param("key", "test.txt"))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("File uploaded successfully"))
                .andExpect(jsonPath("$.key").value("test.txt"))
                .andExpect(jsonPath("$.url").value(expectedUrl));
    }

    @Test
    void listFiles_ShouldReturnFileList() throws Exception {
        // Given
        List<String> files = Arrays.asList("file1.txt", "file2.txt");
        when(s3Service.listFiles("")).thenReturn(files);

        // When & Then
        mockMvc.perform(get("/api/s3/list"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.files.length()").value(2))
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void healthCheck_ShouldReturnHealthStatus() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/s3/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("S3"));
    }
}
