package com.example.hellojava.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FakeS3ServiceTest {

    private FakeS3Service fakeS3Service;

    @BeforeEach
    void setUp() {
        fakeS3Service = new FakeS3Service();
    }

    @Test
    void uploadFileAddsFile() {
        fakeS3Service.uploadFile("file1.txt");
        List<String> files = fakeS3Service.listFiles();

        assertEquals(1, files.size());
        assertEquals("file1.txt", files.get(0));
    }

    @Test
    void listFilesReturnsAllUploadedFiles() {
        fakeS3Service.uploadFile("file1.txt");
        fakeS3Service.uploadFile("file2.txt");

        List<String> files = fakeS3Service.listFiles();

        assertEquals(2, files.size());
        assertTrue(files.contains("file1.txt"));
        assertTrue(files.contains("file2.txt"));
    }
}
