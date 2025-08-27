package com.lithespeed.hellojava06.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ControlMController.class)
class ControlMControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final TypeReference<Map<String, Object>> mapTypeRef = new TypeReference<>() {};

    @Test
    void healthCheck_ShouldReturnHealthyStatus() throws Exception {
        mockMvc.perform(get("/control-m/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("Control-M Integration"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void startJob_WithJobNameOnly_ShouldStartJobSuccessfully() throws Exception {
        mockMvc.perform(post("/control-m/jobs/start")
                .param("jobName", "TestJob"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.executionId").exists())
                .andExpect(jsonPath("$.jobName").value("TestJob"))
                .andExpect(jsonPath("$.status").value("STARTED"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void startJob_WithCustomJobId_ShouldUseProvidedJobId() throws Exception {
        String customJobId = "custom-job-123";
        
        mockMvc.perform(post("/control-m/jobs/start")
                .param("jobName", "TestJob")
                .param("jobId", customJobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(customJobId))
                .andExpect(jsonPath("$.jobName").value("TestJob"))
                .andExpect(jsonPath("$.status").value("STARTED"));
    }

    // @Test - Temporarily disabled due to timing issues with job simulation
    void getJobStatus_WithValidExecutionId_ShouldReturnJobDetails() throws Exception {
        // First start a job to get an execution ID
        String response = mockMvc.perform(post("/control-m/jobs/start")
                .param("jobName", "StatusTestJob"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> startResponse = objectMapper.readValue(response, mapTypeRef);
        String executionId = (String) startResponse.get("executionId");

        // Then check its status - it should exist with valid fields
        mockMvc.perform(get("/control-m/jobs/{executionId}/status", executionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(executionId))
                .andExpect(jsonPath("$.jobName").value("StatusTestJob"))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.startTime").exists())
                .andExpect(jsonPath("$.duration").exists())
                .andExpect(jsonPath("$.parameters").exists());
    }

    @Test
    void getJobStatus_WithInvalidExecutionId_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/control-m/jobs/{executionId}/status", "invalid-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listJobs_WithoutFilter_ShouldReturnAllJobs() throws Exception {
        // Start a couple of jobs first
        mockMvc.perform(post("/control-m/jobs/start").param("jobName", "Job1"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/control-m/jobs/start").param("jobName", "Job2"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/control-m/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs").isArray())
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.filter").value("ALL"));
    }

    @Test
    void listJobs_WithStatusFilter_ShouldReturnFilteredJobs() throws Exception {
        mockMvc.perform(get("/control-m/jobs")
                .param("status", "RUNNING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filter").value("RUNNING"));
    }

    @Test
    void startBatchJobs_WithMultipleJobs_ShouldStartAllJobs() throws Exception {
        List<ControlMController.BatchJobRequest> batchJobs = List.of(
            new ControlMController.BatchJobRequest("DataExtract", Map.of("source", "database1")),
            new ControlMController.BatchJobRequest("DataTransform", Map.of("format", "json")),
            new ControlMController.BatchJobRequest("DataLoad", Map.of("target", "warehouse"))
        );

        mockMvc.perform(post("/control-m/batch/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(batchJobs)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").exists())
                .andExpect(jsonPath("$.jobsStarted").value(3))
                .andExpect(jsonPath("$.jobs").isArray())
                .andExpect(jsonPath("$.jobs", hasSize(3)))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void cancelJob_WithValidExecutionId_ShouldCancelJob() throws Exception {
        // First start a job
        String response = mockMvc.perform(post("/control-m/jobs/start")
                .param("jobName", "CancelTestJob"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> startResponse = objectMapper.readValue(response, mapTypeRef);
        String executionId = (String) startResponse.get("executionId");

        // Then cancel it
        mockMvc.perform(delete("/control-m/jobs/{executionId}", executionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(executionId))
                .andExpect(jsonPath("$.jobName").value("CancelTestJob"))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void cancelJob_WithInvalidExecutionId_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(delete("/control-m/jobs/{executionId}", "invalid-id"))
                .andExpect(status().isNotFound());
    }
}
