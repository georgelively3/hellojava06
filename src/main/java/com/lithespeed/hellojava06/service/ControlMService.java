package com.lithespeed.hellojava06.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service layer for Control-M job management.
 * Handles business logic for job submission, status checking, and lifecycle
 * management.
 * Integrates with external Control-M API via HTTP calls.
 */
@Service
public class ControlMService {

    private static final Logger logger = LoggerFactory.getLogger(ControlMService.class);

    private final RestTemplate restTemplate;
    private final String controlMApiBaseUrl;

    // Business state management (in production this might be database-backed)
    private final Map<String, JobExecution> jobExecutions = new ConcurrentHashMap<>();
    private final Map<String, List<String>> jobLogs = new ConcurrentHashMap<>();

    public ControlMService(RestTemplate restTemplate,
            @Value("${control-m.api.base-url}") String controlMApiBaseUrl) {
        this.restTemplate = restTemplate;
        this.controlMApiBaseUrl = controlMApiBaseUrl;
    }

    /**
     * Submits a job to Control-M and tracks execution locally
     */
    public JobExecutionResult startJob(String jobName, String customJobId, Map<String, Object> parameters) {
        try {
            // Generate execution ID for internal tracking
            String executionId = customJobId != null ? customJobId : UUID.randomUUID().toString();

            logger.info("Starting Control-M job: {} with execution ID: {}", jobName, executionId);

            // Submit job to external Control-M API
            Map<String, Object> submitRequest = createSubmitRequest(jobName, parameters);
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> controlMResponse = restTemplate.postForEntity(
                    controlMApiBaseUrl + "/control-m/jobs/submit",
                    submitRequest,
                    Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> controlMResult = controlMResponse.getBody();
            if (controlMResult == null) {
                throw new ControlMJobException("Empty response from Control-M API", null);
            }
            String controlMJobId = (String) controlMResult.get("controlMJobId");

            // Create local job execution record
            JobExecution execution = new JobExecution(
                    executionId,
                    jobName,
                    "RUNNING",
                    Instant.now(),
                    null,
                    parameters,
                    controlMJobId);

            jobExecutions.put(executionId, execution);
            addJobLog(executionId, "Job submitted to Control-M with ID: " + controlMJobId);

            // Return business result
            return new JobExecutionResult(
                    executionId,
                    jobName,
                    "STARTED",
                    Instant.now().toString(),
                    "Job has been queued for execution",
                    controlMJobId,
                    (String) controlMResult.get("estimatedDuration"));

        } catch (Exception e) {
            logger.error("Failed to start Control-M job: {}", jobName, e);
            throw new ControlMJobException("Failed to submit job to Control-M: " + e.getMessage(), e);
        }
    }

    /**
     * Gets current job status from Control-M and updates local tracking
     */
    public JobStatusResult getJobStatus(String executionId) {
        JobExecution execution = jobExecutions.get(executionId);

        if (execution == null) {
            throw new JobNotFoundException("Job execution not found: " + executionId);
        }

        try {
            // Get status from Control-M if job is still running
            if ("RUNNING".equals(execution.getStatus()) && execution.getControlMJobId() != null) {
                updateJobStatusFromControlM(execution);
            }

            return createJobStatusResult(execution);

        } catch (Exception e) {
            logger.warn("Failed to get status from Control-M for job: {}, using local status",
                    executionId, e);
            return createJobStatusResult(execution);
        }
    }

    /**
     * Cancels a running job in Control-M
     */
    public JobCancellationResult cancelJob(String executionId) {
        JobExecution execution = jobExecutions.get(executionId);

        if (execution == null) {
            throw new JobNotFoundException("Job execution not found: " + executionId);
        }

        try {
            // Cancel in Control-M if it has a Control-M job ID
            if (execution.getControlMJobId() != null) {
                Map<String, Object> cancelRequest = Map.of("reason", "User requested cancellation");
                restTemplate.postForEntity(
                        controlMApiBaseUrl + "/control-m/jobs/" + execution.getControlMJobId() + "/cancel",
                        cancelRequest,
                        Map.class);
            }

            // Update local status
            execution.setStatus("CANCELLED");
            execution.setEndTime(Instant.now());
            addJobLog(executionId, "Job cancelled by user request");

            logger.info("Control-M job {} cancelled", execution.getJobName());

            return new JobCancellationResult(
                    executionId,
                    execution.getJobName(),
                    "CANCELLED",
                    "Job has been cancelled");

        } catch (Exception e) {
            logger.error("Failed to cancel Control-M job: {}", executionId, e);
            throw new ControlMJobException("Failed to cancel job in Control-M: " + e.getMessage(), e);
        }
    }

    /**
     * Lists all job executions with optional status filtering
     */
    public JobListResult listJobs(String statusFilter) {
        List<Map<String, Object>> jobs = jobExecutions.values().stream()
                .filter(job -> statusFilter == null || job.getStatus().equalsIgnoreCase(statusFilter))
                .map(this::createJobSummary)
                .toList();

        return new JobListResult(jobs, jobs.size(), statusFilter != null ? statusFilter : "ALL");
    }

    /**
     * Gets execution logs for a job
     */
    public JobLogsResult getJobLogs(String executionId) {
        if (!jobExecutions.containsKey(executionId)) {
            throw new JobNotFoundException("Job execution not found: " + executionId);
        }

        List<String> logs = jobLogs.getOrDefault(executionId, List.of());
        return new JobLogsResult(executionId, logs, logs.size());
    }

    /**
     * Starts multiple jobs as a batch operation
     */
    public BatchJobResult startBatchJobs(List<BatchJobRequest> jobRequests) {
        List<Map<String, String>> startedJobs = new ArrayList<>();

        for (BatchJobRequest request : jobRequests) {
            try {
                JobExecutionResult result = startJob(request.getJobName(), null, request.getParameters());
                startedJobs.add(Map.of(
                        "executionId", result.getExecutionId(),
                        "jobName", request.getJobName(),
                        "status", "STARTED"));
            } catch (Exception e) {
                logger.error("Failed to start batch job: {}", request.getJobName(), e);
                startedJobs.add(Map.of(
                        "executionId", "",
                        "jobName", request.getJobName(),
                        "status", "FAILED",
                        "error", e.getMessage()));
            }
        }

        logger.info("Started batch of {} Control-M jobs", jobRequests.size());

        return new BatchJobResult(
                UUID.randomUUID().toString(),
                startedJobs.size(),
                startedJobs,
                Instant.now().toString());
    }

    // Private helper methods for business logic

    private Map<String, Object> createSubmitRequest(String jobName, Map<String, Object> parameters) {
        Map<String, Object> request = new HashMap<>();
        request.put("jobName", jobName);
        if (parameters != null && !parameters.isEmpty()) {
            request.put("parameters", parameters);
        }
        return request;
    }

    private void updateJobStatusFromControlM(JobExecution execution) {
        try {
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    controlMApiBaseUrl + "/control-m/jobs/" + execution.getControlMJobId() + "/status",
                    Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> statusData = response.getBody();
            if (statusData == null) {
                logger.warn("Empty status response from Control-M for job: {}", execution.getControlMJobId());
                return;
            }

            String controlMStatus = (String) statusData.get("status");

            // Map Control-M status to our internal status
            if ("SUCCESS".equals(controlMStatus)) {
                execution.setStatus("SUCCESS");
                execution.setEndTime(Instant.now());
                addJobLog(execution.getExecutionId(), "Job completed successfully in Control-M");
            } else if ("FAILED".equals(controlMStatus)) {
                execution.setStatus("FAILED");
                execution.setEndTime(Instant.now());
                addJobLog(execution.getExecutionId(), "Job failed in Control-M");
            }
            // Keep status as RUNNING if still in progress

        } catch (Exception e) {
            logger.warn("Failed to get status from Control-M for job: {}",
                    execution.getControlMJobId(), e);
        }
    }

    private JobStatusResult createJobStatusResult(JobExecution execution) {
        return new JobStatusResult(
                execution.getExecutionId(),
                execution.getJobName(),
                execution.getStatus(),
                execution.getStartTime().toString(),
                execution.getEndTime() != null ? execution.getEndTime().toString() : null,
                calculateDuration(execution),
                execution.getParameters() != null ? execution.getParameters() : Map.of());
    }

    private Map<String, Object> createJobSummary(JobExecution job) {
        return Map.of(
                "executionId", job.getExecutionId(),
                "jobName", job.getJobName(),
                "status", job.getStatus(),
                "startTime", job.getStartTime().toString(),
                "duration", calculateDuration(job));
    }

    private String calculateDuration(JobExecution execution) {
        if (execution.getEndTime() == null) {
            return java.time.Duration.between(execution.getStartTime(), Instant.now()).toSeconds() + "s (running)";
        }
        return java.time.Duration.between(execution.getStartTime(), execution.getEndTime()).toSeconds() + "s";
    }

    private void addJobLog(String executionId, String logMessage) {
        jobLogs.computeIfAbsent(executionId, k -> new ArrayList<>())
                .add(Instant.now() + ": " + logMessage);
    }

    // Inner classes for data structures

    public static class JobExecution {
        private String executionId;
        private String jobName;
        private String status;
        private Instant startTime;
        private Instant endTime;
        private Map<String, Object> parameters;
        private String controlMJobId; // External Control-M job ID

        public JobExecution(String executionId, String jobName, String status, Instant startTime,
                Instant endTime, Map<String, Object> parameters, String controlMJobId) {
            this.executionId = executionId;
            this.jobName = jobName;
            this.status = status;
            this.startTime = startTime;
            this.endTime = endTime;
            this.parameters = parameters;
            this.controlMJobId = controlMJobId;
        }

        // Getters and setters
        public String getExecutionId() {
            return executionId;
        }

        public String getJobName() {
            return jobName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Instant getStartTime() {
            return startTime;
        }

        public Instant getEndTime() {
            return endTime;
        }

        public void setEndTime(Instant endTime) {
            this.endTime = endTime;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public String getControlMJobId() {
            return controlMJobId;
        }
    }

    // Result classes for business operations
    public static class JobExecutionResult {
        private final String executionId;
        private final String jobName;
        private final String status;
        private final String timestamp;
        private final String message;
        private final String controlMJobId;
        private final String estimatedDuration;

        public JobExecutionResult(String executionId, String jobName, String status, String timestamp,
                String message, String controlMJobId, String estimatedDuration) {
            this.executionId = executionId;
            this.jobName = jobName;
            this.status = status;
            this.timestamp = timestamp;
            this.message = message;
            this.controlMJobId = controlMJobId;
            this.estimatedDuration = estimatedDuration;
        }

        // Getters
        public String getExecutionId() {
            return executionId;
        }

        public String getJobName() {
            return jobName;
        }

        public String getStatus() {
            return status;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getMessage() {
            return message;
        }

        public String getControlMJobId() {
            return controlMJobId;
        }

        public String getEstimatedDuration() {
            return estimatedDuration;
        }
    }

    public static class JobStatusResult {
        private final String executionId;
        private final String jobName;
        private final String status;
        private final String startTime;
        private final String endTime;
        private final String duration;
        private final Map<String, Object> parameters;

        public JobStatusResult(String executionId, String jobName, String status, String startTime,
                String endTime, String duration, Map<String, Object> parameters) {
            this.executionId = executionId;
            this.jobName = jobName;
            this.status = status;
            this.startTime = startTime;
            this.endTime = endTime;
            this.duration = duration;
            this.parameters = parameters;
        }

        // Getters
        public String getExecutionId() {
            return executionId;
        }

        public String getJobName() {
            return jobName;
        }

        public String getStatus() {
            return status;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public String getDuration() {
            return duration;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }
    }

    public static class JobCancellationResult {
        private final String executionId;
        private final String jobName;
        private final String status;
        private final String message;

        public JobCancellationResult(String executionId, String jobName, String status, String message) {
            this.executionId = executionId;
            this.jobName = jobName;
            this.status = status;
            this.message = message;
        }

        // Getters
        public String getExecutionId() {
            return executionId;
        }

        public String getJobName() {
            return jobName;
        }

        public String getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class JobListResult {
        private final List<Map<String, Object>> jobs;
        private final int total;
        private final String filter;

        public JobListResult(List<Map<String, Object>> jobs, int total, String filter) {
            this.jobs = jobs;
            this.total = total;
            this.filter = filter;
        }

        // Getters
        public List<Map<String, Object>> getJobs() {
            return jobs;
        }

        public int getTotal() {
            return total;
        }

        public String getFilter() {
            return filter;
        }
    }

    public static class JobLogsResult {
        private final String executionId;
        private final List<String> logs;
        private final int logCount;

        public JobLogsResult(String executionId, List<String> logs, int logCount) {
            this.executionId = executionId;
            this.logs = logs;
            this.logCount = logCount;
        }

        // Getters
        public String getExecutionId() {
            return executionId;
        }

        public List<String> getLogs() {
            return logs;
        }

        public int getLogCount() {
            return logCount;
        }
    }

    public static class BatchJobResult {
        private final String batchId;
        private final int jobsStarted;
        private final List<Map<String, String>> jobs;
        private final String timestamp;

        public BatchJobResult(String batchId, int jobsStarted, List<Map<String, String>> jobs, String timestamp) {
            this.batchId = batchId;
            this.jobsStarted = jobsStarted;
            this.jobs = jobs;
            this.timestamp = timestamp;
        }

        // Getters
        public String getBatchId() {
            return batchId;
        }

        public int getJobsStarted() {
            return jobsStarted;
        }

        public List<Map<String, String>> getJobs() {
            return jobs;
        }

        public String getTimestamp() {
            return timestamp;
        }
    }

    public static class BatchJobRequest {
        private String jobName;
        private Map<String, Object> parameters;

        // Default constructor
        public BatchJobRequest() {
        }

        public BatchJobRequest(String jobName, Map<String, Object> parameters) {
            this.jobName = jobName;
            this.parameters = parameters;
        }

        // Getters and setters
        public String getJobName() {
            return jobName;
        }

        public void setJobName(String jobName) {
            this.jobName = jobName;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }
    }

    // Custom exceptions
    public static class ControlMJobException extends RuntimeException {
        public ControlMJobException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class JobNotFoundException extends RuntimeException {
        public JobNotFoundException(String message) {
            super(message);
        }
    }
}
