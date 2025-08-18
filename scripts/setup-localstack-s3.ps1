# LocalStack S3 Setup Script for Windows
# This script creates the necessary S3 bucket in LocalStack for development

$LOCALSTACK_ENDPOINT = "http://localhost:4566"
$BUCKET_NAME = "test-bucket"
$AWS_REGION = "us-east-1"

Write-Host "Setting up S3 bucket in LocalStack..." -ForegroundColor Green

# Wait for LocalStack to be ready
Write-Host "Waiting for LocalStack to be ready..." -ForegroundColor Yellow
do {
    try {
        $response = Invoke-RestMethod -Uri "$LOCALSTACK_ENDPOINT/_localstack/health" -Method Get -TimeoutSec 5
        $s3Available = $response.services.s3 -eq "available"
    } catch {
        $s3Available = $false
    }
    
    if (-not $s3Available) {
        Write-Host "Waiting for S3 service..." -ForegroundColor Yellow
        Start-Sleep -Seconds 2
    }
} while (-not $s3Available)

Write-Host "LocalStack S3 service is ready!" -ForegroundColor Green

# Create the bucket
Write-Host "Creating S3 bucket: $BUCKET_NAME" -ForegroundColor Cyan
aws s3 mb "s3://$BUCKET_NAME" --endpoint-url $LOCALSTACK_ENDPOINT --region $AWS_REGION --no-cli-pager

# List buckets to verify
Write-Host "Listing S3 buckets:" -ForegroundColor Cyan
aws s3 ls --endpoint-url $LOCALSTACK_ENDPOINT --no-cli-pager

Write-Host "S3 setup complete!" -ForegroundColor Green
Write-Host ""
Write-Host "You can now:" -ForegroundColor White
Write-Host "1. Start your Spring Boot application with profile: localstack" -ForegroundColor White
Write-Host "2. Use the S3 endpoints at: $LOCALSTACK_ENDPOINT" -ForegroundColor White
Write-Host "3. Bucket name: $BUCKET_NAME" -ForegroundColor White
