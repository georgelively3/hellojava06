# S3 Service Virtualization Guide

## Overview
This guide explains how to use S3 service virtualization for development and testing without requiring a real AWS S3 bucket. We use **LocalStack** to provide a local S3-compatible service.

## 🚀 Quick Start

### Option 1: Docker Compose (Recommended for Development)

1. **Start LocalStack with Docker Compose:**
   ```bash
   docker-compose -f docker-compose.dev.yml up -d localstack
   ```

2. **Set up the S3 bucket:**
   ```bash
   # On Linux/macOS
   chmod +x scripts/setup-localstack-s3.sh
   ./scripts/setup-localstack-s3.sh
   
   # On Windows
   .\scripts\setup-localstack-s3.ps1
   ```

3. **Run the application with LocalStack profile:**
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=localstack'
   ```

### Option 2: Standalone LocalStack

1. **Run LocalStack directly:**
   ```bash
   docker run -d \
     --name localstack-s3 \
     -p 4566:4566 \
     -e SERVICES=s3 \
     -e DEBUG=1 \
     localstack/localstack:3.0
   ```

2. **Follow steps 2-3 from Option 1**

## 🧪 Testing Strategies

### 1. Unit Tests (No Docker Required)
```bash
# Run unit tests only
./gradlew unitTest
```
- Uses mocked S3Client
- Fast execution
- No external dependencies
- Perfect for CI/CD pipelines

### 2. Integration Tests with LocalStack
```bash
# Run integration tests (requires Docker)
./gradlew integrationTest
```
- Uses real LocalStack container
- Full S3 API compatibility
- Validates actual S3 interactions

### 3. S3-Specific Tests
```bash
# Run S3 tests with LocalStack
./gradlew s3Test
```
- Focused S3 functionality testing
- Uses LocalStack via Testcontainers

## 🔧 Configuration Profiles

### Development Profile (`localstack`)
**File:** `application-localstack.yml`
- Uses LocalStack endpoint: `http://localhost:4566`
- Dummy credentials: `test`/`test`
- Perfect for local development

### Test Profile (`test`)
**File:** `application.yml` (test resources)
- Uses mocked S3Client
- H2 in-memory database
- Fast test execution

## 📁 Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/lithespeed/hellojava06/
│   │       ├── config/
│   │       │   └── S3Config.java              # S3 client configuration
│   │       └── service/
│   │           └── S3Service.java             # S3 operations service
│   └── resources/
│       ├── application.yml                    # Main configuration
│       └── application-localstack.yml         # LocalStack profile
├── test/
│   ├── java/
│   │   └── com/lithespeed/hellojava06/
│   │       ├── integration/
│   │       │   └── S3IntegrationTest.java     # LocalStack integration tests
│   │       └── service/
│   │           └── S3ServiceTest.java         # Unit tests with mocks
│   └── resources/
│       └── application-localstack.yml         # Test LocalStack config
scripts/
├── setup-localstack-s3.sh                    # Linux/macOS setup script
└── setup-localstack-s3.ps1                   # Windows setup script
docker-compose.dev.yml                        # Development stack
```

## 🐳 Docker Services

### LocalStack Service
- **Image:** `localstack/localstack:3.0`
- **Port:** `4566` (LocalStack Gateway)
- **Services:** S3 only
- **Persistence:** Enabled (data survives container restarts)

### Application Service (Optional)
- **Build:** Uses local Dockerfile
- **Port:** `8080`
- **Profile:** `localstack`
- **Dependencies:** LocalStack, PostgreSQL

## 🔍 API Testing

### Upload File Test
```bash
curl -X POST \
  http://localhost:8080/api/s3/upload \
  -F "file=@test.txt" \
  -F "key=uploads/test.txt"
```

### List Files Test
```bash
curl "http://localhost:8080/api/s3/list?prefix=uploads/"
```

### Direct LocalStack Test
```bash
# List buckets
aws s3 ls --endpoint-url http://localhost:4566

# Upload file directly to LocalStack
aws s3 cp test.txt s3://test-bucket/direct-upload.txt --endpoint-url http://localhost:4566
```

## 🌍 Environment Variables

### Development
```bash
export SPRING_PROFILES_ACTIVE=localstack
export LOCALSTACK_ENDPOINT=http://localhost:4566
export S3_BUCKET_NAME=test-bucket
export AWS_REGION=us-east-1
```

### CI/CD (GitHub Actions)
```yaml
env:
  SPRING_PROFILES_ACTIVE: test  # Use mocked tests
  # No LocalStack needed for unit tests
```

## 🚨 Troubleshooting

### LocalStack Not Starting
1. **Check Docker is running**
2. **Check port 4566 is available:**
   ```bash
   netstat -an | grep 4566
   ```
3. **View LocalStack logs:**
   ```bash
   docker logs hellojava06-localstack
   ```

### S3 Bucket Not Found
1. **Verify bucket creation:**
   ```bash
   aws s3 ls --endpoint-url http://localhost:4566
   ```
2. **Recreate bucket:**
   ```bash
   aws s3 mb s3://test-bucket --endpoint-url http://localhost:4566
   ```

### Tests Failing
1. **Unit tests failing:** Check mocks in `S3ServiceTest.java`
2. **Integration tests failing:** Ensure Docker is running and LocalStack container is healthy
3. **S3 endpoint not reachable:** Check LocalStack endpoint URL configuration

## 📊 Test Execution Matrix

| Test Type | Command | Docker Required | Profile | Purpose |
|-----------|---------|-----------------|---------|---------|
| Unit | `./gradlew unitTest` | ❌ | `test` | Fast feedback, CI/CD |
| Integration | `./gradlew integrationTest` | ✅ | `localstack` | Full stack validation |
| S3 Specific | `./gradlew s3Test` | ✅ | `localstack` | S3 functionality focus |
| All Tests | `./gradlew test` | ❌ | `test` | Standard test suite |

## 🎯 Best Practices

### Development
1. **Use LocalStack profile for local development**
2. **Keep LocalStack running during development sessions**
3. **Use the setup scripts to initialize S3 bucket**
4. **Check LocalStack health before running tests**

### Testing
1. **Use unit tests for fast feedback**
2. **Use integration tests for comprehensive validation**
3. **Mock S3Client in unit tests**
4. **Use Testcontainers for integration tests**

### CI/CD
1. **Use unit tests in fast CI pipelines**
2. **Use integration tests in comprehensive test suites**
3. **Consider separate S3 integration test pipeline**
4. **Cache Docker images for faster builds**

## 🔗 Migration to Real S3

When moving to preprod/UAT/production:

1. **Change profile from `localstack` to `default`**
2. **Update environment variables:**
   ```bash
   unset LOCALSTACK_ENDPOINT  # Remove LocalStack endpoint
   export AWS_REGION=us-east-1
   export S3_BUCKET_NAME=your-real-bucket
   # Use IAM roles instead of access keys in production
   ```
3. **Remove LocalStack-specific configurations**
4. **Update deployment scripts**

The application will automatically use real AWS S3 when LocalStack endpoint is not configured.

---

## 📋 Quick Reference

### Start Development Environment
```bash
docker-compose -f docker-compose.dev.yml up -d localstack
./scripts/setup-localstack-s3.sh  # or .ps1 on Windows
./gradlew bootRun --args='--spring.profiles.active=localstack'
```

### Run Tests
```bash
./gradlew unitTest           # Fast, no Docker
./gradlew s3Test            # S3 with LocalStack
./gradlew integrationTest   # Full integration
```

### Stop Environment
```bash
docker-compose -f docker-compose.dev.yml down
```

This setup provides complete S3 service virtualization without requiring real AWS resources during development and testing phases.
