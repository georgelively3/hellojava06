package com.lithespeed.hellojava06.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.lithespeed.hellojava06.service.S3Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/s3")
public class S3Controller {

    private final S3Service s3Service;

    @Autowired
    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @PostMapping(value = "/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a multipart file", description = "Upload a real file using multipart form data")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> uploadMultipartFile(
            @RequestBody(description = "File to upload", required = true) @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "File cannot be empty");
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(response));
        }

        return s3Service.processFileUpload(file)
                .thenApply(response -> {
                    Boolean success = (Boolean) response.get("success");
                    if (Boolean.TRUE.equals(success)) {
                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                    }
                });
    }

    @GetMapping("/list")
    @Operation(summary = "List files")
    public CompletableFuture<ResponseEntity<Object>> listFiles() {
        return s3Service.processFileList()
                .thenApply(response -> {
                    Boolean success = (Boolean) response.get("success");
                    if (Boolean.TRUE.equals(success)) {
                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((Object) response);
                    }
                });
    }

    @GetMapping("/health")
    @Operation(summary = "Healthcheck")
    public Map<String, String> healthCheck() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "UP");
        return result;
    }
}
