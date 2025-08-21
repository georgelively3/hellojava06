package com.example.s3demo.controller;

import com.example.s3demo.service.S3Service;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(S3Controller.class)
class S3ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private S3Service s3Service;

    @Test
    void uploadFile_shouldReturn200() throws Exception {
        doNothing().when(s3Service).uploadFile(Mockito.eq("test.txt"), any());

        mockMvc.perform(multipart("/s3/upload")
                .file("file", "Hello World".getBytes())
                .param("key", "test.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string("File uploaded successfully"));
    }

    @Test
    void downloadFile_shouldReturnContent() throws Exception {
        when(s3Service.downloadFile("test.txt"))
                .thenReturn("Hello World".getBytes());

        mockMvc.perform(get("/s3/download/test.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello World"));
    }

    @Test
    void deleteFile_shouldReturn200() throws Exception {
        doNothing().when(s3Service).deleteFile("test.txt");

        mockMvc.perform(delete("/s3/delete/test.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string("File deleted successfully"));
    }
}
