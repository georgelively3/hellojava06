# Control-M Integration Implementation Guide

This guide provides step-by-step instructions for implementing Control-M job management integration in a Spring Boot application, including service virtualization with Mountebank for development and testing.

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Implementation Steps](#implementation-steps)
4. [Configuration](#configuration)
5. [Service Virtualization with Mountebank](#service-virtualization-with-mountebank)
6. [Testing](#testing)
7. [Production Deployment](#production-deployment)
8. [Troubleshooting](#troubleshooting)

## Overview

This implementation provides a complete Control-M integration solution with:
- **REST API endpoints** for job management
- **Service layer separation** following Spring best practices
- **External API integration** via RestTemplate
- **Service virtualization** using Mountebank for development/testing
- **Comprehensive testing** with unit and integration tests

## Architecture

### Service Layer Pattern
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│  ControlM       │    │  ControlM        │    │  Control-M      │
│  Controller     │───▶│  Service         │───▶│  External API   │
│  (HTTP Layer)   │    │  (Business Logic)│    │  (Mountebank)   │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

### Key Components
- **ControlMController**: Thin HTTP layer handling REST endpoints
- **ControlMService**: Business logic for Control-M integration
- **RestTemplate**: HTTP client for external API calls
- **Mountebank**: Service virtualization for Control-M API

## Implementation Steps

### Step 1: Dependencies

Add required dependencies to `build.gradle`:

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'io.springfox:springfox-boot-starter:3.0.0' // For Swagger/OpenAPI
    
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'com.intuit.karate:karate-junit5:1.4.1' // For integration tests
}
```

### Step 2: Configuration

#### Application Properties (`application.yml`)
```yaml
# Control-M API Configuration
control-m:
  api:
    base-url: ${CONTROL_M_API_URL:http://localhost:2525}

# Spring Boot Configuration
spring:
  application:
    name: your-app-name
```

#### RestTemplate Configuration
Create `src/main/java/.../config/RestTemplateConfig.java`:
```java
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

### Step 3: Service Layer Implementation

#### Create ControlMService
Create `src/main/java/.../service/ControlMService.java`:

```java
@Service
public class ControlMService {
    
    private static final Logger logger = LoggerFactory.getLogger(ControlMService.class);
    
    private final RestTemplate restTemplate;
    private final String controlMApiBaseUrl;
    private final Map<String, JobExecution> jobExecutions = new ConcurrentHashMap<>();
    private final Map<String, List<String>> jobLogs = new ConcurrentHashMap<>();

    public ControlMService(RestTemplate restTemplate, 
                          @Value("${control-m.api.base-url}") String controlMApiBaseUrl) {
        this.restTemplate = restTemplate;
        this.controlMApiBaseUrl = controlMApiBaseUrl;
    }

    public JobExecutionResult startJob(String jobName, String customJobId, Map<String, Object> parameters) {
        // Implementation details...
    }

    public JobStatusResult getJobStatus(String executionId) {
        // Implementation details...
    }

    public JobCancellationResult cancelJob(String executionId) {
        // Implementation details...
    }

    // Additional business methods...
    
    // Inner classes for result objects and exceptions...
}
```

#### Key Service Methods
1. **startJob()**: Submits jobs to Control-M API and tracks locally
2. **getJobStatus()**: Retrieves job status from Control-M
3. **cancelJob()**: Cancels running jobs
4. **listJobs()**: Lists all job executions with filtering
5. **getJobLogs()**: Retrieves job execution logs
6. **startBatchJobs()**: Handles bulk job submissions

### Step 4: Controller Layer Implementation

#### Create ControlMController
Create `src/main/java/.../controller/ControlMController.java`:

```java
@RestController
@RequestMapping("/control-m")
@Tag(name = "Control-M Integration", description = "Job scheduling and automation endpoints")
public class ControlMController {

    private static final Logger logger = LoggerFactory.getLogger(ControlMController.class);
    
    private final ControlMService controlMService;
    private final String controlMApiBaseUrl;

    public ControlMController(ControlMService controlMService, 
                             @Value("${control-m.api.base-url}") String controlMApiBaseUrl) {
        this.controlMService = controlMService;
        this.controlMApiBaseUrl = controlMApiBaseUrl;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Control-M Integration",
            "timestamp", Instant.now().toString(),
            "controlMApiUrl", controlMApiBaseUrl
        ));
    }

    @PostMapping("/jobs/start")
    public ResponseEntity<Map<String, Object>> startJob(
            @RequestParam String jobName,
            @RequestParam(required = false) String jobId,
            @RequestBody(required = false) Map<String, Object> parameters) {
        // Delegate to service and handle exceptions...
    }

    // Additional endpoint methods...
}
```

#### REST Endpoints
- `GET /control-m/health` - Service health check
- `POST /control-m/jobs/start` - Start a new job
- `GET /control-m/jobs/{executionId}/status` - Get job status
- `DELETE /control-m/jobs/{executionId}` - Cancel job
- `GET /control-m/jobs` - List all jobs
- `GET /control-m/jobs/{executionId}/logs` - Get job logs
- `POST /control-m/batch/start` - Start batch jobs

## Service Virtualization with Mountebank

### Installation and Setup

#### Option 1: NPM Installation
```bash
npm install -g mountebank
```

#### Option 2: Docker (Recommended)
```bash
# Pull Mountebank image
docker pull bbyars/mountebank:2.8.2
```

### Mountebank Configuration

#### Directory Structure
```
src/test/resources/mountebank/
├── imposters.json           # Master configuration
├── control-m-submit.json    # Job submission stubs
├── control-m-status.json    # Job status stubs
└── control-m-cancel.json    # Job cancellation stubs
```

#### Master Configuration (`imposters.json`)
```json
{
  "imposters": [
    {
      "port": 2525,
      "protocol": "http",
      "name": "Control-M API Mock Service",
      "stubs": [
        {
          "predicates": [
            {
              "equals": {
                "method": "POST",
                "path": "/control-m/jobs/submit"
              }
            }
          ],
          "responses": [
            {
              "is": {
                "statusCode": 200,
                "headers": {
                  "Content-Type": "application/json"
                },
                "body": {
                  "controlMJobId": "CTM_GJ_${timestamp}",
                  "status": "SUBMITTED",
                  "scheduledTime": "2025-01-15T14:00:00Z",
                  "estimatedDuration": "5 minutes"
                }
              },
              "_behaviors": {
                "decorate": "function(request, response) { response.body.controlMJobId = response.body.controlMJobId.replace('${timestamp}', Date.now()); response.body.jobName = request.body.jobName; }"
              }
            }
          ]
        }
      ]
    }
  ]
}
```

### Running Mountebank

#### Local Development
```bash
# Start Mountebank with configuration
mb start --port 2525 --configfile src/test/resources/mountebank/imposters.json

# In another terminal, start your application
./gradlew bootRun

# Run tests
./gradlew test
```

#### Docker Approach
```bash
# Start Mountebank container
docker run -d --name mountebank-control-m \
  -p 2525:2525 \
  -v $(pwd)/src/test/resources/mountebank:/config \
  bbyars/mountebank:2.8.2 \
  mb start --port 2525 --configfile /config/imposters.json

# Start application
./gradlew bootRun

# Stop container when done
docker stop mountebank-control-m && docker rm mountebank-control-m
```

#### Gradle Integration (Recommended)
Add to `build.gradle`:

```gradle
task startMountebank(type: Exec) {
    description = 'Start Mountebank with Control-M stubs'
    commandLine 'docker', 'run', '-d', '--name', 'mountebank-control-m',
                '-p', '2525:2525',
                '-v', "${projectDir}/src/test/resources/mountebank:/config",
                'bbyars/mountebank:2.8.2',
                'mb', 'start', '--port', '2525', '--configfile', '/config/imposters.json'
}

task stopMountebank(type: Exec) {
    description = 'Stop Mountebank container'
    commandLine 'docker', 'stop', 'mountebank-control-m'
    finalizedBy 'removeMountebank'
}

task removeMountebank(type: Exec) {
    description = 'Remove Mountebank container'
    commandLine 'docker', 'rm', 'mountebank-control-m'
}

task intTest(type: Test) {
    description = 'Run integration tests with Mountebank'
    dependsOn startMountebank
    finalizedBy stopMountebank
    
    useJUnitPlatform()
    include '**/*IntegrationTest*'
    
    systemProperty 'control-m.api.base-url', 'http://localhost:2525'
}
```

## Testing

### Unit Tests

#### Controller Tests with Service Mocking
```java
@SpringBootTest(properties = {
    "control-m.api.base-url=http://localhost:2525"
})
@AutoConfigureMockMvc
class ControlMControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ControlMService controlMService;

    @Test
    void startJob_WithValidRequest_ShouldReturnSuccess() throws Exception {
        // Given
        JobExecutionResult mockResult = new JobExecutionResult(
            "exec-123", "TestJob", "STARTED", "2025-01-15T14:00:00Z",
            "Job started successfully", "CTM_123", "5 minutes"
        );
        
        when(controlMService.startJob(eq("TestJob"), isNull(), any()))
            .thenReturn(mockResult);

        // When & Then
        mockMvc.perform(post("/control-m/jobs/start")
                .param("jobName", "TestJob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value("exec-123"))
                .andExpect(jsonPath("$.jobName").value("TestJob"))
                .andExpect(jsonPath("$.status").value("STARTED"));
    }
}
```

### Integration Tests with Karate

#### Create Karate Test (`control-m-api.feature`)
```gherkin
Feature: Control-M API Integration with Mountebank

Background:
  * url 'http://localhost:8080'

Scenario: Health Check - Control-M Integration Service
  Given path '/control-m/health'
  When method GET
  Then status 200
  And match response.status == 'UP'
  And match response.service == 'Control-M Integration'

Scenario: Start Job - ETL Job Type
  Given path '/control-m/jobs/start'
  And param jobName = 'ETL_DAILY_LOAD'
  When method POST
  Then status 200
  And match response.executionId == '#uuid'
  And match response.jobName == 'ETL_DAILY_LOAD'
  And match response.status == 'STARTED'
```

### Test Configuration

#### Test Properties (`application-test.yml`)
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  
control-m:
  api:
    base-url: http://localhost:2525

logging:
  level:
    com.yourpackage: DEBUG
```

## Configuration

### Environment-Specific Settings

#### Development
```yaml
control-m:
  api:
    base-url: http://localhost:2525  # Mountebank
```

#### Test
```yaml
control-m:
  api:
    base-url: http://localhost:2525  # Mountebank
```

#### Production
```yaml
control-m:
  api:
    base-url: https://control-m-api.yourdomain.com
    # Add authentication configuration as needed
```

### Environment Variables
- `CONTROL_M_API_URL`: Override Control-M API base URL
- `CONTROL_M_USERNAME`: Production API username (if needed)
- `CONTROL_M_PASSWORD`: Production API password (if needed)

## Production Deployment

### Pre-Production Checklist
- [ ] Replace Mountebank URL with actual Control-M API endpoint
- [ ] Configure proper authentication (API keys, certificates, etc.)
- [ ] Set up monitoring and alerting for Control-M integration
- [ ] Remove Mountebank dependencies from production builds
- [ ] Configure proper logging levels
- [ ] Set up health checks for Control-M connectivity

### Security Considerations
1. **API Authentication**: Use secure authentication methods (OAuth2, API keys)
2. **Network Security**: Ensure secure communication with Control-M servers
3. **Credential Management**: Use secure credential storage (Vault, AWS Secrets Manager)
4. **Input Validation**: Validate all job parameters and inputs
5. **Audit Logging**: Log all Control-M operations for audit trails

### Monitoring and Observability
```java
// Example metrics and health checks
@Component
public class ControlMHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            // Check Control-M API connectivity
            ResponseEntity<String> response = restTemplate.getForEntity(
                controlMApiBaseUrl + "/health", String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return Health.up()
                    .withDetail("control-m-api", "Available")
                    .withDetail("endpoint", controlMApiBaseUrl)
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("control-m-api", "Unavailable")
                .withDetail("error", e.getMessage())
                .build();
        }
        return Health.down().build();
    }
}
```

## Troubleshooting

### Common Issues

#### 1. Mountebank Not Starting
```bash
# Check if port is available
netstat -an | grep 2525

# Kill process using port
taskkill /F /PID <process-id>  # Windows
kill -9 <process-id>           # Linux/Mac
```

#### 2. Control-M API Connection Issues
```bash
# Test connectivity
curl -X GET http://localhost:2525/imposters

# Check application health
curl -X GET http://localhost:8080/control-m/health
```

#### 3. Service Layer Issues
- Verify `@Service` annotation on ControlMService
- Check dependency injection with `@Autowired` or constructor injection
- Ensure RestTemplate bean is configured

#### 4. Test Failures
- Verify `@MockBean` usage in controller tests
- Check Mountebank is running before integration tests
- Validate test configuration properties

### Debug Commands
```bash
# Check Mountebank status
curl http://localhost:2525/imposters

# View specific imposter
curl http://localhost:2525/imposters/2525

# Check application endpoints
curl http://localhost:8080/actuator/health
curl http://localhost:8080/control-m/health

# Test job submission
curl -X POST http://localhost:8080/control-m/jobs/start \
  -H "Content-Type: application/json" \
  -d '{"jobName": "TestJob"}'
```

### Logging Configuration
```yaml
logging:
  level:
    com.yourpackage.service.ControlMService: DEBUG
    org.springframework.web.client.RestTemplate: DEBUG
    org.springframework.web: DEBUG
```

## Migration Guide

### Moving to Another Environment

1. **Copy Configuration Files**:
   - `application.yml` (Control-M configuration)
   - `src/test/resources/mountebank/` (All stub files)

2. **Copy Source Code**:
   - `ControlMService.java`
   - `ControlMController.java`
   - `RestTemplateConfig.java`

3. **Copy Tests**:
   - `ControlMControllerTest.java`
   - Karate feature files

4. **Update Dependencies** in `build.gradle`

5. **Environment-Specific Configuration**:
   - Update `control-m.api.base-url` for target environment
   - Configure authentication if moving to production

6. **Verification Steps**:
   ```bash
   # Start Mountebank
   ./gradlew startMountebank
   
   # Run tests
   ./gradlew test
   
   # Start application
   ./gradlew bootRun
   
   # Test endpoints
   curl http://localhost:8080/control-m/health
   ```

## Best Practices

1. **Service Layer Separation**: Keep business logic in service layer, HTTP concerns in controller
2. **Exception Handling**: Use custom exceptions for different error scenarios
3. **Configuration Management**: Use external configuration for environment-specific settings
4. **Testing Strategy**: Unit tests with mocking + Integration tests with Mountebank
5. **Documentation**: Keep API documentation updated with Swagger/OpenAPI
6. **Monitoring**: Implement health checks and metrics for production monitoring
7. **Security**: Follow security best practices for API authentication and data handling

## References

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Mountebank Documentation](http://www.mbtest.org/)
- [Control-M API Documentation](https://docs.bmc.com/docs/automation-api/monthly)
- [RestTemplate Guide](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#rest-resttemplate)
