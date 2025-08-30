package com.lithespeed.hellojava06.controller;

import com.lithespeed.hellojava06.service.ControlMService;
import com.lithespeed.hellojava06.service.ControlMService.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Control-M job management.
 * Provides HTTP endpoints for job lifecycle operations.
 * Delegates business logic to ControlMService.
 */
@RestController
@RequestMapping("/control-m")
@Tag(name = "Control-M Integration", description = "Job scheduling and automation endpoints for Control-M integration")
public class ControlMController {

    private static final Logger logger = LoggerFactory.getLogger(ControlMController.class);

    private final ControlMService controlMService;
    private final String controlMApiBaseUrl;

    public ControlMController(ControlMService controlMService,
            @Value("${control-m.api.base-url}") String controlMApiBaseUrl) {
        this.controlMService = controlMService;
        this.controlMApiBaseUrl = controlMApiBaseUrl;
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if Control-M integration service is running")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Control-M Integration",
                "timestamp", Instant.now().toString(),
                "controlMApiUrl", controlMApiBaseUrl));
    }

    @PostMapping("/jobs/start")
    @Operation(summary = "Start a Control-M job", description = "Initialize and start a new job execution")
    @ApiResponse(responseCode = "200", description = "Job started successfully")
    public ResponseEntity<Map<String, Object>> startJob(
            @Parameter(description = "Name of the job to start", required = true) @RequestParam String jobName,
            @Parameter(description = "Optional custom job ID") @RequestParam(required = false) String jobId,
            @Parameter(description = "Job parameters") @RequestBody(required = false) Map<String, Object> parameters) {

        try {
            JobExecutionResult result = controlMService.startJob(jobName, jobId, parameters);

            return ResponseEntity.ok(Map.of(
                    "executionId", result.getExecutionId(),
                    "jobName", result.getJobName(),
                    "status", result.getStatus(),
                    "timestamp", result.getTimestamp(),
                    "message", result.getMessage(),
                    "controlMJobId", result.getControlMJobId() != null ? result.getControlMJobId() : "",
                    "estimatedDuration", result.getEstimatedDuration() != null ? result.getEstimatedDuration() : ""));

        } catch (ControlMJobException e) {
            logger.error("Failed to start job: {}", jobName, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "CONTROL_M_ERROR",
                    "message", e.getMessage(),
                    "jobName", jobName));
        }
    }

    @GetMapping("/jobs/{executionId}/status")
    @Operation(summary = "Get job status", description = "Retrieve the current status and details of a job execution")
    @ApiResponse(responseCode = "200", description = "Job status retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Job not found")
    public ResponseEntity<Map<String, Object>> getJobStatus(
            @Parameter(description = "Job execution ID", required = true) @PathVariable String executionId) {

        try {
            JobStatusResult result = controlMService.getJobStatus(executionId);

            Map<String, Object> response = new HashMap<>();
            response.put("executionId", result.getExecutionId());
            response.put("jobName", result.getJobName());
            response.put("status", result.getStatus());
            response.put("startTime", result.getStartTime());
            response.put("endTime", result.getEndTime()); // Can be null
            response.put("duration", result.getDuration());
            response.put("parameters", result.getParameters());

            return ResponseEntity.ok(response);

        } catch (JobNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/jobs/{executionId}/complete")
    @Operation(summary = "Mark job as complete", description = "Manually mark a job as completed with a specific status")
    @ApiResponse(responseCode = "200", description = "Job marked as complete")
    @ApiResponse(responseCode = "404", description = "Job not found")
    public ResponseEntity<Map<String, Object>> completeJob(
            @Parameter(description = "Job execution ID", required = true) @PathVariable String executionId,
            @Parameter(description = "Final job status (SUCCESS, FAILED, CANCELLED)", required = true) @RequestParam String status,
            @Parameter(description = "Job execution results") @RequestBody(required = false) Map<String, Object> results) {

        try {
            // This operation requires getting the job first, then updating its status
            JobStatusResult currentStatus = controlMService.getJobStatus(executionId);

            // In a real implementation, you might want to add a completeJob method to the
            // service
            return ResponseEntity.ok(Map.of(
                    "executionId", executionId,
                    "jobName", currentStatus.getJobName(),
                    "status", status.toUpperCase(),
                    "completedAt", Instant.now().toString(),
                    "duration", currentStatus.getDuration(),
                    "results", results != null ? results : Map.of(),
                    "message", "Manual completion not implemented - use Control-M API directly"));

        } catch (JobNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/jobs")
    @Operation(summary = "List all jobs", description = "Retrieve a list of all job executions, optionally filtered by status")
    @ApiResponse(responseCode = "200", description = "Jobs retrieved successfully")
    public ResponseEntity<Map<String, Object>> listJobs(
            @Parameter(description = "Filter by job status (optional)") @RequestParam(required = false) String status) {

        JobListResult result = controlMService.listJobs(status);

        return ResponseEntity.ok(Map.of(
                "jobs", result.getJobs(),
                "total", result.getTotal(),
                "filter", result.getFilter()));
    }

    @GetMapping("/jobs/{executionId}/logs")
    @Operation(summary = "Get job logs", description = "Retrieve execution logs for a specific job")
    @ApiResponse(responseCode = "200", description = "Job logs retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Job not found")
    public ResponseEntity<Map<String, Object>> getJobLogs(
            @Parameter(description = "Job execution ID", required = true) @PathVariable String executionId) {

        try {
            JobLogsResult result = controlMService.getJobLogs(executionId);

            return ResponseEntity.ok(Map.of(
                    "executionId", result.getExecutionId(),
                    "logs", result.getLogs(),
                    "logCount", result.getLogCount()));

        } catch (JobNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/batch/start")
    @Operation(summary = "Start batch jobs", description = "Start multiple jobs simultaneously")
    @ApiResponse(responseCode = "200", description = "Batch jobs started successfully")
    public ResponseEntity<Map<String, Object>> startBatchJobs(
            @Parameter(description = "List of jobs to start", required = true) @RequestBody List<ControlMService.BatchJobRequest> jobRequests) {

        BatchJobResult result = controlMService.startBatchJobs(jobRequests);

        return ResponseEntity.ok(Map.of(
                "batchId", result.getBatchId(),
                "jobsStarted", result.getJobsStarted(),
                "jobs", result.getJobs(),
                "timestamp", result.getTimestamp()));
    }

    @DeleteMapping("/jobs/{executionId}")
    @Operation(summary = "Cancel job", description = "Cancel a running job execution")
    @ApiResponse(responseCode = "200", description = "Job cancelled successfully")
    @ApiResponse(responseCode = "404", description = "Job not found")
    public ResponseEntity<Map<String, Object>> cancelJob(
            @Parameter(description = "Job execution ID", required = true) @PathVariable String executionId) {

        try {
            JobCancellationResult result = controlMService.cancelJob(executionId);

            return ResponseEntity.ok(Map.of(
                    "executionId", result.getExecutionId(),
                    "jobName", result.getJobName(),
                    "status", result.getStatus(),
                    "message", result.getMessage()));

        } catch (JobNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (ControlMJobException e) {
            logger.error("Failed to cancel job: {}", executionId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "CONTROL_M_ERROR",
                    "message", e.getMessage(),
                    "executionId", executionId));
        }
    }
}
