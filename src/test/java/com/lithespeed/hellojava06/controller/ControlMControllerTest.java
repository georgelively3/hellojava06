package com.lithespeed.hellojava06.controller;

import com.lithespeed.hellojava06.service.ControlMService;
import com.lithespeed.hellojava06.service.ControlMService.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "control-m.api.base-url=http://localhost:2525"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ControlMControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ControlMService controlMService;

    @Test
    void healthCheck_ShouldReturnHealthyStatus() throws Exception {
        mockMvc.perform(get("/control-m/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("Control-M Integration"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.controlMApiUrl").exists());
    }

    @Test
    void startJob_WithValidRequest_ShouldReturnSuccess() throws Exception {
        // Given
        JobExecutionResult mockResult = new JobExecutionResult(
                "exec-123", "TestJob", "STARTED", "2025-01-15T14:00:00Z",
                "Job started successfully", "CTM_123", "5 minutes");

        when(controlMService.startJob(eq("TestJob"), isNull(), any()))
                .thenReturn(mockResult);

        // When & Then
        mockMvc.perform(post("/control-m/jobs/start")
                .param("jobName", "TestJob"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.executionId").value("exec-123"))
                .andExpect(jsonPath("$.jobName").value("TestJob"))
                .andExpect(jsonPath("$.status").value("STARTED"))
                .andExpect(jsonPath("$.controlMJobId").value("CTM_123"));
    }

    @Test
    void startJob_WithServiceException_ShouldReturnError() throws Exception {
        // Given
        when(controlMService.startJob(eq("FailJob"), isNull(), any()))
                .thenThrow(new ControlMJobException("Control-M API unavailable", null));

        // When & Then
        mockMvc.perform(post("/control-m/jobs/start")
                .param("jobName", "FailJob"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("CONTROL_M_ERROR"))
                .andExpect(jsonPath("$.message").value("Control-M API unavailable"));
    }

    @Test
    void getJobStatus_WithValidId_ShouldReturnStatus() throws Exception {
        // Given
        JobStatusResult mockResult = new JobStatusResult(
                "exec-123", "TestJob", "RUNNING", "2025-01-15T14:00:00Z",
                null, "120s (running)", Map.of("param1", "value1"));

        when(controlMService.getJobStatus("exec-123")).thenReturn(mockResult);

        // When & Then
        mockMvc.perform(get("/control-m/jobs/exec-123/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value("exec-123"))
                .andExpect(jsonPath("$.jobName").value("TestJob"))
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.startTime").value("2025-01-15T14:00:00Z"))
                .andExpect(jsonPath("$.endTime").isEmpty())
                .andExpect(jsonPath("$.duration").value("120s (running)"));
    }

    @Test
    void getJobStatus_WithInvalidId_ShouldReturn404() throws Exception {
        // Given
        when(controlMService.getJobStatus("invalid-id"))
                .thenThrow(new JobNotFoundException("Job not found"));

        // When & Then
        mockMvc.perform(get("/control-m/jobs/invalid-id/status"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listJobs_WithoutFilter_ShouldReturnAllJobs() throws Exception {
        // Given
        JobListResult mockResult = new JobListResult(
                List.of(
                        Map.of("executionId", "exec-1", "jobName", "Job1", "status", "RUNNING"),
                        Map.of("executionId", "exec-2", "jobName", "Job2", "status", "SUCCESS")),
                2,
                "ALL");

        when(controlMService.listJobs(null)).thenReturn(mockResult);

        // When & Then
        mockMvc.perform(get("/control-m/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs").isArray())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.filter").value("ALL"));
    }

    @Test
    void listJobs_WithStatusFilter_ShouldReturnFilteredJobs() throws Exception {
        // Given
        JobListResult mockResult = new JobListResult(
                List.of(Map.of("executionId", "exec-1", "jobName", "Job1", "status", "RUNNING")),
                1,
                "RUNNING");

        when(controlMService.listJobs("RUNNING")).thenReturn(mockResult);

        // When & Then
        mockMvc.perform(get("/control-m/jobs").param("status", "RUNNING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filter").value("RUNNING"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void getJobLogs_WithValidId_ShouldReturnLogs() throws Exception {
        // Given
        JobLogsResult mockResult = new JobLogsResult(
                "exec-123",
                List.of("Log line 1", "Log line 2"),
                2);

        when(controlMService.getJobLogs("exec-123")).thenReturn(mockResult);

        // When & Then
        mockMvc.perform(get("/control-m/jobs/exec-123/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value("exec-123"))
                .andExpect(jsonPath("$.logs").isArray())
                .andExpect(jsonPath("$.logCount").value(2));
    }

    @Test
    void cancelJob_WithValidId_ShouldReturnSuccess() throws Exception {
        // Given
        JobCancellationResult mockResult = new JobCancellationResult(
                "exec-123", "TestJob", "CANCELLED", "Job cancelled successfully");

        when(controlMService.cancelJob("exec-123")).thenReturn(mockResult);

        // When & Then
        mockMvc.perform(delete("/control-m/jobs/exec-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value("exec-123"))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.message").value("Job cancelled successfully"));
    }

    @Test
    void cancelJob_WithServiceException_ShouldReturnError() throws Exception {
        // Given
        when(controlMService.cancelJob("exec-123"))
                .thenThrow(new ControlMJobException("Failed to cancel in Control-M", null));

        // When & Then
        mockMvc.perform(delete("/control-m/jobs/exec-123"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("CONTROL_M_ERROR"));
    }

    @Test
    void startBatchJobs_WithValidRequest_ShouldReturnSuccess() throws Exception {
        // Given
        BatchJobResult mockResult = new BatchJobResult(
                "batch-123", 2,
                List.of(
                        Map.of("executionId", "exec-1", "jobName", "Job1", "status", "STARTED"),
                        Map.of("executionId", "exec-2", "jobName", "Job2", "status", "STARTED")),
                "2025-01-15T14:00:00Z");

        when(controlMService.startBatchJobs(any())).thenReturn(mockResult);

        // When & Then
        mockMvc.perform(post("/control-m/batch/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        [
                            {"jobName": "Job1", "parameters": {"param1": "value1"}},
                            {"jobName": "Job2", "parameters": {"param2": "value2"}}
                        ]
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").value("batch-123"))
                .andExpect(jsonPath("$.jobsStarted").value(2))
                .andExpect(jsonPath("$.jobs").isArray());
    }
}
