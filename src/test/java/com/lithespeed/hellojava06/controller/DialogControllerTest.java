package com.lithespeed.hellojava06.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lithespeed.hellojava06.dto.DialogRequestDTO;
import com.lithespeed.hellojava06.entity.Dialog;
import com.lithespeed.hellojava06.dto.DialogResponseDTO;
import com.lithespeed.hellojava06.service.DialogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DialogController.class)
class DialogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DialogService dialogService;

    @Autowired
    private ObjectMapper objectMapper;

    private DialogRequestDTO testRequest;
    private DialogResponseDTO testResponse;
    private List<Dialog> testDialogs;

    @BeforeEach
    void setUp() {
        testRequest = new DialogRequestDTO(1, "Hello");
        testResponse = new DialogResponseDTO(1, "Hello there!");
        testDialogs = Arrays.asList(
                new Dialog(1, "Hello", "Hello there!"),
                new Dialog(2, "How are you?", "I'm doing well!"),
                new Dialog(3, "Goodbye", "See you later!")
        );
    }

    @Test
    void testGetDialogWithValidRequest() throws Exception {
        // Given
        when(dialogService.getDialogByIdAndRequest(1, "Hello")).thenReturn(testResponse);

        // When & Then
        mockMvc.perform(post("/dialogs/get")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.response").value("Hello there!"));

        verify(dialogService, times(1)).getDialogByIdAndRequest(1, "Hello");
    }

    @Test
    void testGetDialogWithServiceException() throws Exception {
        // Given
        when(dialogService.getDialogByIdAndRequest(anyInt(), anyString()))
                .thenThrow(new Exception("Dialog not found"));

        // When & Then
        mockMvc.perform(post("/dialogs/get")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(0))
                .andExpect(jsonPath("$.response").value("Dialog not found"));

        verify(dialogService, times(1)).getDialogByIdAndRequest(1, "Hello");
    }

    @Test
    void testGetDialogWithInvalidJson() throws Exception {
        // When & Then
        mockMvc.perform(post("/dialogs/get")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().isBadRequest());

        verify(dialogService, never()).getDialogByIdAndRequest(anyInt(), anyString());
    }

    @Test
    void testGetDialogWithValidationErrors() throws Exception {
        // Given - Invalid request with negative ID
        DialogRequestDTO invalidRequest = new DialogRequestDTO(-1, "Test");

        // When & Then
        mockMvc.perform(post("/dialogs/get")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(dialogService, never()).getDialogByIdAndRequest(anyInt(), anyString());
    }

    @Test
    void testGetDialogWithRequestTooLong() throws Exception {
        // Given - Request with text longer than 500 characters
        String longText = "a".repeat(501);
        DialogRequestDTO invalidRequest = new DialogRequestDTO(1, longText);

        // When & Then
        mockMvc.perform(post("/dialogs/get")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(dialogService, never()).getDialogByIdAndRequest(anyInt(), anyString());
    }

    @Test
    void testGetDialogWithZeroId() throws Exception {
        // Given
        DialogRequestDTO zeroIdRequest = new DialogRequestDTO(0, "Test");
        DialogResponseDTO zeroIdResponse = new DialogResponseDTO(1, "Found by text");
        when(dialogService.getDialogByIdAndRequest(0, "Test")).thenReturn(zeroIdResponse);

        // When & Then
        mockMvc.perform(post("/dialogs/get")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(zeroIdRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.response").value("Found by text"));

        verify(dialogService, times(1)).getDialogByIdAndRequest(0, "Test");
    }

    @Test
    void testGetDialogWithNullRequest() throws Exception {
        // Given
        DialogRequestDTO nullRequestText = new DialogRequestDTO(1, null);
        when(dialogService.getDialogByIdAndRequest(1, null)).thenReturn(testResponse);

        // When & Then
        mockMvc.perform(post("/dialogs/get")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nullRequestText)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.response").value("Hello there!"));

        verify(dialogService, times(1)).getDialogByIdAndRequest(1, null);
    }

    @Test
    void testGetAllDialogs() throws Exception {
        // Given
        when(dialogService.getAllDialogs()).thenReturn(testDialogs);

        // When & Then
        mockMvc.perform(get("/dialogs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].request").value("Hello"))
                .andExpect(jsonPath("$[0].response").value("Hello there!"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].request").value("How are you?"))
                .andExpect(jsonPath("$[1].response").value("I'm doing well!"));

        verify(dialogService, times(1)).getAllDialogs();
    }

    @Test
    void testGetAllDialogsWithEmptyList() throws Exception {
        // Given
        when(dialogService.getAllDialogs()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/dialogs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(dialogService, times(1)).getAllDialogs();
    }

    @Test
    void testGetAllDialogsWithServiceException() throws Exception {
        // Given
        when(dialogService.getAllDialogs()).thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(get("/dialogs"))
                .andExpect(status().isInternalServerError());

        verify(dialogService, times(1)).getAllDialogs();
    }

    @Test
    void testGetDialogEndpointPath() throws Exception {
        // Given
        when(dialogService.getDialogByIdAndRequest(1, "Hello")).thenReturn(testResponse);

        // When & Then - Test exact endpoint path
        mockMvc.perform(post("/dialogs/get")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk());

        // Verify wrong path returns 404
        mockMvc.perform(post("/dialogs/getDialog")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetDialogWithDifferentMediaType() throws Exception {
        // When & Then - Test with unsupported media type
        mockMvc.perform(post("/dialogs/get")
                .contentType(MediaType.TEXT_PLAIN)
                .content("plain text"))
                .andExpect(status().isUnsupportedMediaType());

        verify(dialogService, never()).getDialogByIdAndRequest(anyInt(), anyString());
    }

    @Test
    void testGetAllDialogsWithWrongHttpMethod() throws Exception {
        // When & Then - POST to GET endpoint should return method not allowed
        mockMvc.perform(post("/dialogs"))
                .andExpect(status().isMethodNotAllowed());

        verify(dialogService, never()).getAllDialogs();
    }

    @Test
    void testGetDialogResponseStructure() throws Exception {
        // Given
        DialogResponseDTO complexResponse = new DialogResponseDTO(42, "Complex response with special characters: !@#$%");
        when(dialogService.getDialogByIdAndRequest(42, "Complex")).thenReturn(complexResponse);

        DialogRequestDTO complexRequest = new DialogRequestDTO(42, "Complex");

        // When & Then
        mockMvc.perform(post("/dialogs/get")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(complexRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.response").value("Complex response with special characters: !@#$%"));

        verify(dialogService, times(1)).getDialogByIdAndRequest(42, "Complex");
    }
}