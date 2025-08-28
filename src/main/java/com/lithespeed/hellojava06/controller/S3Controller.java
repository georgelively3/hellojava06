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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.lithespeed.hellojava06.service.S3Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/s3")
public class S3Controller {

    private static final Logger logger = LoggerFactory.getLogger(S3Controller.class);

    private final S3Service s3Service;

    @Autowired
    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @PostMapping(value = "/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a multipart file", 
               description = "Upload a real file using multipart form data")
    public ResponseEntity<Map<String, Object>> uploadMultipartFile(
            @RequestBody(description = "File to upload", required = true)
            @RequestParam("file") MultipartFile file) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (file.isEmpty()) {
            response.put("error", "File cannot be empty");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            String etag = s3Service.uploadFile(file);
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
                "fileName: " + file.getOriginalFilename()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/list")
    @Operation(summary = "List files")
    public ResponseEntity<?> listFiles() {
        try {
            List<String> files = s3Service.listFiles();
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            logger.error("Failed to list files", e);
            Map<String, Object> errorResponse = createErrorResponse(
                "list files from S3",
                e,
                "Attempting to retrieve S3 file list"
            );
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
