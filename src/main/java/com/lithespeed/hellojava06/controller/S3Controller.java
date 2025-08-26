package com.lithespeed.hellojava06.controller;

import io.swagger.v3.oas.annotations.Operation;
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

    @PostMapping("/upload")
    @Operation(summary = "Upload a file")
    public ResponseEntity<String> uploadFile(@RequestParam String fileName) {
        try {
            s3Service.uploadFile(fileName);
            return ResponseEntity.ok("Uploaded: " + fileName);
        } catch (Exception e) {
            logger.error("Failed to upload file: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload file: " + e.getMessage());
        }
    }

    @PostMapping("/upload-file")
    @Operation(summary = "Upload a multipart file", 
               description = "Upload a real file using multipart form data")
    public ResponseEntity<Map<String, Object>> uploadMultipartFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "customKey", required = false) String customKey) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (file.isEmpty()) {
            response.put("error", "File cannot be empty");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            String etag;
            String fileName = file.getOriginalFilename();
            
            if (customKey != null && !customKey.trim().isEmpty()) {
                etag = s3Service.uploadFile(file, customKey);
                response.put("key", customKey);
            } else {
                etag = s3Service.uploadFile(file);
                response.put("key", fileName);
            }
            
            response.put("success", true);
            response.put("message", "File uploaded successfully");
            response.put("originalFileName", fileName);
            response.put("contentType", file.getContentType());
            response.put("size", file.getSize());
            response.put("etag", etag);
            
            logger.info("Successfully uploaded file: {} (size: {} bytes)", 
                       fileName, file.getSize());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to upload file: {}", file.getOriginalFilename(), e);
            response.put("success", false);
            response.put("error", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/list")
    @Operation(summary = "List files")
    public ResponseEntity<List<String>> listFiles() {
        try {
            List<String> files = s3Service.listFiles();
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            logger.error("Failed to list files", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Healthcheck")
    public Map<String, String> healthCheck() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "UP");
        return result;
    }
}
