package com.lithespeed.hellojava06.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/control-m")
@Tag(name = "Control-M Integration", description = "Job scheduling and automation endpoints for Control-M integration")
public class ControlMController {

    private static final Logger logger = LoggerFactory.getLogger(ControlMController.class);
    
    // In-memory storage for demo purposes (in production, this might be a database)
    private final Map<String, JobExecution> jobExecutions = new ConcurrentHashMap<>();
    private final Map<String, List<String>> jobLogs = new ConcurrentHashMap<>();

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if Control-M integration service is running")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Control-M Integration",
            "timestamp", Instant.now().toString()
        ));
    }

    @PostMapping("/jobs/start")
    @Operation(summary = "Start a Control-M job", description = "Initialize and start a new job execution")
    @ApiResponse(responseCode = "200", description = "Job started successfully")
    public ResponseEntity<Map<String, Object>> startJob(
            @Parameter(description = "Name of the job to start", required = true)
            @RequestParam String jobName,
            @Parameter(description = "Optional custom job ID")
            @RequestParam(required = false) String jobId,
            @Parameter(description = "Job parameters")
            @RequestBody(required = false) Map<String, Object> parameters) {
        
        String executionId = jobId != null ? jobId : UUID.randomUUID().toString();
        
        logger.info("Starting Control-M job: {} with execution ID: {}", jobName, executionId);
        
        JobExecution execution = new JobExecution(
            executionId,
            jobName,
            "RUNNING",
            Instant.now(),
            null,
            parameters
        );
        
        jobExecutions.put(executionId, execution);
        addJobLog(executionId, "Job started: " + jobName);
        
        // Simulate job processing (in real scenarios, this would trigger actual work)
        simulateJobExecution(executionId, jobName, parameters);
        
        return ResponseEntity.ok(Map.of(
            "executionId", executionId,
            "jobName", jobName,
            "status", "STARTED",
            "timestamp", Instant.now().toString(),
            "message", "Job has been queued for execution"
        ));
    }

    @GetMapping("/jobs/{executionId}/status")
    @Operation(summary = "Get job status", description = "Retrieve the current status and details of a job execution")
    @ApiResponse(responseCode = "200", description = "Job status retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Job not found")
    public ResponseEntity<Map<String, Object>> getJobStatus(
            @Parameter(description = "Job execution ID", required = true)
            @PathVariable String executionId) {
        
        JobExecution execution = jobExecutions.get(executionId);
        
        if (execution == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(Map.of(
            "executionId", executionId,
            "jobName", execution.getJobName(),
            "status", execution.getStatus(),
            "startTime", execution.getStartTime().toString(),
            "endTime", execution.getEndTime() != null ? execution.getEndTime().toString() : null,
            "duration", calculateDuration(execution),
            "parameters", execution.getParameters() != null ? execution.getParameters() : Map.of()
        ));
    }

    @PostMapping("/jobs/{executionId}/complete")
    @Operation(summary = "Mark job as complete", description = "Manually mark a job as completed with a specific status")
    @ApiResponse(responseCode = "200", description = "Job marked as complete")
    @ApiResponse(responseCode = "404", description = "Job not found")
    public ResponseEntity<Map<String, Object>> completeJob(
            @Parameter(description = "Job execution ID", required = true)
            @PathVariable String executionId,
            @Parameter(description = "Final job status (SUCCESS, FAILED, CANCELLED)", required = true)
            @RequestParam String status,
            @Parameter(description = "Job execution results")
            @RequestBody(required = false) Map<String, Object> results) {
        
        JobExecution execution = jobExecutions.get(executionId);
        
        if (execution == null) {
            return ResponseEntity.notFound().build();
        }
        
        execution.setStatus(status.toUpperCase());
        execution.setEndTime(Instant.now());
        
        addJobLog(executionId, "Job completed with status: " + status);
        logger.info("Control-M job {} completed with status: {}", execution.getJobName(), status);
        
        return ResponseEntity.ok(Map.of(
            "executionId", executionId,
            "jobName", execution.getJobName(),
            "status", execution.getStatus(),
            "completedAt", execution.getEndTime().toString(),
            "duration", calculateDuration(execution),
            "results", results != null ? results : Map.of()
        ));
    }

    @GetMapping("/jobs")
    @Operation(summary = "List all jobs", description = "Retrieve a list of all job executions, optionally filtered by status")
    @ApiResponse(responseCode = "200", description = "Jobs retrieved successfully")
    public ResponseEntity<Map<String, Object>> listJobs(
            @Parameter(description = "Filter by job status (optional)")
            @RequestParam(required = false) String status) {
        
        List<Map<String, Object>> jobs = jobExecutions.values().stream()
            .filter(job -> status == null || job.getStatus().equalsIgnoreCase(status))
            .map(job -> Map.<String, Object>of(
                "executionId", job.getExecutionId(),
                "jobName", job.getJobName(),
                "status", job.getStatus(),
                "startTime", job.getStartTime().toString(),
                "duration", calculateDuration(job)
            ))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(Map.of(
            "jobs", jobs,
            "total", jobs.size(),
            "filter", status != null ? status : "ALL"
        ));
    }

    @GetMapping("/jobs/{executionId}/logs")
    @Operation(summary = "Get job logs", description = "Retrieve execution logs for a specific job")
    @ApiResponse(responseCode = "200", description = "Job logs retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Job not found")
    public ResponseEntity<Map<String, Object>> getJobLogs(
            @Parameter(description = "Job execution ID", required = true)
            @PathVariable String executionId) {
        
        List<String> logs = jobLogs.get(executionId);
        
        if (logs == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(Map.of(
            "executionId", executionId,
            "logs", logs,
            "logCount", logs.size()
        ));
    }

    @PostMapping("/batch/start")
    @Operation(summary = "Start batch jobs", description = "Start multiple jobs simultaneously")
    @ApiResponse(responseCode = "200", description = "Batch jobs started successfully")
    public ResponseEntity<Map<String, Object>> startBatchJobs(
            @Parameter(description = "List of jobs to start", required = true)
            @RequestBody List<BatchJobRequest> jobRequests) {
        
        List<Map<String, String>> startedJobs = new ArrayList<>();
        
        for (BatchJobRequest request : jobRequests) {
            String executionId = UUID.randomUUID().toString();
            
            JobExecution execution = new JobExecution(
                executionId,
                request.getJobName(),
                "RUNNING",
                Instant.now(),
                null,
                request.getParameters()
            );
            
            jobExecutions.put(executionId, execution);
            addJobLog(executionId, "Batch job started: " + request.getJobName());
            
            simulateJobExecution(executionId, request.getJobName(), request.getParameters());
            
            startedJobs.add(Map.of(
                "executionId", executionId,
                "jobName", request.getJobName(),
                "status", "STARTED"
            ));
        }
        
        logger.info("Started batch of {} Control-M jobs", jobRequests.size());
        
        return ResponseEntity.ok(Map.of(
            "batchId", UUID.randomUUID().toString(),
            "jobsStarted", startedJobs.size(),
            "jobs", startedJobs,
            "timestamp", Instant.now().toString()
        ));
    }

    @DeleteMapping("/jobs/{executionId}")
    @Operation(summary = "Cancel job", description = "Cancel a running job execution")
    @ApiResponse(responseCode = "200", description = "Job cancelled successfully")
    @ApiResponse(responseCode = "404", description = "Job not found")
    public ResponseEntity<Map<String, Object>> cancelJob(
            @Parameter(description = "Job execution ID", required = true)
            @PathVariable String executionId) {
        
        JobExecution execution = jobExecutions.get(executionId);
        
        if (execution == null) {
            return ResponseEntity.notFound().build();
        }
        
        execution.setStatus("CANCELLED");
        execution.setEndTime(Instant.now());
        addJobLog(executionId, "Job cancelled by user request");
        
        logger.info("Control-M job {} cancelled", execution.getJobName());
        
        return ResponseEntity.ok(Map.of(
            "executionId", executionId,
            "jobName", execution.getJobName(),
            "status", "CANCELLED",
            "message", "Job has been cancelled"
        ));
    }

    // Simulate job execution (in real scenarios, this would be actual business logic)
    private void simulateJobExecution(String executionId, String jobName, Map<String, Object> parameters) {
        new Thread(() -> {
            try {
                addJobLog(executionId, "Processing job: " + jobName);
                
                // Simulate some work based on job name
                int workDuration = calculateWorkDuration(jobName);
                Thread.sleep(workDuration);
                
                // Simulate success/failure based on job type
                boolean success = determineJobSuccess(jobName);
                
                JobExecution execution = jobExecutions.get(executionId);
                if (execution != null && !"CANCELLED".equals(execution.getStatus())) {
                    execution.setStatus(success ? "SUCCESS" : "FAILED");
                    execution.setEndTime(Instant.now());
                    addJobLog(executionId, "Job " + (success ? "completed successfully" : "failed"));
                    
                    // Add some simulated results
                    if (success) {
                        addJobLog(executionId, "Results: Processed " + new Random().nextInt(1000) + " records");
                    } else {
                        addJobLog(executionId, "Error: " + getSimulatedError(jobName));
                    }
                }
                
                logger.info("Simulated job {} completed with status: {}", jobName, success ? "SUCCESS" : "FAILED");
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                JobExecution execution = jobExecutions.get(executionId);
                if (execution != null) {
                    execution.setStatus("INTERRUPTED");
                    execution.setEndTime(Instant.now());
                    addJobLog(executionId, "Job was interrupted");
                }
            }
        }).start();
    }

    private int calculateWorkDuration(String jobName) {
        // Different job types have different durations for realistic simulation
        return switch (jobName.toLowerCase()) {
            case "quicktest", "healthcheck" -> 1000 + new Random().nextInt(1000); // 1-2 seconds
            case "dailyreport", "dataextract" -> 3000 + new Random().nextInt(2000); // 3-5 seconds
            case "datatransform", "dataload" -> 5000 + new Random().nextInt(3000); // 5-8 seconds
            default -> 2000 + new Random().nextInt(3000); // 2-5 seconds
        };
    }

    private boolean determineJobSuccess(String jobName) {
        // Different job types have different success rates for realistic simulation
        double successRate = switch (jobName.toLowerCase()) {
            case "healthcheck", "quicktest" -> 0.98; // 98% success
            case "dailyreport" -> 0.95; // 95% success
            case "dataextract", "datatransform" -> 0.90; // 90% success
            case "dataload" -> 0.85; // 85% success
            default -> 0.90; // 90% default success rate
        };
        
        return new Random().nextDouble() < successRate;
    }

    private String getSimulatedError(String jobName) {
        String[] errors = {
            "Connection timeout to external system",
            "Insufficient permissions for operation",
            "Data validation failed",
            "Resource temporarily unavailable",
            "Configuration error detected"
        };
        return errors[new Random().nextInt(errors.length)];
    }

    private void addJobLog(String executionId, String logMessage) {
        jobLogs.computeIfAbsent(executionId, k -> new ArrayList<>())
               .add(Instant.now() + ": " + logMessage);
    }

    private String calculateDuration(JobExecution execution) {
        if (execution.getEndTime() == null) {
            return Duration.between(execution.getStartTime(), Instant.now()).toSeconds() + "s (running)";
        }
        return Duration.between(execution.getStartTime(), execution.getEndTime()).toSeconds() + "s";
    }

    // Inner classes for data structures
    public static class JobExecution {
        private String executionId;
        private String jobName;
        private String status;
        private Instant startTime;
        private Instant endTime;
        private Map<String, Object> parameters;

        public JobExecution(String executionId, String jobName, String status, Instant startTime, Instant endTime, Map<String, Object> parameters) {
            this.executionId = executionId;
            this.jobName = jobName;
            this.status = status;
            this.startTime = startTime;
            this.endTime = endTime;
            this.parameters = parameters;
        }

        // Getters and setters
        public String getExecutionId() { return executionId; }
        public String getJobName() { return jobName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Instant getStartTime() { return startTime; }
        public Instant getEndTime() { return endTime; }
        public void setEndTime(Instant endTime) { this.endTime = endTime; }
        public Map<String, Object> getParameters() { return parameters; }
    }

    @Schema(description = "Request object for batch job execution")
    public static class BatchJobRequest {
        @Schema(description = "Name of the job to execute", required = true)
        private String jobName;
        
        @Schema(description = "Parameters for job execution")
        private Map<String, Object> parameters;

        // Default constructor
        public BatchJobRequest() {}

        public BatchJobRequest(String jobName, Map<String, Object> parameters) {
            this.jobName = jobName;
            this.parameters = parameters;
        }

        // Getters and setters
        public String getJobName() { return jobName; }
        public void setJobName(String jobName) { this.jobName = jobName; }
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    }
}
