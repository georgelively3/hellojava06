package com.lithespeed.hellojava06.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3AsyncClient s3AsyncClient;

    private S3Service s3Service;
    private final String bucketName = "test-bucket";

    @BeforeEach
    void setUp() {
        s3Service = new S3Service(s3AsyncClient, bucketName);
    }

    @Test
    void constructor_ShouldCreateService() {
        // Act & Assert
        assertNotNull(s3Service);
    }

    @Test
    void debugCredentials_ShouldNotThrow() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            var result = s3Service.debugCredentials();
            assertNotNull(result);
        });
    }
}
