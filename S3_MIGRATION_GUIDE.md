# S3 Functionality Migration Guide

## Overview
This guide contains all the necessary elements to migrate the S3 functionality (upload and list operations) from this Spring Boot application to another existing Spring Boot application that already has PostgreSQL implementation.

## 📋 Required Dependencies

Add these dependencies to your `build.gradle`:

```gradle
dependencies {
    // AWS S3
    implementation platform('software.amazon.awssdk:bom:2.21.29')
    implementation 'software.amazon.awssdk:s3'
}
```

Or for `pom.xml` (Maven):

```xml
<properties>
    <aws-sdk.version>2.21.29</aws-sdk.version>
</properties>

<dependencies>
    <!-- AWS S3 -->
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>bom</artifactId>
        <version>${aws-sdk.version}</version>
        <type>pom</type>
        <scope>import</scope>
    </dependency>
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>s3</artifactId>
    </dependency>
</dependencies>
```

## 📁 Files to Copy

### 1. Service Layer
Copy the entire S3Service class:

**File:** `src/main/java/[your-package]/service/S3Service.java`

```java
package [your-package].service;  // Update package name

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class S3Service {

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    private S3Client s3Client;

    @PostConstruct
    public void initializeS3Client() {
        if (accessKey != null && !accessKey.isEmpty() && secretKey != null && !secretKey.isEmpty()) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            this.s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();
        } else {
            // Use default credential provider chain (for production with IAM roles)
            this.s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .build();
        }
    }

    public String uploadFile(String key, MultipartFile file) throws IOException {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    public List<String> listFiles(String prefix) {
        try {
            ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(listObjectsRequest);

            return response.contents().stream()
                    .map(S3Object::key)
                    .collect(Collectors.toList());
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to list files from S3", e);
        }
    }
}
```

### 2. Controller Endpoints

Add these S3 endpoints to your existing controller (or create a new S3Controller):

**Option A: Add to existing controller**
```java
// Add this import
import [your-package].service.S3Service;

// Add this field and update constructor
private final S3Service s3Service;

public YourController(/* existing dependencies */, S3Service s3Service) {
    // existing assignments
    this.s3Service = s3Service;
}

// Add these endpoints
// ========== S3 OPERATIONS ==========

@PostMapping("/s3/upload")
public ResponseEntity<Map<String, String>> uploadFile(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "key", required = false) String key) {

    try {
        if (key == null || key.isEmpty()) {
            key = "uploads/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        }

        String fileUrl = s3Service.uploadFile(key, file);

        Map<String, String> response = new HashMap<>();
        response.put("message", "File uploaded successfully");
        response.put("key", key);
        response.put("url", fileUrl);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (IOException e) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "Failed to upload file: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

@GetMapping("/s3/list")
public ResponseEntity<Map<String, Object>> listFiles(
        @RequestParam(value = "prefix", required = false, defaultValue = "") String prefix) {

    try {
        List<String> files = s3Service.listFiles(prefix);

        Map<String, Object> response = new HashMap<>();
        response.put("files", files);
        response.put("count", files.size());
        response.put("prefix", prefix);

        return ResponseEntity.ok(response);
    } catch (RuntimeException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Failed to list files: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
```

**Option B: Create dedicated S3Controller**
```java
package [your-package].controller;

import [your-package].service.S3Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class S3Controller {

    private final S3Service s3Service;

    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @PostMapping("/s3/upload")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "key", required = false) String key) {

        try {
            if (key == null || key.isEmpty()) {
                key = "uploads/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            }

            String fileUrl = s3Service.uploadFile(key, file);

            Map<String, String> response = new HashMap<>();
            response.put("message", "File uploaded successfully");
            response.put("key", key);
            response.put("url", fileUrl);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/s3/list")
    public ResponseEntity<Map<String, Object>> listFiles(
            @RequestParam(value = "prefix", required = false, defaultValue = "") String prefix) {

        try {
            List<String> files = s3Service.listFiles(prefix);

            Map<String, Object> response = new HashMap<>();
            response.put("files", files);
            response.put("count", files.size());
            response.put("prefix", prefix);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to list files: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
```

## ⚙️ Configuration

Add these configurations to your `application.yml`:

```yaml
# AWS S3 Configuration
aws:
  s3:
    bucket-name: ${S3_BUCKET_NAME:your-bucket-name}
    region: ${AWS_REGION:us-east-1}
    access-key: ${AWS_ACCESS_KEY:}
    secret-key: ${AWS_SECRET_KEY:}
```

Or for `application.properties`:

```properties
# AWS S3 Configuration
aws.s3.bucket-name=${S3_BUCKET_NAME:your-bucket-name}
aws.s3.region=${AWS_REGION:us-east-1}
aws.s3.access-key=${AWS_ACCESS_KEY:}
aws.s3.secret-key=${AWS_SECRET_KEY:}
```

## 🔧 Required Imports

Make sure your controller has these imports:

```java
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
```

## 🧪 Tests (Optional but Recommended)

### S3Service Tests
**File:** `src/test/java/[your-package]/service/S3ServiceTest.java`

```java
package [your-package].service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private S3Service s3Service;

    private final String bucketName = "test-bucket";
    private final String region = "us-east-1";
    private final String testKey = "test-file.txt";
    private final byte[] testContent = "test content".getBytes();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3Service, "bucketName", bucketName);
        ReflectionTestUtils.setField(s3Service, "region", region);
        ReflectionTestUtils.setField(s3Service, "accessKey", "test-access-key");
        ReflectionTestUtils.setField(s3Service, "secretKey", "test-secret-key");
        ReflectionTestUtils.setField(s3Service, "s3Client", s3Client);
    }

    @Test
    void uploadFile_WithValidFile_ShouldReturnFileUrl() throws IOException {
        // Given
        InputStream inputStream = new ByteArrayInputStream(testContent);
        when(multipartFile.getInputStream()).thenReturn(inputStream);
        when(multipartFile.getSize()).thenReturn((long) testContent.length);
        when(multipartFile.getContentType()).thenReturn("text/plain");

        // When
        String result = s3Service.uploadFile(testKey, multipartFile);

        // Then
        assertThat(result).isEqualTo("https://test-bucket.s3.us-east-1.amazonaws.com/test-file.txt");
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void listFiles_WithPrefix_ShouldReturnFileList() {
        // Given
        String prefix = "uploads/";
        List<S3Object> s3Objects = Arrays.asList(
                S3Object.builder().key("uploads/file1.txt").build(),
                S3Object.builder().key("uploads/file2.txt").build());

        ListObjectsV2Response response = ListObjectsV2Response.builder()
                .contents(s3Objects)
                .build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(response);

        // When
        List<String> result = s3Service.listFiles(prefix);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder("uploads/file1.txt", "uploads/file2.txt");
        verify(s3Client, times(1)).listObjectsV2(any(ListObjectsV2Request.class));
    }
}
```

## 🌍 Environment Variables

Set these environment variables:

```bash
# Required
S3_BUCKET_NAME=your-actual-bucket-name
AWS_REGION=us-east-1

# For development with explicit credentials (not recommended for production)
AWS_ACCESS_KEY=your-access-key
AWS_SECRET_KEY=your-secret-key
```

**For production, use IAM roles instead of explicit access keys.**

## 📡 API Endpoints

Once integrated, you'll have these endpoints available:

### POST /api/s3/upload
Upload a file to S3
- **Content-Type:** `multipart/form-data`
- **Parameters:**
  - `file` (required): The file to upload
  - `key` (optional): Custom S3 key, auto-generated if not provided
- **Response:** JSON with message, key, and file URL

### GET /api/s3/list
List S3 objects
- **Parameters:**
  - `prefix` (optional): Filter objects by prefix
- **Response:** JSON with files array, count, and prefix

## ✅ Migration Checklist

- [ ] Add AWS SDK dependencies to build file
- [ ] Copy S3Service.java and update package name
- [ ] Add S3 endpoints to controller (or create new S3Controller)
- [ ] Add AWS S3 configuration to application.yml/properties
- [ ] Set environment variables for S3 bucket and credentials
- [ ] Update import statements in controller
- [ ] (Optional) Copy and adapt S3 tests
- [ ] Test the endpoints

## 🔒 Security Notes

1. **Never commit AWS credentials to version control**
2. **Use IAM roles in production environments**  
3. **Configure proper S3 bucket policies**
4. **Consider adding file type/size validation**
5. **Add authentication/authorization if needed**

That's it! Your S3 functionality should now be fully portable to any Spring Boot application with PostgreSQL.
