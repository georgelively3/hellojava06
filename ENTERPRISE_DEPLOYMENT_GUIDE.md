# Enterprise BOM/Helm Deployment Guide

## 🎯 Overview

This guide outlines the changes and configurations needed to deploy the hellojava06 application in enterprise environments using standardized BOM (Bill of Materials) and Helm charts for preprod/UAT and production deployments.

## 🔧 Key Changes Made for Enterprise Readiness

### 1. **Externalized Configuration**
- ✅ **All configuration values are externalized** via environment variables
- ✅ **No hardcoded defaults** that could conflict with enterprise standards
- ✅ **Support for Kubernetes secrets and config maps**
- ✅ **Profile-based configuration** (kubernetes, prod, uat, preprod)

### 2. **Enterprise Security Standards** 
- ✅ **IAM Roles for Service Accounts (IRSA)** support
- ✅ **No hardcoded AWS credentials** in production profiles
- ✅ **Kubernetes secrets integration** for sensitive data
- ✅ **SSL/TLS termination** at ingress level

### 3. **Observability & Monitoring**
- ✅ **Prometheus metrics** endpoint (`/actuator/prometheus`)
- ✅ **Kubernetes health probes** (liveness/readiness)
- ✅ **Custom health indicators** for S3 connectivity
- ✅ **Structured logging** for log aggregation
- ✅ **Custom application info** endpoint

### 4. **Resource Management**
- ✅ **Connection pool configuration** for database
- ✅ **S3 client timeouts and retry policies** 
- ✅ **HTTP/2 and compression** enabled
- ✅ **Separate management port** for monitoring

## 📁 New/Modified Files

### Configuration Files
| File | Purpose |
|------|---------|
| `application-prod.yml` | Enhanced production configuration with all enterprise settings |
| `application-kubernetes.yml` | Kubernetes-specific configuration with IRSA support |
| `S3Config.java` | Enterprise S3 client with proper timeouts and retry policies |
| `S3HealthIndicator.java` | Custom health check for S3 connectivity |
| `CustomInfoContributor.java` | Application info for monitoring dashboards |
| `ApplicationProperties.java` | Type-safe configuration properties |

### Enterprise Integration
| File | Purpose |
|------|---------|
| `helm-values-template.yaml` | Template showing expected Helm values structure |

## 🚀 Deployment Configuration

### Environment Variables Required

#### **Application Core**
```bash
APP_NAME=hellojava06
APP_VERSION=1.0.0
APP_ENVIRONMENT=preprod # or uat, prod
SPRING_PROFILES_ACTIVE=kubernetes,prod
```

#### **Database (from Kubernetes Secrets)**
```bash
DATABASE_URL=jdbc:postgresql://postgres-service.database.svc.cluster.local:5432/hellojava06_preprod
DB_USERNAME # From secret: hellojava06-db-credentials
DB_PASSWORD # From secret: hellojava06-db-credentials
DB_POOL_MAX_SIZE=20
DB_POOL_MIN_IDLE=5
```

#### **S3 (Enterprise BOM)**
```bash
S3_BUCKET_NAME=enterprise-hellojava06-preprod
AWS_REGION=us-east-1
# No AWS credentials - uses IAM Roles for Service Accounts (IRSA)
```

#### **Monitoring & Management**
```bash
MANAGEMENT_PORT=8081
MANAGEMENT_ENDPOINTS=health,info,metrics,prometheus
HEALTH_SHOW_DETAILS=when-authorized
PROMETHEUS_ENABLED=true
```

#### **Logging**
```bash
LOG_LEVEL_APP=INFO
LOG_LEVEL_SPRING=WARN
LOG_LEVEL_HIBERNATE=WARN
LOG_LEVEL_AWS=WARN
LOG_LEVEL_ROOT=WARN
```

### Kubernetes Resources Required

#### **Service Account with IRSA**
```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: hellojava06-s3-access
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::123456789012:role/hellojava06-s3-role
```

#### **Database Credentials Secret**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: hellojava06-db-credentials
type: Opaque
data:
  username: <base64-encoded-username>
  password: <base64-encoded-password>
```

#### **Health Probes**
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8081
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8081
  initialDelaySeconds: 10
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 3
```

## 🔍 Enterprise BOM Integration Points

### 1. **Database Integration**
- **Enterprise PostgreSQL service** automatically discovered
- **Connection pooling** configured for enterprise workloads  
- **Credentials management** via Kubernetes secrets
- **SSL/TLS** connection support

### 2. **S3 Integration**
- **IAM Roles for Service Accounts (IRSA)** - no credential management needed
- **Enterprise S3 bucket naming** convention support
- **Custom endpoint** support for S3-compatible services
- **Connection pooling and retry policies** for reliability

### 3. **Monitoring Integration**
- **Prometheus metrics** for enterprise monitoring dashboards
- **Custom health indicators** for dependency monitoring
- **Application info** endpoint for deployment tracking
- **Structured logging** for enterprise log aggregation

### 4. **Security Integration**
- **No hardcoded credentials** anywhere in the application
- **Kubernetes secrets** integration for sensitive data
- **CORS configuration** for enterprise frontend integration
- **SSL/TLS termination** at ingress level

## 📊 Monitoring Endpoints

| Endpoint | Purpose | Port |
|----------|---------|------|
| `/actuator/health` | Overall application health | 8081 |
| `/actuator/health/liveness` | Kubernetes liveness probe | 8081 |
| `/actuator/health/readiness` | Kubernetes readiness probe | 8081 |
| `/actuator/metrics` | Application metrics | 8081 |
| `/actuator/prometheus` | Prometheus scraping endpoint | 8081 |
| `/actuator/info` | Application information | 8081 |

## 🎛️ Configuration Profiles

### Development (`localstack`)
- Uses LocalStack for S3
- H2 database
- Debug logging
- No security constraints

### Kubernetes (`kubernetes`)  
- Uses IRSA for S3 access
- Enterprise PostgreSQL
- Production logging
- Health probes enabled

### Production (`prod`)
- Enterprise-grade configuration
- Optimized connection pools
- Minimal logging
- Full security enabled

## ✅ Pre-deployment Checklist

### **Enterprise BOM Requirements**
- [ ] Enterprise PostgreSQL service configured
- [ ] S3 bucket created with proper IAM policies
- [ ] IAM role for service account created
- [ ] Kubernetes secrets created for database credentials
- [ ] Monitoring infrastructure configured for Prometheus scraping

### **Application Configuration**
- [ ] All environment variables provided by Helm values
- [ ] No hardcoded values in application configuration
- [ ] Health check endpoints accessible
- [ ] Application starts with `kubernetes` profile

### **Security Validation** 
- [ ] No AWS credentials in environment variables
- [ ] Database password stored in Kubernetes secret
- [ ] S3 access works with IRSA
- [ ] CORS configured for enterprise frontend domains

## 🔄 Migration from Development

### What Changes
- **Profile**: `localstack` → `kubernetes` or `prod`
- **Database**: H2 → Enterprise PostgreSQL
- **S3 Access**: LocalStack → Real S3 with IRSA
- **Credentials**: Hardcoded → Kubernetes secrets
- **Monitoring**: Basic → Enterprise Prometheus integration

### What Stays the Same
- **Application code** - no changes needed
- **API endpoints** - same interface
- **Business logic** - unchanged  
- **Test coverage** - same test suite

## 🚀 Deployment Commands

### Build Enterprise Image
```bash
# Using enterprise build pipeline
docker build -t your-enterprise-registry/hellojava06:1.0.0 .
docker push your-enterprise-registry/hellojava06:1.0.0
```

### Helm Deployment
```bash
# Using enterprise Helm charts
helm upgrade --install hellojava06 \
  your-enterprise-charts/spring-boot-app \
  --values helm-values-preprod.yaml \
  --namespace hellojava06-preprod
```

### Verify Deployment
```bash
# Check health
kubectl get pods -n hellojava06-preprod
kubectl logs -f deployment/hellojava06 -n hellojava06-preprod

# Check endpoints
curl https://hellojava06-preprod.mycompany.com/actuator/health
curl https://hellojava06-preprod.mycompany.com/actuator/info
```

## 🎉 Enterprise Benefits

- ✅ **Zero configuration changes** needed for different environments
- ✅ **Full observability** with enterprise monitoring tools  
- ✅ **Secure by default** with no hardcoded credentials
- ✅ **Scalable architecture** with proper resource management
- ✅ **Cloud-native ready** with Kubernetes health probes
- ✅ **Maintainable** with externalized configuration

The application is now fully ready for enterprise BOM/Helm deployment with standardized configurations, security practices, and monitoring integration!
