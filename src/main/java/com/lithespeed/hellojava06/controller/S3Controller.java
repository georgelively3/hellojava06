package com.lithespeed.hellojava06.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.springframework.http.MediaType;
import java.time.LocalDateTime;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.lithespeed.hellojava06.service.S3Service;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/s3")
public class S3Controller {

    private static final Logger logger = LoggerFactory.getLogger(S3Controller.class);

    private final S3Service s3Service;
    private final Environment environment;

    @Autowired
    public S3Controller(S3Service s3Service, Environment environment) {
        this.s3Service = s3Service;
        this.environment = environment;
    }

    @PostMapping(value = "/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a multipart file", description = "Upload a real file using multipart form data")
    public ResponseEntity<Map<String, Object>> uploadMultipartFile(
            @RequestBody(description = "File to upload", required = true) @RequestParam("file") MultipartFile file) {

        Map<String, Object> response = new HashMap<>();

        if (file.isEmpty()) {
            response.put("error", "File cannot be empty");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            String etag = s3Service.uploadFile(file);
            logger.info("File uploaded successfully using S3Service");
            
            String fileName = file.getOriginalFilename();

            response.put("success", true);
            response.put("message", "File uploaded successfully");
            response.put("fileName", fileName);
            response.put("contentType", file.getContentType());
            response.put("size", file.getSize());
            response.put("etag", etag);

            logger.info("Successfully uploaded file: {} (size: {} bytes)",
                    fileName, file.getSize());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to upload file: {}", file.getOriginalFilename(), e);
            Map<String, Object> errorResponse = createErrorResponse(
                    "upload file to S3",
                    e,
                    "fileName: " + file.getOriginalFilename());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/list")
    @Operation(summary = "List files")
    public ResponseEntity<?> listFiles() {
        try {
            List<String> files = s3Service.listFiles();
            logger.info("Files listed successfully using S3Service");
            
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            logger.error("Failed to list files", e);
            Map<String, Object> errorResponse = createErrorResponse(
                    "list files from S3",
                    e,
                    "Attempting to retrieve S3 file list");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Healthcheck")
    public Map<String, String> healthCheck() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "UP");
        return result;
    }

    /**
     * Debug endpoint to show all AWS/S3/K8s related configuration
     * DELETE THIS METHOD once debugging is complete!
     */
    @GetMapping("/debug/config")
    @Operation(summary = "Debug configuration (REMOVE IN PRODUCTION)")
    public ResponseEntity<Map<String, Object>> getDebugConfig() {
        Map<String, Object> config = new HashMap<>();
        
        // AWS Environment Variables
        Map<String, String> awsVars = new HashMap<>();
        awsVars.put("AWS_ROLE_ARN", System.getenv("AWS_ROLE_ARN"));
        awsVars.put("AWS_WEB_IDENTITY_TOKEN_FILE", System.getenv("AWS_WEB_IDENTITY_TOKEN_FILE"));
        awsVars.put("AWS_REGION", System.getenv("AWS_REGION"));
        awsVars.put("S3_BUCKET_NAME", System.getenv("S3_BUCKET_NAME"));
        awsVars.put("S3_BUCKET_ARN", System.getenv("S3_BUCKET_ARN"));
        awsVars.put("AWS_USE_IAM_ROLE", System.getenv("AWS_USE_IAM_ROLE"));
        
        // What Spring Environment is resolving these to
        Map<String, String> springResolved = new HashMap<>();
        springResolved.put("aws.s3.bucket-name", environment.getProperty("aws.s3.bucket-name"));
        springResolved.put("aws.s3.bucket-arn", environment.getProperty("aws.s3.bucket-arn"));
        springResolved.put("aws.s3.region", environment.getProperty("aws.s3.region"));
        springResolved.put("aws.s3.use-iam-role", environment.getProperty("aws.s3.use-iam-role"));
        springResolved.put("aws.s3.connection-timeout", environment.getProperty("aws.s3.connection-timeout"));
        springResolved.put("aws.s3.socket-timeout", environment.getProperty("aws.s3.socket-timeout"));
        springResolved.put("aws.s3.max-connections", environment.getProperty("aws.s3.max-connections"));
        
        // File system checks
        Map<String, Object> fileChecks = new HashMap<>();
        File tokenFile = new File("/var/run/secrets/eks.amazonaws.com/serviceaccount/token");
        fileChecks.put("irsa-token-exists", tokenFile.exists());
        fileChecks.put("irsa-token-readable", tokenFile.canRead());
        if (tokenFile.exists()) {
            fileChecks.put("irsa-token-size", tokenFile.length());
        }
        
        // AWS SDK Default Region check
        try {
            fileChecks.put("aws-sdk-default-region", 
                software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain.builder()
                    .build().getRegion().id());
        } catch (Exception e) {
            fileChecks.put("aws-sdk-default-region-error", e.getMessage());
        }
        
        // All environment variables (filtered for security)
        Map<String, String> filteredEnv = new HashMap<>();
        Map<String, String> env = System.getenv();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("AWS_") || key.startsWith("S3_") || 
                key.contains("KUBERNETES") || key.contains("SERVICE") || 
                key.startsWith("POD_") || key.contains("NAMESPACE")) {
                filteredEnv.put(key, entry.getValue());
            }
        }
        
        // Build response
        config.put("environment-variables", awsVars);
        config.put("spring-resolved-config", springResolved);
        config.put("file-checks", fileChecks);
        config.put("filtered-environment", filteredEnv);
        config.put("timestamp", java.time.Instant.now().toString());
        
        logger.info("Debug config endpoint accessed");
        
        return ResponseEntity.ok(config);
    }

    /**
     * Debug endpoint to show what credentials the S3Service is actually using
     * DELETE THIS METHOD once debugging is complete!
     */
    @GetMapping("/debug/credentials")
    @Operation(summary = "Debug S3 credentials (REMOVE IN PRODUCTION)")
    public ResponseEntity<Map<String, String>> getDebugCredentials() {
        try {
            Map<String, String> credentialsInfo = s3Service.debugCredentials();
            logger.info("S3 credentials debug endpoint accessed");
            return ResponseEntity.ok(credentialsInfo);
        } catch (Exception e) {
            logger.error("Error getting S3 credentials debug info", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("error-type", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Creates a detailed error response with stack trace and context information
     */
    private Map<String, Object> createErrorResponse(String operation, Exception e, String context) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("operation", operation);
        errorResponse.put("exceptionType", e.getClass().getSimpleName());
        errorResponse.put("message", e.getMessage());
        errorResponse.put("context", context);

        // Add stack trace for detailed debugging
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        errorResponse.put("stackTrace", sw.toString());

        // Add cause information if available
        if (e.getCause() != null) {
            errorResponse.put("causeType", e.getCause().getClass().getSimpleName());
            errorResponse.put("causeMessage", e.getCause().getMessage());
        }

        return errorResponse;
    }
}
