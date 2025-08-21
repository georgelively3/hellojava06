package com.example.hellojava.controller;

import com.example.hellojava.service.S3Service;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(S3Controller.class)
class S3ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private S3Service s3Service;

    @Test
    void testUploadFile() throws Exception {
        String fileName = "test.txt";

        doNothing().when(s3Service).uploadFile(fileName);

        mockMvc.perform(post("/s3/upload")
                .param("filename", fileName)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("File uploaded successfully: " + fileName));

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
}
