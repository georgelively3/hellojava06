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
public class S3Controller {

    private final S3Service s3Service;

    @Autowired
    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "key", required = false) String key) {

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
    public ResponseEntity<Map<String, Object>> listFiles(
            @RequestParam(value = "prefix", required = false, defaultValue = "") String prefix) {

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
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "S3");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
}
