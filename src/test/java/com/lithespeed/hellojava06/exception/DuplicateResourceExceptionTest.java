package com.lithespeed.hellojava06.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DuplicateResourceExceptionTest {

    @Test
    void testConstructorWithMessage() {
        // Given
        String message = "Resource already exists";

        // When
        DuplicateResourceException exception = new DuplicateResourceException(message);

        // Then
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        // Given
        String message = "Resource already exists";
        Throwable cause = new RuntimeException("Database constraint violation");

        // When
        DuplicateResourceException exception = new DuplicateResourceException(message, cause);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testExceptionIsInstanceOfRuntimeException() {
        // Given & When
        DuplicateResourceException exception = new DuplicateResourceException("Test message");

        // Then
        assertInstanceOf(RuntimeException.class, exception);
    }
}
