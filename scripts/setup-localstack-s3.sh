#!/bin/bash

# LocalStack S3 Setup Script
# This script creates the necessary S3 bucket in LocalStack for development

LOCALSTACK_ENDPOINT="http://localhost:4566"
BUCKET_NAME="test-bucket"
AWS_REGION="us-east-1"

echo "Setting up S3 bucket in LocalStack..."

# Wait for LocalStack to be ready
echo "Waiting for LocalStack to be ready..."
while ! curl -s $LOCALSTACK_ENDPOINT/_localstack/health | grep -q "s3.*available"; do
  echo "Waiting for S3 service..."
  sleep 2
done

echo "LocalStack S3 service is ready!"

# Create the bucket
echo "Creating S3 bucket: $BUCKET_NAME"
aws s3 mb s3://$BUCKET_NAME \
  --endpoint-url $LOCALSTACK_ENDPOINT \
  --region $AWS_REGION \
  --no-cli-pager

# List buckets to verify
echo "Listing S3 buckets:"
aws s3 ls \
  --endpoint-url $LOCALSTACK_ENDPOINT \
  --no-cli-pager

echo "S3 setup complete!"
echo ""
echo "You can now:"
echo "1. Start your Spring Boot application with profile: localstack"
echo "2. Use the S3 endpoints at: $LOCALSTACK_ENDPOINT"
echo "3. Bucket name: $BUCKET_NAME"
