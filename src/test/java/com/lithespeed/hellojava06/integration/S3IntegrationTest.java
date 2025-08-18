package com.lithespeed.hellojava06.integration;

import com.lithespeed.hellojava06.service.S3Service;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("localstack")
class S3IntegrationTest {

    @Container
    static final LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
            .withServices(S3);

    @Autowired
    private S3Service s3Service;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    private static S3Client testS3Client;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("aws.s3.endpoint-url", () -> localstack.getEndpointOverride(S3).toString());
    }

    @BeforeAll
    static void setupS3Client() {
        testS3Client = S3Client.builder()
                .endpointOverride(localstack.getEndpointOverride(S3))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")))
                .region(Region.US_EAST_1)
                .forcePathStyle(true)
                .build();
    }

    @BeforeEach
    void setUp() {
        // Create the test bucket if it doesn't exist
        try {
            testS3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
        } catch (Exception e) {
            testS3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        }
    }

    @Test
    void uploadFile_WithLocalStack_ShouldUploadSuccessfully() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "testfile", 
                "test.txt", 
                "text/plain", 
                "Hello LocalStack!".getBytes()
        );
        String key = "integration-test/test.txt";

        // When
        String result = s3Service.uploadFile(key, file);

        // Then
        assertThat(result).contains(bucketName);
        assertThat(result).contains(key);

        // Verify the file actually exists in LocalStack
        List<String> files = s3Service.listFiles("integration-test/");
        assertThat(files).contains(key);
    }

    @Test
    void listFiles_WithLocalStack_ShouldReturnUploadedFiles() throws Exception {
        // Given - Upload a few test files
        String prefix = "list-test/";
        
        MockMultipartFile file1 = new MockMultipartFile("file1", "file1.txt", "text/plain", "Content 1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file2", "file2.txt", "text/plain", "Content 2".getBytes());
        
        String key1 = prefix + "file1.txt";
        String key2 = prefix + "file2.txt";
        
        s3Service.uploadFile(key1, file1);
        s3Service.uploadFile(key2, file2);

        // When
        List<String> files = s3Service.listFiles(prefix);

        // Then
        assertThat(files).hasSize(2);
        assertThat(files).containsExactlyInAnyOrder(key1, key2);
    }

    @Test
    void listFiles_WithEmptyPrefix_ShouldReturnAllFiles() throws Exception {
        // Given - Upload files with different prefixes
        MockMultipartFile file1 = new MockMultipartFile("file1", "root.txt", "text/plain", "Root content".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file2", "sub.txt", "text/plain", "Sub content".getBytes());
        
        String key1 = "root-file.txt";
        String key2 = "subfolder/sub-file.txt";
        
        s3Service.uploadFile(key1, file1);
        s3Service.uploadFile(key2, file2);

        // When
        List<String> allFiles = s3Service.listFiles("");

        // Then
        assertThat(allFiles).contains(key1, key2);
    }

    @Test
    void uploadFile_WithAutoGeneratedKey_ShouldSucceed() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "autofile", 
                "original.txt", 
                "text/plain", 
                "Auto-generated key test".getBytes()
        );

        // When - Pass null key to trigger auto-generation
        String result = s3Service.uploadFile(null, file);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("original.txt");
        
        // Verify the file was actually uploaded
        List<String> files = s3Service.listFiles("uploads/");
        assertThat(files).hasSize(1);
        assertThat(files.get(0)).contains("original.txt");
    }
}
