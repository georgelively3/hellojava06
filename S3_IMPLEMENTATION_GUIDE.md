# S3 Implementation Guide

A comprehensive step-by-step guide for implementing Amazon S3 integration in a Spring Boot application from scratch.

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Dependencies Setup](#dependencies-setup)
3. [Configuration](#configuration)
4. [Service Layer Implementation](#service-layer-implementation)
5. [Controller Layer Implementation](#controller-layer-implementation)
6. [API Documentation with Swagger](#api-documentation-with-swagger)
7. [Testing Setup](#testing-setup)
8. [Karate API Tests](#karate-api-tests)
9. [Best Practices](#best-practices)

## Prerequisites

- Java 17 or higher
- Spring Boot 3.x
- Gradle 8.x
- AWS Account (for production) or LocalStack (for local development)

## Dependencies Setup

### 1. Add AWS SDK Dependencies to `build.gradle`

```gradle
dependencies {
    // AWS SDK for S3
    implementation 'software.amazon.awssdk:s3:2.21.29'
    implementation 'software.amazon.awssdk:auth:2.21.29'
    implementation 'software.amazon.awssdk:regions:2.21.29'
    
    // Spring Boot starters
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    
    // Swagger/OpenAPI for API documentation
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0'
    
    // For multipart file uploads
    implementation 'commons-fileupload:commons-fileupload:1.5'
    
    // Testing dependencies
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'com.intuit.karate:karate-junit5:1.4.1'
}
```

### 2. Configure Application Properties

Create `src/main/resources/application.yml`:

```yaml
aws:
  s3:
    endpoint: ${AWS_S3_ENDPOINT:https://s3.amazonaws.com}
    bucket-name: ${AWS_S3_BUCKET:your-bucket-name}
    region: ${AWS_REGION:us-east-1}
    access-key: ${AWS_ACCESS_KEY:your-access-key}
    secret-key: ${AWS_SECRET_KEY:your-secret-key}

spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```

Create `src/test/resources/application-test.yml` for testing:

```yaml
aws:
  s3:
    endpoint: http://localhost:9999  # Dummy endpoint for testing
    bucket-name: test-bucket
    region: us-east-1
    access-key: test-access-key
    secret-key: test-secret-key
```

## Configuration

### 1. Create S3 Configuration Class

Create `src/main/java/com/yourpackage/config/S3Config.java`:

```java
package com.yourpackage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class S3Config {

    @Value("${aws.s3.endpoint}")
    private String endpoint;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        
        return S3Client.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}
```

### 2. Create Test Configuration

Create `src/test/java/com/yourpackage/config/TestS3Config.java`:

```java
package com.yourpackage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
@Profile("test")
public class TestS3Config {

    @Value("${aws.s3.endpoint}")
    private String endpoint;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    @Bean
    @Primary
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        
        return S3Client.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}
```

## Service Layer Implementation

### Create S3Service

Create `src/main/java/com/yourpackage/service/S3Service.java`:

```java
package com.yourpackage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class S3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);
    
    private final S3Client s3Client;
    
    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Upload a file to S3
     */
    public String uploadFile(String fileName, MultipartFile file) throws IOException {
        logger.info("Uploading file: {} to bucket: {}", fileName, bucketName);
        
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
            
            String fileUrl = String.format("https://%s.s3.amazonaws.com/%s", bucketName, fileName);
            logger.info("File uploaded successfully: {}", fileUrl);
            
            return fileUrl;
            
        } catch (S3Exception e) {
            logger.error("Failed to upload file to S3: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    /**
     * List files in S3 bucket with optional prefix
     */
    public List<String> listFiles(String prefix) {
        logger.info("Listing files in bucket: {} with prefix: {}", bucketName, prefix);
        
        try {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucketName);
            
            if (prefix != null && !prefix.isEmpty()) {
                requestBuilder.prefix(prefix);
            }
            
            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());
            
            List<String> fileNames = response.contents().stream()
                    .map(S3Object::key)
                    .collect(Collectors.toList());
            
            logger.info("Found {} files", fileNames.size());
            return fileNames;
            
        } catch (S3Exception e) {
            logger.error("Failed to list files from S3: {}", e.getMessage());
            throw new RuntimeException("Failed to list files from S3", e);
        }
    }

    /**
     * Delete a file from S3
     */
    public void deleteFile(String fileName) {
        logger.info("Deleting file: {} from bucket: {}", fileName, bucketName);
        
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            s3Client.deleteObject(request);
            logger.info("File deleted successfully: {}", fileName);
            
        } catch (S3Exception e) {
            logger.error("Failed to delete file from S3: {}", e.getMessage());
            throw new RuntimeException("Failed to delete file from S3", e);
        }
    }

    /**
     * Check if S3 service is available
     */
    public boolean isHealthy() {
        try {
            HeadBucketRequest request = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            
            s3Client.headBucket(request);
            return true;
            
        } catch (Exception e) {
            logger.warn("S3 health check failed: {}", e.getMessage());
            return false;
        }
    }
}
```

## Controller Layer Implementation

### Create S3Controller

Create `src/main/java/com/yourpackage/controller/S3Controller.java`:

```java
package com.yourpackage.controller;

import com.yourpackage.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/s3")
@Tag(name = "S3 File Management", description = "APIs for S3 file operations")
public class S3Controller {

    private final S3Service s3Service;

    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    /**
     * Upload file endpoint
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file to S3", description = "Upload a file to S3 bucket with optional custom key")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file or parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error during upload")
    })
    public ResponseEntity<Map<String, String>> uploadFile(
            @Parameter(description = "File to upload", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Custom key/path for the file in S3", required = false)
            @RequestParam("fileName") String fileName) {
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File is empty"));
            }

            String fileUrl = s3Service.uploadFile(fileName, file);
            
            return ResponseEntity.ok(Map.of(
                    "message", "File uploaded successfully",
                    "fileName", fileName,
                    "fileUrl", fileUrl
            ));
            
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        }
    }

    /**
     * List files endpoint
     */
    @GetMapping("/files")
    @Operation(summary = "List S3 files", description = "List files in the S3 bucket with optional prefix filter")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Files listed successfully"),
            @ApiResponse(responseCode = "500", description = "Error accessing S3 bucket")
    })
    public ResponseEntity<Map<String, Object>> listFiles(
            @Parameter(description = "Prefix to filter files", required = false)
            @RequestParam(value = "prefix", required = false) String prefix) {
        
        try {
            List<String> files = s3Service.listFiles(prefix);
            
            return ResponseEntity.ok(Map.of(
                    "files", files,
                    "count", files.size()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to list files: " + e.getMessage()));
        }
    }

    /**
     * Delete file endpoint
     */
    @DeleteMapping("/files/{fileName}")
    @Operation(summary = "Delete file from S3", description = "Delete a file from S3 bucket by filename")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File deleted successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Error deleting file")
    })
    public ResponseEntity<Map<String, String>> deleteFile(
            @Parameter(description = "Name of file to delete")
            @PathVariable String fileName) {
        try {
            s3Service.deleteFile(fileName);
            
            return ResponseEntity.ok(Map.of(
                    "message", "File deleted successfully",
                    "fileName", fileName
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to delete file: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "S3 service health check", description = "Check if S3 service is accessible and healthy")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "S3 service is healthy")
    })
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        boolean isHealthy = s3Service.isHealthy();
        
        response.put("status", isHealthy ? "UP" : "DOWN");
        response.put("service", "S3");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
}
```

## API Documentation with Swagger

### 1. OpenAPI Configuration

Create `src/main/java/com/yourpackage/config/OpenApiConfig.java`:

```java
package com.yourpackage.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        Server localServer = new Server()
                .url("http://localhost:" + serverPort)
                .description("Local development server");

        Contact contact = new Contact()
                .name("API Team")
                .email("support@yourcompany.com")
                .url("https://github.com/yourusername/your-repo");

        License license = new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0");

        Info info = new Info()
                .title("S3 Integration API")
                .version("1.0.0")
                .description("A comprehensive Spring Boot application demonstrating S3 file operations with modern testing approaches using Karate.")
                .contact(contact)
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}
```

### 2. Controller Annotations

Add comprehensive Swagger annotations to your controllers:

```java
@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "APIs for managing users")
public class UserController {

    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieve a list of all users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved users",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class)))
    })
    public ResponseEntity<List<User>> getAllUsers() {
        // Implementation
    }

    @PostMapping
    @Operation(summary = "Create new user", description = "Create a new user with the provided information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<User> createUser(
            @Parameter(description = "User information") @Valid @RequestBody User user) {
        // Implementation
    }
}
```

### 3. Accessing Swagger UI

Once your application is running, access the Swagger UI at:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

### 4. Key Annotations

| Annotation | Purpose | Usage |
|------------|---------|-------|
| `@Tag` | Groups endpoints in Swagger UI | Class level |
| `@Operation` | Describes the endpoint operation | Method level |
| `@ApiResponses` | Documents possible responses | Method level |
| `@Parameter` | Describes method parameters | Parameter level |
| `@Schema` | Defines data models | Class/Field level |

## Testing Setup

### 1. Create Unit Tests

Create `src/test/java/com/yourpackage/service/S3ServiceTest.java`:

```java
package com.yourpackage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        s3Service = new S3Service(s3Client);
        ReflectionTestUtils.setField(s3Service, "bucketName", "test-bucket");
    }

    @Test
    void testUploadFile() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello World".getBytes()
        );
        
        when(s3Client.putObject(any(PutObjectRequest.class), any())).thenReturn(null);

        // When
        String result = s3Service.uploadFile("test.txt", file);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("test-bucket"));
        assertTrue(result.contains("test.txt"));
        verify(s3Client).putObject(any(PutObjectRequest.class), any());
    }

    @Test
    void testListFiles() {
        // Given
        ListObjectsV2Response response = ListObjectsV2Response.builder()
                .contents(S3Object.builder().key("file1.txt").build(),
                         S3Object.builder().key("file2.txt").build())
                .build();
        
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(response);

        // When
        var files = s3Service.listFiles(null);

        // Then
        assertEquals(2, files.size());
        assertTrue(files.contains("file1.txt"));
        assertTrue(files.contains("file2.txt"));
    }

    @Test
    void testDeleteFile() {
        // Given
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(null);

        // When & Then
        assertDoesNotThrow(() -> s3Service.deleteFile("test.txt"));
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }
}
```

### 2. Create Controller Tests

Create `src/test/java/com/yourpackage/controller/S3ControllerTest.java`:

```java
package com.yourpackage.controller;

import com.yourpackage.service.S3Service;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(S3Controller.class)
@ActiveProfiles("test")
class S3ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private S3Service s3Service;

    @Test
    void testHealthCheck() throws Exception {
        when(s3Service.isHealthy()).thenReturn(true);

        mockMvc.perform(get("/api/s3/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("S3"));
    }

    @Test
    void testUploadFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello World".getBytes()
        );

        when(s3Service.uploadFile(eq("test.txt"), any())).thenReturn("https://test-bucket.s3.amazonaws.com/test.txt");

        mockMvc.perform(multipart("/api/s3/upload")
                        .file(file)
                        .param("fileName", "test.txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("File uploaded successfully"))
                .andExpected(jsonPath("$.fileName").value("test.txt"));
    }

    @Test
    void testListFiles() throws Exception {
        when(s3Service.listFiles(null)).thenReturn(List.of("file1.txt", "file2.txt"));

        mockMvc.perform(get("/api/s3/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files").isArray())
                .andExpect(jsonPath("$.count").value(2));
    }
}
```

## Karate API Tests

### 1. Create Karate Test Runner

Create `src/test/java/com/yourpackage/karate/KarateTestRunner.java`:

```java
package com.yourpackage.karate;

import com.intuit.karate.junit5.Karate;
import com.yourpackage.config.TestS3Config;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestS3Config.class)
public class KarateTestRunner {

    @LocalServerPort
    private int serverPort;

    @Karate.Test
    Karate testS3() {
        System.setProperty("karate.server.port", String.valueOf(serverPort));
        return Karate.run("s3-api").relativeTo(getClass());
    }
}
```

### 2. Create Karate Feature File

Create `src/test/resources/com/yourpackage/karate/s3-api.feature`:

```gherkin
Feature: S3 API Integration Tests

  Background:
    * url 'http://localhost:' + karate.properties['karate.server.port']
    * def baseUrl = 'http://localhost:' + karate.properties['karate.server.port']

  Scenario: S3 Health Check
    Given url baseUrl + '/api/s3/health'
    When method GET
    Then status 200
    And match response.status == 'UP' || response.status == 'DOWN'
    And match response.service == 'S3'
    And match response.timestamp == '#number'

  Scenario: Upload File
    Given url baseUrl + '/api/s3/upload'
    And multipart field fileName = 'test-file.txt'
    And multipart file file = { read: 'classpath:test-file.txt', contentType: 'text/plain' }
    When method POST
    Then status 200
    And match response.message == 'File uploaded successfully'
    And match response.fileName == 'test-file.txt'
    And match response.fileUrl contains 'test-file.txt'

  Scenario: List Files
    Given url baseUrl + '/api/s3/files'
    When method GET
    Then status 200
    And match response.files == '#array'
    And match response.count == '#number'

  Scenario: List Files with Prefix
    Given url baseUrl + '/api/s3/files'
    And param prefix = 'test/'
    When method GET
    Then status 200
    And match response.files == '#array'
    And match response.count == '#number'

  Scenario: Delete File
    Given url baseUrl + '/api/s3/files/test-file.txt'
    When method DELETE
    Then status 200
    And match response.message == 'File deleted successfully'
    And match response.fileName == 'test-file.txt'

  Scenario: Error Handling - Upload Empty File
    Given url baseUrl + '/api/s3/upload'
    And multipart field fileName = 'empty.txt'
    And multipart file file = { value: '', contentType: 'text/plain' }
    When method POST
    Then status 400
    And match response.error contains 'File is empty'
```

### 3. Create Test File Resource

Create `src/test/resources/test-file.txt`:

```text
This is a test file for S3 upload testing.
It contains sample content to verify file upload functionality.
```

## Best Practices

### 1. Security Considerations

- **Never hardcode AWS credentials** in your source code
- Use environment variables or AWS IAM roles for credential management
- Implement proper input validation for file uploads
- Set appropriate file size limits
- Validate file types and extensions

### 2. Error Handling

- Always wrap S3 operations in try-catch blocks
- Provide meaningful error messages to clients
- Log errors for debugging purposes
- Implement circuit breaker pattern for resilience

### 3. Performance Optimization

- Use multipart uploads for large files
- Implement connection pooling for S3Client
- Consider using presigned URLs for direct uploads
- Cache frequently accessed file lists

### 4. Testing Strategy

- **Unit Tests**: Test service logic with mocked S3Client
- **Integration Tests**: Use Karate for API endpoint testing
- **Test Configurations**: Use separate profiles for test environments
- **Mock Services**: Use TestS3Config for lightweight testing

### 5. Configuration Management

- Use Spring profiles for different environments
- Externalize all configuration through properties files
- Use meaningful property names and defaults
- Document all configuration options

### 6. Monitoring and Logging

- Add structured logging for all S3 operations
- Monitor upload/download success rates
- Track file sizes and operation latencies
- Set up alerts for S3 service failures

## Deployment Considerations

### 1. Local Development with LocalStack

For local development, you can use LocalStack to simulate S3:

```yaml
# docker-compose.yml
version: '3.8'
services:
  localstack:
    image: localstack/localstack:latest
    ports:
      - "4566:4566"
    environment:
      - SERVICES=s3
      - DEBUG=1
      - DATA_DIR=/tmp/localstack/data
    volumes:
      - "/tmp/localstack:/tmp/localstack"
```

### 2. Production Configuration

```yaml
# application-prod.yml
aws:
  s3:
    endpoint: https://s3.amazonaws.com
    bucket-name: ${AWS_S3_BUCKET}
    region: ${AWS_REGION}
    # Use IAM roles instead of access keys in production

logging:
  level:
    com.yourpackage.service.S3Service: INFO
    software.amazon.awssdk.services.s3: WARN
```

## Conclusion

This guide provides a complete implementation of S3 integration in a Spring Boot application with:

- ✅ Proper configuration management
- ✅ Service layer with error handling
- ✅ RESTful API endpoints with Swagger documentation
- ✅ Comprehensive API documentation with OpenAPI 3.0
- ✅ Interactive Swagger UI for testing
- ✅ Comprehensive testing with Karate
- ✅ Security best practices
- ✅ Production-ready considerations

### Quick Start Checklist

1. **Dependencies**: Add AWS SDK and SpringDoc OpenAPI dependencies
2. **Configuration**: Set up S3 configuration classes for main and test profiles
3. **Service Layer**: Implement S3Service with upload, list, delete operations
4. **Controllers**: Create RESTful endpoints with Swagger annotations
5. **API Documentation**: Configure OpenAPI and access Swagger UI
6. **Testing**: Implement unit tests and Karate API tests
7. **Security**: Use environment variables for credentials
8. **Deployment**: Configure for different environments

### Next Steps

- **Production Deployment**: Use IAM roles instead of access keys
- **Monitoring**: Add metrics and health checks
- **Performance**: Implement connection pooling and caching
- **Security**: Add authentication and authorization
- **Advanced Features**: Implement presigned URLs, multipart uploads

Follow these patterns and you'll have a robust, well-documented, testable S3 implementation that's ready for production use!
