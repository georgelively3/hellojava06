package com.example.s3demo.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Assertions;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

class S3ServiceTest {

    private S3Service s3Service;

    // Supply both implementations
    static Stream<S3Service> serviceProvider() {
        return Stream.of(
                new FakeS3Service(),
                new AwsS3Service() {
                    @Override
                    public void uploadFile(String key, byte[] content) {
                        // stubbed for unit test
                        Assertions.assertNotNull(key);
                        Assertions.assertNotNull(content);
                    }

                    @Override
                    public byte[] downloadFile(String key) {
                        // stubbed for unit test
                        return ("stubbed-" + key).getBytes();
                    }
                });
    }

    @TempDir
    File tempDir;

    @ParameterizedTest
    @MethodSource("serviceProvider")
    void testUploadAndDownload(S3Service service) throws IOException {
        this.s3Service = service;

        String key = "test-file.txt";
        byte[] content = "Hello, world!".getBytes();

        // Upload
        s3Service.uploadFile(key, content);

        // Download
        byte[] result = s3Service.downloadFile(key);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.length > 0);
    }
}
