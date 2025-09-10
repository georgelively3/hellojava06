# AWS S3 Integration with Spring Boot and Kubernetes

A comprehensive guide for implementing AWS S3 integration in Spring Boot applications with Kubernetes deployment.

## Overview

This guide covers the complete implementation of AWS S3 integration using Spring Cloud AWS, from dependency management to Kubernetes deployment. The implementation includes file upload, download, deletion, listing, and existence checking operations.

## Table of Contents

1. [Gradle Dependencies](#gradle-dependencies)
2. [AWS Configuration](#aws-configuration)
3. [S3 Service Implementation](#s3-service-implementation)
4. [S3 Controller Implementation](#s3-controller-implementation)
5. [Application Configuration Files](#application-configuration-files)
6. [Docker Environment Variables](#docker-environment-variables)
7. [Kubernetes Deployment](#kubernetes-deployment)
8. [Testing](#testing)
9. [Security Best Practices](#security-best-practices)

## Gradle Dependencies

### build.gradle

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

java {
    sourceCompatibility = '17'
}

dependencyManagement {
    imports {
        mavenBom "io.awspring.cloud:spring-cloud-aws-dependencies:3.0.3"
    }
}

dependencies {
    // Spring Boot Starters
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    
    // AWS S3 Integration (Recommended approach)
    implementation 'io.awspring.cloud:spring-cloud-aws-starter-s3'
    
    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

**Why Spring Cloud AWS over AWS SDK:**
- Automatic Spring Boot integration and configuration
- Built-in credential management
- Simplified dependency management with BOM
- Better Spring ecosystem integration
- Reduced boilerplate code

## AWS Configuration

### src/main/java/com/lithespeed/hellojava06/config/AwsConfig.java

```java
package com.lithespeed.hellojava06.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                    )
                )
                .build();
    }
}
```

**Key Features:**
- Configurable AWS credentials via environment variables
- Region-specific S3 client configuration
- Spring-managed S3Client bean for dependency injection

## S3 Service Implementation

### src/main/java/com/lithespeed/hellojava06/service/S3Service.java

```java
package com.lithespeed.hellojava06.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class S3Service {

    @Autowired
    private S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public String uploadFile(MultipartFile file, String key) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, 
                software.amazon.awssdk.core.sync.RequestBody.fromInputStream(
                    file.getInputStream(), file.getSize()));

            return "File uploaded successfully: " + key;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    public ResponseInputStream<GetObjectResponse> downloadFile(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        return s3Client.getObject(getObjectRequest);
    }

    public String deleteFile(String key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
        return "File deleted successfully: " + key;
    }

    public List<String> listFiles(String prefix) {
        ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(listObjectsRequest);
        return response.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    public boolean fileExists(String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
}
```

**Service Features:**
- Complete CRUD operations for S3 objects
- Proper error handling and exception management
- Stream-based file operations for memory efficiency
- Configurable bucket name via properties

## S3 Controller Implementation

### src/main/java/com/lithespeed/hellojava06/controller/S3Controller.java

```java
package com.lithespeed.hellojava06.controller;

import com.lithespeed.hellojava06.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.util.List;

@RestController
@RequestMapping("/api/s3")
public class S3Controller {

    @Autowired
    private S3Service s3Service;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("key") String key) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a file to upload");
        }
        
        String result = s3Service.uploadFile(file, key);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/download/{key}")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable String key) {
        ResponseInputStream<GetObjectResponse> s3Object = s3Service.downloadFile(key);
        GetObjectResponse response = s3Object.response();
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(response.contentType()))
                .contentLength(response.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + key + "\"")
                .body(new InputStreamResource(s3Object));
    }

    @DeleteMapping("/delete/{key}")
    public ResponseEntity<String> deleteFile(@PathVariable String key) {
        String result = s3Service.deleteFile(key);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/list")
    public ResponseEntity<List<String>> listFiles(@RequestParam(required = false) String prefix) {
        List<String> files = s3Service.listFiles(prefix != null ? prefix : "");
        return ResponseEntity.ok(files);
    }

    @GetMapping("/exists/{key}")
    public ResponseEntity<Boolean> fileExists(@PathVariable String key) {
        boolean exists = s3Service.fileExists(key);
        return ResponseEntity.ok(exists);
    }
}
```

**Controller Features:**
- RESTful API design with proper HTTP methods
- File upload with validation
- Stream-based file download with proper headers
- List and existence check operations
- Comprehensive error handling

## Application Configuration Files

### src/main/resources/application.yml

```yaml
spring:
  application:
    name: hellojava06
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

aws:
  s3:
    bucket-name: ${S3_BUCKET_NAME:default-bucket}
    region: ${AWS_REGION:us-east-1}
    access-key: ${AWS_ACCESS_KEY_ID:}
    secret-key: ${AWS_SECRET_ACCESS_KEY:}

logging:
  level:
    software.amazon.awssdk: INFO
    com.lithespeed.hellojava06: DEBUG
```

### src/main/resources/application-preprod.yml

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

aws:
  s3:
    bucket-name: ${S3_BUCKET_NAME:preprod-bucket}
    region: ${AWS_REGION:us-east-1}

logging:
  level:
    software.amazon.awssdk: WARN
    com.lithespeed.hellojava06: INFO
```

**Configuration Features:**
- Environment-specific settings
- Configurable file upload limits
- Proper logging configuration
- Environment variable integration

## Docker Environment Variables

### Development Environment

```bash
# Docker run command for development
docker run -d \
  --name s3-app-dev \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e S3_BUCKET_NAME=dev-bucket \
  -e AWS_REGION=us-east-1 \
  -e AWS_ACCESS_KEY_ID=your-dev-access-key \
  -e AWS_SECRET_ACCESS_KEY=your-dev-secret-key \
  your-app-image:latest
```

### Integration Environment

```bash
# Docker run command for integration testing
docker run -d \
  --name s3-app-int \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=preprod \
  -e S3_BUCKET_NAME=integration-bucket \
  -e AWS_REGION=us-east-1 \
  -e AWS_ACCESS_KEY_ID=your-int-access-key \
  -e AWS_SECRET_ACCESS_KEY=your-int-secret-key \
  your-app-image:latest
```

### Docker Compose Example

```yaml
version: '3.8'
services:
  s3-app:
    image: your-app-image:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-dev}
      - S3_BUCKET_NAME=${S3_BUCKET_NAME}
      - AWS_REGION=${AWS_REGION:-us-east-1}
      - AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
      - AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
    restart: unless-stopped
```

## Kubernetes Deployment

### s3-configmap.yaml

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: s3-config
  namespace: default
data:
  SPRING_PROFILES_ACTIVE: "preprod"
  AWS_REGION: "us-east-1"
  S3_BUCKET_NAME: "production-s3-bucket"
---
apiVersion: v1
kind: Secret
metadata:
  name: s3-secrets
  namespace: default
type: Opaque
data:
  # Base64 encoded values
  AWS_ACCESS_KEY_ID: <base64-encoded-access-key>
  AWS_SECRET_ACCESS_KEY: <base64-encoded-secret-key>
```

### deployment.yaml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: s3-app-deployment
  namespace: default
  labels:
    app: s3-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: s3-app
  template:
    metadata:
      labels:
        app: s3-app
    spec:
      containers:
      - name: s3-app
        image: your-registry/s3-app:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          valueFrom:
            configMapKeyRef:
              name: s3-config
              key: SPRING_PROFILES_ACTIVE
        - name: AWS_REGION
          valueFrom:
            configMapKeyRef:
              name: s3-config
              key: AWS_REGION
        - name: S3_BUCKET_NAME
          valueFrom:
            configMapKeyRef:
              name: s3-config
              key: S3_BUCKET_NAME
        - name: AWS_ACCESS_KEY_ID
          valueFrom:
            secretKeyRef:
              name: s3-secrets
              key: AWS_ACCESS_KEY_ID
        - name: AWS_SECRET_ACCESS_KEY
          valueFrom:
            secretKeyRef:
              name: s3-secrets
              key: AWS_SECRET_ACCESS_KEY
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /api/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /api/health
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: s3-app-service
  namespace: default
spec:
  selector:
    app: s3-app
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: ClusterIP
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: s3-app-ingress
  namespace: default
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  rules:
  - host: s3-app.yourdomain.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: s3-app-service
            port:
              number: 80
```

### Helm Chart Structure

```
helm/
├── Chart.yaml
├── values.yaml
├── templates/
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── configmap.yaml
│   ├── secret.yaml
│   └── ingress.yaml
```

### values.yaml (Helm)

```yaml
replicaCount: 3

image:
  repository: your-registry/s3-app
  tag: latest
  pullPolicy: IfNotPresent

service:
  type: ClusterIP
  port: 80
  targetPort: 8080

ingress:
  enabled: true
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
  hosts:
    - host: s3-app.yourdomain.com
      paths:
        - path: /
          pathType: Prefix

config:
  springProfile: preprod
  awsRegion: us-east-1
  s3BucketName: production-s3-bucket

secrets:
  awsAccessKeyId: ""
  awsSecretAccessKey: ""

resources:
  requests:
    memory: "256Mi"
    cpu: "250m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```

## Testing

### Unit Test Example

```java
@ExtendWith(MockitoExtension.class)
class S3ServiceTest {
    
    @Mock
    private S3Client s3Client;
    
    @InjectMocks
    private S3Service s3Service;
    
    @Test
    void uploadFile_Success() {
        // Test implementation
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.txt", "text/plain", "test content".getBytes());
        
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(PutObjectResponse.builder().build());
        
        String result = s3Service.uploadFile(file, "test-key");
        
        assertThat(result).contains("File uploaded successfully");
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }
}
```

### Integration Test

```java
@SpringBootTest
@Testcontainers
class S3IntegrationTest {
    
    @Container
    static LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
        .withServices(LocalStackContainer.Service.S3);
    
    @Test
    void testS3Operations() {
        // Integration test with LocalStack
    }
}
```

## Security Best Practices

### IAM Policy (Least Privilege)

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::your-bucket-name/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket"
      ],
      "Resource": "arn:aws:s3:::your-bucket-name"
    }
  ]
}
```

### Security Recommendations

1. **Use IAM Roles in production** instead of access keys when possible
2. **Encrypt sensitive environment variables** in Kubernetes secrets
3. **Implement proper file validation** before upload
4. **Set up bucket policies** to restrict access
5. **Use VPC endpoints** for private S3 access
6. **Enable S3 server-side encryption**
7. **Implement rate limiting** on upload endpoints

### Production Checklist

- [ ] IAM roles configured with minimal permissions
- [ ] S3 bucket policies implemented
- [ ] SSL/TLS encryption enabled
- [ ] File type validation implemented
- [ ] Rate limiting configured
- [ ] Monitoring and logging enabled
- [ ] Error handling and retry logic implemented
- [ ] Resource limits configured in Kubernetes
- [ ] Health checks implemented
- [ ] Backup and disaster recovery planned

This comprehensive guide provides everything needed to implement AWS S3 integration in Spring Boot applications with proper Kubernetes deployment practices.
