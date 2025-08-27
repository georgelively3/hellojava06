package com.lithespeed.hellojava06.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Mock Control-M service for development and testing environments.
 * Simulates Control-M server responses without requiring actual Control-M infrastructure.
 */
@Service
@ConditionalOnProperty(name = "control-m.mock.enabled", havingValue = "true", matchIfMissing = true)
public class MockControlMService {
    
    /**
     * Simulates Control-M server job submission responses
     */
    public Map<String, Object> simulateJobSubmission(String jobName, Map<String, Object> parameters) {
        // Simulate different response times based on job type
        int delay = switch (jobName.toLowerCase()) {
            case "quicktest" -> 100;
            case "dailyreport" -> 500;
            case "dataextract", "datatransform", "dataload" -> 1000;
            default -> 300;
        };
        
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simulate job submission response
        return Map.of(
            "controlMJobId", "CTM_JOB_" + System.currentTimeMillis(),
            "status", "SUBMITTED",
            "scheduledTime", "2025-01-15T14:00:00Z",
            "estimatedDuration", calculateEstimatedDuration(jobName),
            "priority", parameters.getOrDefault("priority", "NORMAL")
        );
    }
    
    /**
     * Simulates Control-M job status polling
     */
    public Map<String, Object> simulateJobStatusCheck(String controlMJobId) {
        // Simulate different statuses based on job ID pattern
        String status;
        if (controlMJobId.endsWith("1") || controlMJobId.endsWith("3")) {
            status = "SUCCESS";
        } else if (controlMJobId.endsWith("2")) {
            status = "RUNNING";
        } else {
            status = "FAILED";
        }
        
        return Map.of(
            "controlMJobId", controlMJobId,
            "status", status,
            "startTime", "2025-01-15T14:00:00Z",
            "endTime", status.equals("RUNNING") ? null : "2025-01-15T14:05:00Z",
            "output", status.equals("SUCCESS") ? "Job completed successfully" : 
                     status.equals("FAILED") ? "Job failed due to timeout" : "Job in progress"
        );
    }
    
    private String calculateEstimatedDuration(String jobName) {
        return switch (jobName.toLowerCase()) {
            case "quicktest" -> "30 seconds";
            case "dailyreport" -> "5 minutes";
            case "dataextract" -> "10 minutes";
            case "datatransform" -> "15 minutes"; 
            case "dataload" -> "20 minutes";
            default -> "5 minutes";
        };
    }
}
