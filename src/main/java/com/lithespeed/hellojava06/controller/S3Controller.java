package com.lithespeed.hellojava06.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.lithespeed.hellojava06.service.S3Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/s3")
public class S3Controller {

    private final S3Service s3Service;

    @Autowired
    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @PostMapping("/upload")
    @Operation(summary = "Upload a file")
    public String uploadFile(@RequestParam String fileName) {
        s3Service.uploadFile(fileName);
        return "Uploaded: " + fileName;
    }

    @GetMapping("/list")
    @Operation(summary = "List files")
    public List<String> listFiles() {
        return s3Service.listFiles();
    }

    @GetMapping("/health")
    @Operation(summary = "Healthcheck")
    public Map<String, String> healthCheck() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "UP");
        return result;
    }
}
