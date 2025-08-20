# Docker S3 Integration Troubleshooting Guide

## Problem: Container Exits After 12 Seconds When Adding S3Client

### Root Causes:
1. **Missing AWS Configuration Properties** - Spring Boot fails during startup when trying to inject undefined properties
2. **AWS Auto-Configuration Issues** - Spring Boot tries to connect to real AWS services during startup
3. **Credential Provider Chain Failure** - DefaultCredentialsProvider can't find any valid credentials
4. **Network Issues** - Container can't reach AWS endpoints during health checks

## Solutions:

### 1. **IMMEDIATE FIX - Disable S3 by Default**

Add these properties to your `application.properties`:

```properties
# Disable S3 by default to prevent startup failures
aws.s3.enabled=false
aws.s3.region=us-east-1
aws.s3.bucket-name=your-bucket-name

# Connection settings with defaults
aws.s3.connection-timeout=10000
aws.s3.socket-timeout=50000
aws.s3.max-connections=50
aws.s3.max-error-retry=3
```

### 2. **Replace Your Current S3Config**

Replace your current `S3Config.java` with `FixedS3Config.java` (provided above) that:
- Uses `@ConditionalOnProperty` to only create S3Client when enabled
- Provides a safe mock S3Client by default
- Won't try to connect to AWS during startup

### 3. **Update Your S3Service**

Replace your current `S3Service.java` with the Docker-safe version that:
- Checks if S3 is enabled before making calls
- Returns mock data when S3 is disabled
- Has better error handling and logging

### 4. **Docker Environment Variables**

When running your Docker container, you can control S3 behavior:

```bash
# Run with S3 disabled (safe default)
docker run your-app:latest

# Run with S3 enabled for LocalStack
docker run -e AWS_S3_ENABLED=true \
          -e AWS_S3_ENDPOINT_URL=http://localstack:4566 \
          -e AWS_S3_ACCESS_KEY=test \
          -e AWS_S3_SECRET_KEY=test \
          your-app:latest

# Run with real AWS S3
docker run -e AWS_S3_ENABLED=true \
          -e AWS_S3_REGION=us-east-1 \
          -e AWS_S3_BUCKET_NAME=your-real-bucket \
          -e AWS_ACCESS_KEY_ID=your-key \
          -e AWS_SECRET_ACCESS_KEY=your-secret \
          your-app:latest
```

### 5. **Debugging Steps**

If your container still fails:

1. **Check Startup Logs**:
```bash
docker logs your-container-name
```

2. **Run with Debug Logging**:
```bash
docker run -e LOGGING_LEVEL_ROOT=DEBUG your-app:latest
```

3. **Test Health Endpoint**:
```bash
# After container starts
curl http://localhost:8080/api/s3/health
```

4. **Check AWS SDK Auto-Configuration**:
Add this to your `application.properties`:
```properties
logging.level.software.amazon.awssdk=DEBUG
logging.level.com.lithespeed.hellojava06=DEBUG
```

### 6. **Application.properties Template for Docker**

Create this `application-docker.properties`:

```properties
# Spring Boot Docker Configuration
server.port=8080
spring.profiles.active=docker

# Database (your existing Postgres config)
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/hellojava06}
spring.datasource.username=${DATABASE_USERNAME:postgres}
spring.datasource.password=${DATABASE_PASSWORD:password}

# S3 Configuration - Safe defaults
aws.s3.enabled=${AWS_S3_ENABLED:false}
aws.s3.region=${AWS_S3_REGION:us-east-1}
aws.s3.bucket-name=${AWS_S3_BUCKET_NAME:test-bucket}
aws.s3.endpoint-url=${AWS_S3_ENDPOINT_URL:}
aws.s3.access-key=${AWS_S3_ACCESS_KEY:}
aws.s3.secret-key=${AWS_S3_SECRET_KEY:}

# Multipart upload settings
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Logging
logging.level.com.lithespeed.hellojava06=INFO
logging.level.software.amazon.awssdk=WARN
```

### 7. **Dockerfile Recommendations**

```dockerfile
# Add health check to your Dockerfile
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Set safe environment variables
ENV AWS_S3_ENABLED=false
ENV SPRING_PROFILES_ACTIVE=docker
```

### 8. **Testing Sequence**

Test in this order:

1. **Deploy with S3 disabled** (should work)
2. **Test your existing DialogController** (should work)
3. **Test S3 health endpoint** (should return "DOWN" status)
4. **Test S3 list endpoint** (should return mock data)
5. **Enable S3 with LocalStack** (if available)
6. **Enable S3 with real AWS** (production)

### 9. **Common Error Messages and Solutions**

| Error | Cause | Solution |
|-------|-------|----------|
| `Unable to load credentials from any of the providers in the chain` | No AWS credentials found | Set `aws.s3.enabled=false` or provide credentials |
| `Connection refused: localhost:4566` | LocalStack not running | Start LocalStack or disable S3 |
| `The AWS Access Key Id you provided does not exist` | Invalid credentials | Check your AWS credentials |
| `ApplicationContext failed to start` | Missing properties | Add default values to all `@Value` annotations |

### 10. **Quick Test Commands**

Once your container is running:

```bash
# Test health
curl http://localhost:8080/api/s3/health

# Test list files (should return mock data)
curl http://localhost:8080/api/s3/list

# Test with prefix
curl "http://localhost:8080/api/s3/list?prefix=test"
```

## Next Steps

1. Replace your configuration files with the safe versions provided
2. Deploy with S3 disabled first to verify basic functionality
3. Gradually enable S3 features as needed
4. Add proper logging to track startup issues
5. Use environment variables for all AWS configuration

This approach ensures your container will start successfully even without AWS access, and you can enable S3 features incrementally.
