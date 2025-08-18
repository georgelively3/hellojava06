# Controller Architecture Migration: From Single Controller to Separated S3 Architecture

## 🎯 Overview
This comprehensive guide documents the complete migration from a monolithic single controller architecture to a clean, separated controller design with dedicated S3 functionality. This migration resolves dependency injection conflicts, improves maintainability, and provides a scalable architecture with full Swagger API documentation.

## 📖 Migration Story

### The Problem
Initially, our Spring Boot application had a single `MainController` that handled both:
- **User CRUD operations** (backed by PostgreSQL/H2)
- **S3 file operations** (upload, list, health checks)

This monolithic approach created several issues:
1. **Dependency Injection Conflicts**: Tests failed due to complex dependency chains
2. **Tight Coupling**: User and S3 functionality were unnecessarily intertwined  
3. **Maintainability Issues**: Single controller became bloated and hard to test
4. **Scalability Problems**: Adding new features required touching the main controller

### The Solution
We implemented a **Clean Architecture Separation Pattern**:
- **`MainController`**: Dedicated to User operations only (`/api/users`)
- **`S3Controller`**: Dedicated to S3 operations only (`/api/s3`)
- **Independent Testing**: Each controller tested in complete isolation
- **Swagger Integration**: Full API documentation for both controllers

## 🏗️ Architecture Before vs After

### Before: Monolithic Controller
```
MainController (/api)
├── User Operations (GET, POST, PUT, DELETE /users)
├── S3 Upload (POST /s3/upload)
├── S3 List (GET /s3/list)
└── S3 Health (GET /s3/health)

Dependencies: UserService + S3Service + S3Client
Issues: Complex dependency injection, test conflicts
```

### After: Separated Architecture
```
MainController (/api/users)           S3Controller (/api/s3)
├── GET /users                       ├── POST /upload  
├── POST /users                      ├── GET /list
├── PUT /users/{id}                  └── GET /health

Dependencies: UserService only       Dependencies: S3Service only
Benefits: Clean isolation, easy testing, independent development
```

## 📋 Step-by-Step Migration Guide

### Phase 1: Add Required Dependencies

First, ensure your `build.gradle` has all necessary dependencies:

```gradle
dependencies {
    // Spring Boot Starters
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    
    // AWS S3
    implementation platform('software.amazon.awssdk:bom:2.21.29')
    implementation 'software.amazon.awssdk:s3'
    
    // Swagger/OpenAPI Documentation
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0'
    
    // Database
    implementation 'com.h2database:h2'
    implementation 'org.flywaydb:flyway-core'
    
    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.testcontainers:junit-jupiter'
}
```

### Phase 2: Create the S3 Service Layer

Create a dedicated S3 service to handle all S3 operations:

**File:** `src/main/java/com/lithespeed/hellojava06/service/S3Service.java`

```java
package com.lithespeed.hellojava06.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private static final String BUCKET_NAME = "lithespeed-test-bucket";
    
    private final S3Client s3Client;
    
    @Autowired
    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }
    
    public String uploadFile(String key, MultipartFile file) throws IOException {
        logger.info("Uploading file with key: {}", key);
        
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            
            String fileUrl = String.format("https://%s.s3.amazonaws.com/%s", BUCKET_NAME, key);
            logger.info("File uploaded successfully: {}", fileUrl);
            return fileUrl;
            
        } catch (S3Exception e) {
            logger.error("Failed to upload file to S3: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }
    
    public List<String> listFiles(String prefix) {
        logger.info("Listing files with prefix: {}", prefix);
        
        try {
            ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder()
                    .bucket(BUCKET_NAME)
                    .prefix(prefix)
                    .build();
            
            ListObjectsV2Response response = s3Client.listObjectsV2(listObjectsRequest);
            
            List<String> files = response.contents().stream()
                    .map(S3Object::key)
                    .collect(Collectors.toList());
            
            logger.info("Found {} files", files.size());
            return files;
            
        } catch (S3Exception e) {
            logger.error("Failed to list files from S3: {}", e.getMessage());
            throw new RuntimeException("Failed to list files from S3", e);
        }
    }
    
    public boolean checkS3Health() {
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(BUCKET_NAME)
                    .build();
            
            s3Client.headBucket(headBucketRequest);
            logger.info("S3 health check passed");
            return true;
            
        } catch (Exception e) {
            logger.warn("S3 health check failed: {}", e.getMessage());
            return false;
        }
    }
}
```

### Phase 3: Create S3 Configuration

Create a configuration class for S3 client setup:

**File:** `src/main/java/com/lithespeed/hellojava06/config/S3Config.java`

```java
package com.lithespeed.hellojava06.config;

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
public class S3Config {
    
    @Bean
    @Primary
    @Profile("localstack")
    public S3Client localStackS3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create("http://localhost:4566"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("localstack", "localstack")))
                .region(Region.US_EAST_1)
                .forcePathStyle(true)
                .build();
    }
    
    @Bean
    @Profile("!localstack")
    public S3Client realS3Client() {
        return S3Client.builder()
                .region(Region.US_EAST_1)
                .build();
    }
}
```

### Phase 4: Refactor MainController (Remove S3 Dependencies)

Transform your monolithic controller into a focused User controller:

**File:** `src/main/java/com/lithespeed/hellojava06/controller/MainController.java`

```java
package com.lithespeed.hellojava06.controller;

import com.lithespeed.hellojava06.entity.User;
import com.lithespeed.hellojava06.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "Operations for managing users")
public class MainController {
    
    private final UserService userService;
    
    @Autowired
    public MainController(UserService userService) {
        this.userService = userService;
    }
    
    @Operation(summary = "Get all users", description = "Retrieve a list of all users")
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User found"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(
            @Parameter(description = "User ID") @PathVariable Long id) {
        Optional<User> user = userService.getUserById(id);
        return user.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }
    
    @Operation(summary = "Create a new user", description = "Create a new user with the provided information")
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User savedUser = userService.saveUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }
    
    @Operation(summary = "Update an existing user", description = "Update user information for the specified ID")
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @Parameter(description = "User ID") @PathVariable Long id, 
            @RequestBody User userDetails) {
        Optional<User> existingUser = userService.getUserById(id);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setName(userDetails.getName());
            user.setEmail(userDetails.getEmail());
            User updatedUser = userService.saveUser(user);
            return ResponseEntity.ok(updatedUser);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @Operation(summary = "Delete a user", description = "Delete the user with the specified ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "User ID") @PathVariable Long id) {
        if (userService.getUserById(id).isPresent()) {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
```

### Phase 5: Create the New S3Controller

Create a dedicated controller for all S3 operations:

**File:** `src/main/java/com/lithespeed/hellojava06/controller/S3Controller.java`

```java
package com.lithespeed.hellojava06.controller;

import com.lithespeed.hellojava06.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/s3")
@Tag(name = "S3 File Operations", description = "Operations for managing files in S3")
public class S3Controller {
    
    private final S3Service s3Service;
    
    @Autowired
    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }
    
    @Operation(summary = "Upload file to S3", description = "Upload a file to S3 storage")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "File uploaded successfully"),
        @ApiResponse(responseCode = "500", description = "Upload failed")
    })
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(
            @Parameter(description = "File to upload") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Custom S3 key (optional)") @RequestParam(value = "key", required = false) String key) {
        
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
    
    @Operation(summary = "List S3 files", description = "List files in S3 with optional prefix filter")
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listFiles(
            @Parameter(description = "Prefix filter (optional)") @RequestParam(value = "prefix", required = false, defaultValue = "") String prefix) {
        
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
    
    @Operation(summary = "S3 Health Check", description = "Check if S3 service is accessible")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkS3Health() {
        boolean isHealthy = s3Service.checkS3Health();
        
        Map<String, Object> response = new HashMap<>();
        response.put("s3_accessible", isHealthy);
        response.put("status", isHealthy ? "UP" : "DOWN");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
}
```

### Phase 6: Create Independent Test Configurations

Create isolated test configurations to prevent dependency injection conflicts:

**File:** `src/test/java/com/lithespeed/hellojava06/config/TestS3Config.java`

```java
package com.lithespeed.hellojava06.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.services.s3.S3Client;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestS3Config {
    
    @Bean
    @Primary
    public S3Client mockS3Client() {
        return mock(S3Client.class);
    }
}
```

### Phase 7: Create Isolated Unit Tests

**File:** `src/test/java/com/lithespeed/hellojava06/controller/S3ControllerTest.java`

```java
package com.lithespeed.hellojava06.controller;

import com.lithespeed.hellojava06.config.TestS3Config;
import com.lithespeed.hellojava06.service.S3Service;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(S3Controller.class)
@Import(TestS3Config.class)
@ActiveProfiles("test")
class S3ControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private S3Service s3Service;
    
    @MockBean
    private S3Client s3Client;
    
    @Test
    void uploadFile_WithValidFile_ShouldReturnSuccess() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test content".getBytes());
        String expectedUrl = "https://lithespeed-test-bucket.s3.amazonaws.com/uploads/test.txt";
        
        when(s3Service.uploadFile(anyString(), any())).thenReturn(expectedUrl);
        
        // When & Then
        mockMvc.perform(multipart("/api/s3/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("File uploaded successfully"))
                .andExpect(jsonPath("$.url").value(expectedUrl));
    }
    
    @Test
    void listFiles_WithPrefix_ShouldReturnFileList() throws Exception {
        // Given
        List<String> files = Arrays.asList("file1.txt", "file2.txt");
        when(s3Service.listFiles(anyString())).thenReturn(files);
        
        // When & Then
        mockMvc.perform(get("/api/s3/list").param("prefix", "uploads/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.files").isArray())
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.prefix").value("uploads/"));
    }
    
    @Test
    void checkS3Health_WhenHealthy_ShouldReturnUp() throws Exception {
        // Given
        when(s3Service.checkS3Health()).thenReturn(true);
        
        // When & Then
        mockMvc.perform(get("/api/s3/health"))
                .andExpected(status().isOk())
                .andExpected(content().contentType(MediaType.APPLICATION_JSON))
                .andExpected(jsonPath("$.s3_accessible").value(true))
                .andExpected(jsonPath("$.status").value("UP"));
    }
}
```

### Phase 8: Update Configuration Files

Update your application configuration to support the new architecture:

**File:** `src/main/resources/application-test.yml`

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  datasource:
    url: jdbc:h2:mem:testdb
    username: sa
    password: 
    driver-class-name: org.h2.Driver
  
flyway:
  enabled: false

logging:
  level:
    com.lithespeed.hellojava06: DEBUG
```

**File:** `src/main/resources/application-localstack.yml`

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  datasource:
    url: jdbc:h2:mem:testdb
    username: sa
    password: 
    driver-class-name: org.h2.Driver

flyway:
  enabled: true

logging:
  level:
    com.lithespeed.hellojava06: INFO
    software.amazon.awssdk: DEBUG
```

### Phase 9: Add Swagger Configuration

Enable comprehensive API documentation:

**File:** `src/main/java/com/lithespeed/hellojava06/config/OpenApiConfig.java`

```java
package com.lithespeed.hellojava06.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HelloJava06 API")
                        .version("1.0.0")
                        .description("Spring Boot application with separated User and S3 controllers")
                        .contact(new Contact()
                                .name("LitheSpeed Team")
                                .email("support@lithespeed.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
```

## 🧪 Testing the Migration

### 1. Run Unit Tests
```bash
# Test each controller independently
./gradlew test --tests="*UserServiceTest*"
./gradlew test --tests="*S3ControllerTest*"

# Run all tests
./gradlew test
```

### 2. Start the Application
```bash
# Start with LocalStack profile for development
SPRING_PROFILES_ACTIVE=localstack ./gradlew bootRun

# Or using PowerShell
$env:SPRING_PROFILES_ACTIVE="localstack"; .\gradlew.bat bootRun
```

### 3. Access Swagger UI
Open your browser and navigate to:
```
http://localhost:8080/swagger-ui/index.html
```

You'll see two separate controller sections:
- **User Management**: All user CRUD operations
- **S3 File Operations**: All S3-related operations

### 4. Test the APIs

**User Operations:**
```bash
# Get all users
curl http://localhost:8080/api/users

# Create a user
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com"}'
```

**S3 Operations:**
```bash
# Check S3 health
curl http://localhost:8080/api/s3/health

# List S3 files
curl http://localhost:8080/api/s3/list

# Upload a file (using form data)
curl -X POST http://localhost:8080/api/s3/upload \
  -F "file=@/path/to/your/file.txt" \
  -F "key=uploads/my-file.txt"
```

## ✅ Migration Benefits Achieved

### 1. **Clean Separation of Concerns**
- User operations completely isolated from S3 operations
- Each controller has a single responsibility
- Independent development and maintenance

### 2. **Resolved Dependency Injection Issues**
- No more test conflicts between User and S3 functionality
- Each controller tested in complete isolation
- Clean dependency graphs

### 3. **Enhanced Maintainability**
- Smaller, focused controllers are easier to understand
- Changes in one area don't affect the other
- Clear API boundaries

### 4. **Improved Scalability**
- Easy to add new controllers for additional features
- Controllers can be deployed independently if needed
- Clear patterns for future development

### 5. **Professional API Documentation**
- Complete Swagger UI integration
- Interactive testing interface
- Comprehensive API documentation

### 6. **Robust Testing Strategy**
- Independent unit tests for each controller
- Proper mocking and isolation
- Reliable test suite

## 🎯 Key Takeaways

1. **Architecture Patterns Matter**: Separating controllers by domain leads to cleaner, more maintainable code
2. **Dependency Injection**: Keeping dependencies focused prevents complex injection conflicts
3. **Testing Isolation**: Independent tests are more reliable and easier to debug
4. **Documentation**: Swagger integration provides professional API documentation
5. **Configuration Management**: Profile-based configurations support different environments

This migration demonstrates how to evolve from a monolithic controller approach to a clean, separated architecture that scales well and maintains high code quality.

## 🚀 Next Steps

Consider these additional improvements:
- **Add input validation** with `@Valid` annotations
- **Implement proper error handling** with `@ControllerAdvice`
- **Add authentication/authorization** for sensitive operations
- **Include metrics and monitoring** with Micrometer
- **Add caching** for frequently accessed data
- **Implement pagination** for large result sets

The separated architecture now provides a solid foundation for these enhancements!
