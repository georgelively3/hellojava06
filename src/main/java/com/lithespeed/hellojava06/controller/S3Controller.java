package com.lithespeed.hellojava06.controller;

import com.lithespeed.hellojava06.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/s3")
@CrossOrigin(origins = "*")
@Tag(name = "S3 File Management", description = "APIs for S3 file operations")
public class S3Controller {

    private final S3Service s3Service;

    @Autowired
    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file to S3", description = "Upload a file to S3 bucket with optional custom key")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file or parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error during upload")
    })
    public ResponseEntity<Map<String, String>> uploadFile(
            @Parameter(description = "File to upload", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)) @RequestParam("file") MultipartFile file,
            @Parameter(description = "Custom key/path for the file in S3", required = false) @RequestParam(value = "key", required = false) String key) {

        try {
            if (key == null || key.isEmpty()) {
                key = "uploads/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            }

            String fileUrl = s3Service.uploadFile(key, file);

            Map<String, String> response = new HashMap<>();
            response.put("message", "File uploaded successfully");
            response.put("key", key);
            response.put("url", fileUrl);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/list")
    @Operation(summary = "List S3 files", description = "List files in the S3 bucket with optional prefix filter")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Files listed successfully"),
            @ApiResponse(responseCode = "500", description = "Error accessing S3 bucket")
    })
    public ResponseEntity<Map<String, Object>> listFiles(
            @Parameter(description = "Prefix to filter files", required = false) @RequestParam(value = "prefix", required = false, defaultValue = "") String prefix) {

        try {
            List<String> files = s3Service.listFiles(prefix);

            Map<String, Object> response = new HashMap<>();
            response.put("files", files);
            response.put("count", files.size());
            response.put("prefix", prefix);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to list files: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/health")
    @Operation(summary = "S3 service health check", description = "Check if S3 service is accessible and healthy")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "S3 service is healthy")
    })
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "S3");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
}
