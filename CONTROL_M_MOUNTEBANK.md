# Control-M Service Virtualization with Mountebank

This project uses **Mountebank** for Control-M service virtualization, providing realistic stubbed responses for development and testing without requiring actual Control-M infrastructure.

## Overview

The Control-M integration uses:
- **ControlMController**: Makes HTTP calls to external Control-M API
- **Mountebank**: Provides stubbed Control-M API responses on port 2525
- **Karate Tests**: Integration tests that validate end-to-end Control-M workflows

## Mountebank Configuration

### Imposter Files

- `src/test/resources/mountebank/imposters.json` - Master configuration with all stubs
- `src/test/resources/mountebank/control-m-submit.json` - Job submission stubs
- `src/test/resources/mountebank/control-m-status.json` - Job status stubs  
- `src/test/resources/mountebank/control-m-cancel.json` - Job cancellation stubs

### Stubbed Endpoints

#### Job Submission: `POST /control-m/jobs/submit`
Returns realistic Control-M job IDs based on job type:
- `quicktest` → `CTM_QT_<timestamp>`
- `dailyreport` → `CTM_DR_<timestamp>`
- `dataextract` → `CTM_DE_<timestamp>`
- `datatransform` → `CTM_DT_<timestamp>`
- `dataload` → `CTM_DL_<timestamp>`
- Generic jobs → `CTM_GJ_<timestamp>`

#### Job Status: `GET /control-m/jobs/{jobId}/status`
Returns status based on job ID pattern:
- Quick test jobs: Fast completion with minimal resources
- Daily reports: Medium duration with moderate resources
- ETL jobs: Longer duration with high resource usage
- All jobs: SUCCESS status with realistic resource metrics

#### Job Cancellation: `POST /control-m/jobs/{jobId}/cancel`
Returns cancellation confirmation with cleanup details.

## Running with Mountebank

### Prerequisites
- Mountebank installed (`npm install -g mountebank`)
- Docker (optional, for containerized approach)

### Option 1: Local Mountebank

```bash
# Start Mountebank
mb start --port 2525 --configfile src/test/resources/mountebank/imposters.json

# Run your application
./gradlew bootRun

# Run integration tests
./gradlew intTest
```

### Option 2: Docker Mountebank

```bash
# Start Mountebank in Docker
docker run -d --name mountebank-control-m \
  -p 2525:2525 \
  -v $(pwd)/src/test/resources/mountebank:/config \
  bbyars/mountebank:2.8.2 \
  mb start --port 2525 --configfile /config/imposters.json

# Run your application
./gradlew bootRun

# Run integration tests  
./gradlew intTest

# Stop Mountebank
docker stop mountebank-control-m
docker rm mountebank-control-m
```

### Option 3: Gradle Integration (Recommended)

Add to your `build.gradle`:

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
    include '**/*KarateTest*'
    
    systemProperty 'control-m.api.base-url', 'http://localhost:2525'
    
    testLogging {
        events "passed", "skipped", "failed"
        exceptionFormat "full"
    }
}
```

## Configuration

### Application Properties

The application uses the following configuration:

```yaml
# Default configuration
control-m:
  api:
    base-url: ${CONTROL_M_API_URL:http://localhost:2525}
```

### Environment Variables

- `CONTROL_M_API_URL`: Override Control-M API URL (default: http://localhost:2525)

## Karate Test Integration

The Karate tests automatically work with Mountebank:

1. **Job Type Validation**: Tests verify correct Control-M job ID prefixes
2. **Status Polling**: Tests validate realistic job status responses  
3. **Resource Metrics**: Tests check CPU, memory, and runtime values
4. **ETL Workflows**: Tests simulate complete data pipeline workflows
5. **Error Scenarios**: Tests handle cancellation and failure cases

## Benefits of Mountebank Approach

### ✅ **Independent Testing**
- No dependency on actual Control-M infrastructure
- Consistent, predictable responses for all environments
- Fast test execution without external service delays

### ✅ **Realistic Simulation**  
- Job-type specific responses and timing
- Resource usage metrics that match real Control-M behavior
- Proper HTTP status codes and error handling

### ✅ **CI/CD Friendly**
- Easily integrates with build pipelines
- Containerized execution for consistent environments
- No external service dependencies or credentials

### ✅ **Development Efficiency**
- Immediate feedback during development
- No wait time for actual job execution
- Easy modification of stub responses for testing edge cases

## Troubleshooting

### Common Issues

1. **Port 2525 in use**: Change port in `imposters.json` and update `CONTROL_M_API_URL`
2. **Mountebank not starting**: Check Docker installation and port availability
3. **Tests failing**: Verify Mountebank is running and accessible at configured URL
4. **Missing stubs**: Check imposter configuration matches your test scenarios

### Debugging Tips

```bash
# Check Mountebank status
curl http://localhost:2525/imposters

# View specific imposter
curl http://localhost:2525/imposters/2525

# Check application health with Control-M URL
curl http://localhost:8080/control-m/health
```

## Production Deployment

In production environments:
1. Replace `control-m.api.base-url` with actual Control-M API endpoint
2. Configure proper authentication/certificates for Control-M
3. Remove Mountebank dependencies from production builds
4. Use feature flags to switch between mock and real Control-M APIs
