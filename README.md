# HelloJava06 - Spring Boot Application

A comprehensive Spring Boot application demonstrating CRUD operations with PostgreSQL (H2 in development), AWS S3 integration, comprehensive testing, and API documentation.

## Features

- **User Management**: Full CRUD operations for user entities
- **Database Integration**: PostgreSQL for production, H2 for development
- **Database Migration**: Flyway for schema management and data seeding
- **Cloud Storage**: AWS S3 integration for file operations
- **Comprehensive Testing**: Unit tests, integration tests, and Cucumber BDD tests
- **API Documentation**: RESTful API endpoints with proper error handling
- **Build System**: Gradle 8.5 with comprehensive build configuration

## Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: PostgreSQL (Production), H2 (Development)
- **Migration**: Flyway
- **Cloud Storage**: AWS S3
- **Testing**: JUnit 5, Mockito, Testcontainers, Cucumber
- **Build Tool**: Gradle 8.5
- **Code Coverage**: JaCoCo (80% minimum coverage)

## Project Structure

```
src/
├── main/
│   ├── java/com/lithespeed/hellojava06/
│   │   ├── HelloJava06Application.java          # Main application class
│   │   ├── controller/
│   │   │   └── MainController.java              # REST API controller
│   │   ├── entity/
│   │   │   └── User.java                        # User entity
│   │   ├── exception/
│   │   │   ├── DuplicateResourceException.java
│   │   │   ├── ResourceNotFoundException.java
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── repository/
│   │   │   └── UserRepository.java              # Data repository
│   │   └── service/
│   │       ├── UserService.java                 # Business logic
│   │       └── S3Service.java                   # S3 integration
│   └── resources/
│       ├── application.yml                      # Main configuration
│       ├── application-prod.yml                 # Production configuration
│       └── db/migration/                        # Flyway migrations
│           ├── V1__Create_users_table.sql
│           └── V2__Insert_initial_data.sql
└── test/
    ├── java/com/lithespeed/hellojava06/
    │   ├── controller/
    │   │   └── MainControllerTest.java          # Controller unit tests
    │   ├── service/
    │   │   ├── UserServiceTest.java             # Service unit tests
    │   │   └── S3ServiceTest.java               # S3 service unit tests
    │   ├── integration/
    │   │   └── UserIntegrationTest.java         # Integration tests
    │   └── cucumber/
    │       ├── CucumberTestRunner.java          # Cucumber test runner
    │       └── steps/                           # Cucumber step definitions
    └── resources/
        ├── application.yml                      # Test configuration
        └── features/
            └── user-management.feature          # BDD scenarios
```

## Database Schema

The application uses a simple user management schema:

### Users Table
| Column     | Type         | Constraints           |
|------------|-------------|-----------------------|
| id         | BIGSERIAL    | PRIMARY KEY           |
| username   | VARCHAR(50)  | NOT NULL, UNIQUE      |
| email      | VARCHAR(100) | NOT NULL, UNIQUE      |
| first_name | VARCHAR(50)  | NOT NULL              |
| last_name  | VARCHAR(50)  | NOT NULL              |
| created_at | TIMESTAMP    | NOT NULL, DEFAULT NOW |
| updated_at | TIMESTAMP    | NOT NULL, DEFAULT NOW |

## API Endpoints

### User Management
- `GET /api/users` - Get all users
- `GET /api/users/paginated` - Get paginated users
- `GET /api/users/{id}` - Get user by ID
- `GET /api/users/username/{username}` - Get user by username
- `GET /api/users/email/{email}` - Get user by email
- `GET /api/users/search?name={name}` - Search users by name
- `POST /api/users` - Create new user
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user
- `GET /api/users/count` - Get user count

### S3 File Operations
- `POST /api/s3/upload` - Upload file to S3
- `GET /api/s3/download/{key}` - Download file from S3
- `DELETE /api/s3/delete/{key}` - Delete file from S3
- `GET /api/s3/list?prefix={prefix}` - List files in S3
- `GET /api/s3/exists/{key}` - Check if file exists in S3

### System
- `GET /api/health` - Health check endpoint

## Configuration

### Application Configuration (application.yml)
```yaml
spring:
  application:
    name: hellojava06
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: password
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
  flyway:
    baseline-on-migrate: true
    validate-on-migrate: true

aws:
  s3:
    bucket-name: ${S3_BUCKET_NAME:test-bucket}
    region: ${AWS_REGION:us-east-1}
    access-key: ${AWS_ACCESS_KEY:}
    secret-key: ${AWS_SECRET_KEY:}
```

### Environment Variables
- `DB_HOST` - Database host (production)
- `DB_PORT` - Database port (production)
- `DB_NAME` - Database name (production)
- `DB_USERNAME` - Database username (production)
- `DB_PASSWORD` - Database password (production)
- `S3_BUCKET_NAME` - AWS S3 bucket name
- `AWS_REGION` - AWS region
- `AWS_ACCESS_KEY` - AWS access key
- `AWS_SECRET_KEY` - AWS secret key

## Getting Started

### Prerequisites
- Java 17 or higher
- Docker (for running PostgreSQL in production)
- AWS account and S3 bucket (for S3 features)

### Running the Application

1. **Development Mode (H2 Database)**
   ```powershell
   ./gradlew bootRun
   ```

2. **Production Mode (PostgreSQL)**
   ```powershell
   # Set up PostgreSQL database
   docker run --name postgres-db -e POSTGRES_DB=hellojava06 -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=password -p 5432:5432 -d postgres:15-alpine
   
   # Run application with production profile
   ./gradlew bootRun --args="--spring.profiles.active=prod"
   ```

3. **Access the Application**
   - Application: http://localhost:8080
   - H2 Console (dev mode): http://localhost:8080/h2-console
   - Health Check: http://localhost:8080/api/health

### Building the Application

```powershell
# Build the application
./gradlew build

# Build without tests
./gradlew build -x test

# Create executable JAR
./gradlew bootJar
```

## Testing

The project includes comprehensive testing at multiple levels:

### Running Tests

```powershell
# Run all tests
./gradlew test

# Run only unit tests
./gradlew test --tests "*Test"

# Run only integration tests
./gradlew test --tests "*IntegrationTest"

# Run only Cucumber tests
./gradlew test --tests "*CucumberTestRunner"

# Generate coverage report
./gradlew jacocoTestReport
```

### Test Coverage
- **Target**: 80% code coverage
- **Tools**: JaCoCo for coverage reporting
- **Location**: `build/reports/jacoco/test/html/index.html`

### Test Types

1. **Unit Tests**
   - Service layer tests with Mockito
   - Controller tests with MockMvc
   - Repository layer validation

2. **Integration Tests**
   - Full application context testing
   - Database integration with Testcontainers
   - End-to-end API testing

3. **BDD Tests (Cucumber)**
   - Behavior-driven development scenarios
   - User story validation
   - API contract testing

## AWS S3 Integration

### Setup S3 Credentials

1. **Environment Variables**
   ```powershell
   $env:AWS_ACCESS_KEY="your-access-key"
   $env:AWS_SECRET_KEY="your-secret-key"
   $env:S3_BUCKET_NAME="your-bucket-name"
   $env:AWS_REGION="us-east-1"
   ```

2. **IAM Policy Required**
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Action": [
           "s3:GetObject",
           "s3:PutObject",
           "s3:DeleteObject",
           "s3:ListBucket"
         ],
         "Resource": [
           "arn:aws:s3:::your-bucket-name",
           "arn:aws:s3:::your-bucket-name/*"
         ]
       }
     ]
   }
   ```

### S3 Operations Examples

```powershell
# Upload a file
curl -X POST http://localhost:8080/api/s3/upload -F "file=@example.txt" -F "key=uploads/example.txt"

# Download a file
curl -X GET http://localhost:8080/api/s3/download/uploads/example.txt -o downloaded-example.txt

# List files
curl -X GET "http://localhost:8080/api/s3/list?prefix=uploads/"

# Check if file exists
curl -X GET http://localhost:8080/api/s3/exists/uploads/example.txt

# Delete a file
curl -X DELETE http://localhost:8080/api/s3/delete/uploads/example.txt
```

## Database Migration

Flyway migrations are located in `src/main/resources/db/migration/`:

- `V1__Create_users_table.sql` - Creates the users table with indexes
- `V2__Insert_initial_data.sql` - Seeds initial user data

### Migration Commands
```powershell
# Check migration status
./gradlew flywayInfo

# Migrate database
./gradlew flywayMigrate

# Clean database (development only)
./gradlew flywayClean
```

## Production Deployment

### Docker Deployment

1. **Build Docker Image**
   ```dockerfile
   FROM openjdk:17-jdk-slim
   COPY build/libs/hellojava06-*.jar app.jar
   EXPOSE 8080
   ENTRYPOINT ["java", "-jar", "/app.jar"]
   ```

2. **Docker Compose Setup**
   ```yaml
   version: '3.8'
   services:
     app:
       build: .
       ports:
         - "8080:8080"
       environment:
         - SPRING_PROFILES_ACTIVE=prod
         - DB_HOST=postgres
         - DB_NAME=hellojava06
         - DB_USERNAME=postgres
         - DB_PASSWORD=password
       depends_on:
         - postgres
     postgres:
       image: postgres:15-alpine
       environment:
         - POSTGRES_DB=hellojava06
         - POSTGRES_USER=postgres
         - POSTGRES_PASSWORD=password
       ports:
         - "5432:5432"
   ```

### Environment Configuration

Production applications should use environment-specific configurations:

- Database connection pooling
- Connection timeout settings
- Logging configuration
- Security configurations
- Monitoring and metrics

## Monitoring and Health Checks

- **Health Endpoint**: `/actuator/health`
- **Application Health**: `/api/health`
- **Metrics**: `/actuator/metrics` (when enabled)

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions, please create an issue in the repository or contact the development team.
