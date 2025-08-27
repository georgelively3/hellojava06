package com.lithespeed.hellojava06.exception;

import com.lithespeed.hellojava06.exception.GlobalExceptionHandler.ErrorResponse;
import com.lithespeed.hellojava06.exception.GlobalExceptionHandler.ValidationErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
        webRequest = mock(WebRequest.class);
        when(webRequest.getDescription(false)).thenReturn("uri=/test");
    }

    @Test
    void testHandleResourceNotFoundException() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("User not found");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
                .handleResourceNotFoundException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("User not found", response.getBody().getMessage());
        assertEquals("uri=/test", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void testHandleDuplicateResourceException() {
        // Given
        DuplicateResourceException exception = new DuplicateResourceException("User already exists");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
                .handleDuplicateResourceException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("User already exists", response.getBody().getMessage());
        assertEquals("uri=/test", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void testHandleValidationExceptions() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("user", "email", "Email is required");
        
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        // When
        ResponseEntity<ValidationErrorResponse> response = globalExceptionHandler
                .handleValidationExceptions(exception, webRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Validation failed", response.getBody().getMessage());
        assertEquals("uri=/test", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
        
        Map<String, String> fieldErrors = response.getBody().getFieldErrors();
        assertEquals(1, fieldErrors.size());
        assertEquals("Email is required", fieldErrors.get("email"));
    }

    @Test
    void testHandleGlobalException() {
        // Given
        Exception exception = new RuntimeException("Unexpected error");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
                .handleGlobalException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
        assertEquals("uri=/test", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void testErrorResponseGettersAndSetters() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        ErrorResponse errorResponse = new ErrorResponse(404, "Not found", now, "/test");

        // When & Then
        assertEquals(404, errorResponse.getStatus());
        assertEquals("Not found", errorResponse.getMessage());
        assertEquals(now, errorResponse.getTimestamp());
        assertEquals("/test", errorResponse.getPath());

        // Test setters
        errorResponse.setStatus(500);
        errorResponse.setMessage("Internal error");
        errorResponse.setPath("/error");

        assertEquals(500, errorResponse.getStatus());
        assertEquals("Internal error", errorResponse.getMessage());
        assertEquals("/error", errorResponse.getPath());
    }

    @Test
    void testValidationErrorResponseGettersAndSetters() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        Map<String, String> fieldErrors = Map.of("email", "Email is required");
        ValidationErrorResponse response = new ValidationErrorResponse(
                400, "Validation failed", now, "/test", fieldErrors);

        // When & Then
        assertEquals(400, response.getStatus());
        assertEquals("Validation failed", response.getMessage());
        assertEquals(now, response.getTimestamp());
        assertEquals("/test", response.getPath());
        assertEquals(fieldErrors, response.getFieldErrors());

        // Test setter
        Map<String, String> newErrors = Map.of("name", "Name is required");
        response.setFieldErrors(newErrors);
        assertEquals(newErrors, response.getFieldErrors());
    }
}
