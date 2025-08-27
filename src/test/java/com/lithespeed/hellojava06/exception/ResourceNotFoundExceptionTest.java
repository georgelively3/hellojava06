package com.lithespeed.hellojava06.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourceNotFoundExceptionTest {

    @Test
    void testConstructorWithMessage() {
        // Given
        String message = "Resource not found";

        // When
        ResourceNotFoundException exception = new ResourceNotFoundException(message);

        // Then
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        // Given
        String message = "Resource not found";
        Throwable cause = new RuntimeException("Database connection failed");

        // When
        ResourceNotFoundException exception = new ResourceNotFoundException(message, cause);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testExceptionIsInstanceOfRuntimeException() {
        // Given & When
        ResourceNotFoundException exception = new ResourceNotFoundException("Test message");

        // Then
        assertInstanceOf(RuntimeException.class, exception);
    }
}
