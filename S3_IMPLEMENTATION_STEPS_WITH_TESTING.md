# S3 Implementation Steps with LocalStack Testing

## Overview

This guide provides a complete, step-by-step approach to implementing AWS S3 integration in a Spring Boot application with comprehensive testing using LocalStack service virtualization. This approach eliminates the need for real AWS credentials during testing while providing true AWS API compatibility.

## Architecture Benefits

- **Real AWS API Compatibility**: LocalStack provides actual S3 behavior, not HTTP mocks
- **No Complex Mocking**: Uses real S3Client against containerized S3 service
- **Container Isolation**: Each test gets a fresh LocalStack instance
- **Spring Boot Integration**: Proper dependency injection with test configurations
- **Production-Ready**: Same S3Service code runs in tests and production

## Prerequisites

- Java 17+
- Docker installed and running
- Gradle 8.5+
- Spring Boot 3.2+

## Step 1: Dependencies

Add the following dependencies to your `build.gradle`:

```gradle
dependencies {
    // AWS S3
    implementation platform('software.amazon.awssdk:bom:2.21.29')
    implementation 'software.amazon.awssdk:s3'
    
    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.testcontainers:junit-jupiter'
    
    // LocalStack for AWS service testing
    testImplementation 'org.testcontainers:localstack:1.19.3'
    
    // Karate for API testing
    testImplementation 'com.intuit.karate:karate-junit5:1.4.1'
}
```

## Step 2: Production S3Service Implementation

Create `src/main/java/com/lithespeed/hellojava06/service/S3Service.java`:

```java
@Service
@Profile("!localstack") // Exclude when using test profiles
public class S3Service {

    private final S3Client s3Client;
    private final String bucketName;

    // Constructor for testing with injected S3Client
    public S3Service(S3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    // Production constructor with configuration
    @Autowired
    public S3Service(@Value("${aws.s3.region}") String region,
            @Value("${aws.s3.bucket-name}") String bucketName,
            @Value("${aws.s3.use-iam-role:true}") boolean useIamRole) {
        this.bucketName = bucketName;
        
        S3ClientBuilder builder = S3Client.builder().region(Region.of(region));
        
        if (useIamRole) {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        
        this.s3Client = builder.build();
    }

    public void uploadFile(String fileName) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build();
        s3Client.putObject(putRequest, RequestBody.fromString("dummy content"));
    }

    public String uploadFileWithContent(String key, String content) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        return s3Client.putObject(putRequest, RequestBody.fromString(content)).eTag();
    }

    public String downloadFile(String key) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        return s3Client.getObjectAsBytes(getRequest).asUtf8String();
    }

    public List<String> listFiles() {
        ListObjectsRequest request = ListObjectsRequest.builder()
                .bucket(bucketName)
                .build();
        return s3Client.listObjects(request).contents()
                .stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    public boolean deleteFile(String key) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.deleteObject(deleteRequest);
            return true;
        } catch (S3Exception e) {
            return false;
        }
    }
}
```

## Step 3: LocalStack Test Configuration

Create `src/test/java/com/lithespeed/hellojava06/config/LocalStackS3Config.java`:

```java
@TestConfiguration
@Profile("localstack")
public class LocalStackS3Config {

    @Bean("localStackS3Client")
    @Primary
    public S3Client localStackS3Client() {
        String endpoint = System.getProperty("aws.s3.endpoint", "http://localhost:4566");
        String region = System.getProperty("aws.s3.region", "us-east-1");
        String accessKey = System.getProperty("aws.accessKeyId", "test");
        String secretKey = System.getProperty("aws.secretAccessKey", "test");
        
        return S3Client.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .forcePathStyle(true) // Required for LocalStack
                .build();
    }

    @Bean("localStackS3Service")
    @Primary
    public S3Service localStackS3Service() {
        String bucketName = System.getProperty("aws.s3.bucket-name", "test-bucket");
        return new S3Service(localStackS3Client(), bucketName);
    }
    
    public static void configureProperties(DynamicPropertyRegistry registry, LocalStackContainer localStack) {
        registry.add("aws.s3.endpoint", () -> localStack.getEndpointOverride(LocalStackContainer.Service.S3).toString());
        registry.add("aws.s3.region", localStack::getRegion);
        registry.add("aws.accessKeyId", localStack::getAccessKey);
        registry.add("aws.secretAccessKey", localStack::getSecretKey);
        registry.add("aws.s3.bucket-name", () -> "test-bucket");
    }
}
```

## Step 4: Unit Tests (No Docker Required)

Create basic unit tests that work without LocalStack for CI/CD environments where Docker may not be available.

Create `src/test/java/com/lithespeed/hellojava06/service/S3ServiceTest.java`:

```java
@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;
    
    @Mock
    private PutObjectResponse putObjectResponse;
    
    private S3Service s3Service;
    private static final String BUCKET_NAME = "test-bucket";

    @BeforeEach
    void setUp() {
        s3Service = new S3Service(s3Client, BUCKET_NAME);
    }

    @Test
    void shouldUploadFileWithContent() {
        // Given
        String fileName = "test-file.txt";
        String content = "test content";
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(putObjectResponse);
        when(putObjectResponse.eTag()).thenReturn("test-etag");

        // When
        String result = s3Service.uploadFileWithContent(fileName, content);

        // Then
        assertEquals("test-etag", result);
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }
}
```

## Step 5: LocalStack Integration Tests

Create `src/test/java/com/lithespeed/hellojava06/integration/LocalStackBasicTest.java`:

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("localstack")
@Import(LocalStackS3Config.class)
@DisplayName("LocalStack Basic Integration Test")
class LocalStackBasicTest {

    private static final String BUCKET_NAME = "test-bucket";
    
    @Container
    static LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.0"))
            .withServices(LocalStackContainer.Service.S3);

    @Autowired
    private S3Service s3Service;
    
    @Autowired
    private S3Client s3Client;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        LocalStackS3Config.configureProperties(registry, localStack);
    }

    @BeforeEach
    void setUp() {
        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
        } catch (Exception e) {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(BUCKET_NAME).build());
        }
    }

    @Test
    @DisplayName("Should be able to upload and list files")
    void shouldBeAbleToUploadAndListFiles() {
        // Given
        String fileName = "basic-test-file.txt";

        // When
        s3Service.uploadFile(fileName);
        List<String> files = s3Service.listFiles();

        // Then
        assertThat(files).contains(fileName);
    }
}
```

## Step 6: Comprehensive S3Service Integration Tests

Create `src/test/java/com/lithespeed/hellojava06/integration/S3ServiceLocalStackTest.java`:

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("localstack")
@Import(LocalStackS3Config.class)
@DisplayName("S3Service Integration Tests with LocalStack")
class S3ServiceLocalStackTest {

    // Full implementation with comprehensive test coverage
    // Tests upload, download, list, delete operations
    // Each test method follows Given-When-Then pattern
}
```

## Step 7: Karate API Tests with LocalStack

Create `src/test/java/com/lithespeed/hellojava06/karate/S3KarateTestRunner.java`:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("localstack")
@Import(LocalStackS3Config.class)
public class S3KarateTestRunner {

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.0"))
            .withServices(LocalStackContainer.Service.S3);

    @LocalServerPort
    private int serverPort;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        LocalStackS3Config.configureProperties(registry, localStack);
    }

    @Karate.Test
    Karate testS3Api() {
        System.setProperty("karate.server.port", String.valueOf(serverPort));
        return Karate.run("s3-api").relativeTo(getClass());
    }
}
```

## Step 8: Build Configuration

Configure your `build.gradle` for different test types:

```gradle
// Unit tests only (no Docker required)
task unitTest(type: Test) {
    description = 'Run unit tests only (no Docker required)'
    group = 'verification'
    useJUnitPlatform()
    
    include '**/*Test.class'
    exclude '**/integration/**'
    exclude '**/karate/**'
}

// LocalStack integration tests
task localStackTest(type: Test) {
    useJUnitPlatform()
    include '**/integration/**'
    
    jvmArgs '--add-opens', 'java.base/java.lang=ALL-UNNAMED'
    jvmArgs '--add-opens', 'java.base/java.util=ALL-UNNAMED'
    systemProperty 'file.encoding', 'UTF-8'
}

// Integration tests including Karate
task integrationTest(type: Test) {
    description = 'Run integration tests that require Docker'
    group = 'verification'
    useJUnitPlatform()
    
    include '**/karate/**'
    include '**/integration/**'
    
    systemProperty 'spring.profiles.active', 'test'
    shouldRunAfter test
}
```

## Step 9: Running Tests

### Unit Tests (No Docker Required)
```bash
./gradlew unitTest
```

### LocalStack Integration Tests
```bash
./gradlew localStackTest
```

### Full Integration Tests (Including Karate)
```bash
./gradlew integrationTest
```

### Regular Build (Unit Tests Only)
```bash
./gradlew build
```

## Step 10: Production Configuration

For production deployment, configure your `application.yml`:

```yaml
aws:
  s3:
    region: us-east-1
    bucket-name: ${S3_BUCKET_NAME:my-production-bucket}
    use-iam-role: true
    connection-timeout: 10000
    socket-timeout: 30000
    max-connections: 25
```

For Kubernetes with IRSA (IAM Roles for Service Accounts):

```yaml
aws:
  s3:
    region: us-east-1
    bucket-name: ${S3_BUCKET_NAME}
    use-iam-role: true  # Will use ServiceAccount IAM role
```

## Key Benefits of This Approach

1. **Real S3 Behavior**: LocalStack provides actual AWS S3 API responses
2. **No Mocking Complexity**: No need for complex HTTP response mocking
3. **Container Isolation**: Each test runs with a fresh S3 instance
4. **Production Parity**: Same S3Service code runs in tests and production
5. **Flexible Testing**: Unit tests for CI/CD, integration tests for comprehensive validation
6. **Easy Debugging**: Real S3 operations make debugging straightforward

## Troubleshooting

### Docker Not Available
- Unit tests will still pass without Docker
- Use `./gradlew unitTest` for CI/CD environments without Docker

### LocalStack Container Issues
- Ensure Docker is running
- Check container logs: `docker logs <container-id>`
- Verify LocalStack version compatibility

### AWS SDK Configuration
- Ensure `forcePathStyle(true)` is set for LocalStack
- Verify endpoint override is configured correctly
- Check credentials are properly injected

## Migration from WireMock

If migrating from WireMock:
1. WireMock approach is preserved in `feature/wiremock-integration` branch
2. LocalStack provides better AWS API fidelity
3. No complex XML response matching required
4. Easier to maintain and debug

This implementation provides a robust, production-ready S3 integration with comprehensive testing capabilities.
