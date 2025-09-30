package com.lithespeed.hellojava06.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DialogRequestDTOTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidDialogRequestDTO() {
        // Given
        DialogRequestDTO dto = new DialogRequestDTO(1, "Hello");

        // When
        Set<ConstraintViolation<DialogRequestDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.isEmpty(), "Valid DTO should have no validation errors");
        assertEquals(1, dto.getId(), "ID should be set correctly");
        assertEquals("Hello", dto.getRequest(), "Request should be set correctly");
    }

    @Test
    void testDefaultConstructor() {
        // When
        DialogRequestDTO dto = new DialogRequestDTO();

        // Then
        assertEquals(0, dto.getId(), "Default ID should be 0");
        assertNull(dto.getRequest(), "Default request should be null");
    }

    @Test
    void testParameterizedConstructor() {
        // When
        DialogRequestDTO dto = new DialogRequestDTO(42, "Test request");

        // Then
        assertEquals(42, dto.getId(), "ID should be set from constructor");
        assertEquals("Test request", dto.getRequest(), "Request should be set from constructor");
    }

    @Test
    void testSettersAndGetters() {
        // Given
        DialogRequestDTO dto = new DialogRequestDTO();

        // When
        dto.setId(5);
        dto.setRequest("Test message");

        // Then
        assertEquals(5, dto.getId(), "ID should be set via setter");
        assertEquals("Test message", dto.getRequest(), "Request should be set via setter");
    }

    @Test
    void testValidationWithNegativeId() {
        // Given
        DialogRequestDTO dto = new DialogRequestDTO(-1, "Hello");

        // When
        Set<ConstraintViolation<DialogRequestDTO>> violations = validator.validate(dto);

        // Then
        assertFalse(violations.isEmpty(), "Negative ID should cause validation error");
        assertEquals(1, violations.size(), "Should have exactly one violation");
        
        ConstraintViolation<DialogRequestDTO> violation = violations.iterator().next();
        assertEquals("id", violation.getPropertyPath().toString(), "Violation should be for id field");
        assertTrue(violation.getMessage().contains("non-negative"), "Message should mention non-negative requirement");
    }

    @Test
    void testValidationWithZeroId() {
        // Given
        DialogRequestDTO dto = new DialogRequestDTO(0, "Hello");

        // When
        Set<ConstraintViolation<DialogRequestDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.isEmpty(), "Zero ID should be valid (non-negative)");
    }

    @Test
    void testValidationWithLongRequest() {
        // Given
        String longRequest = "a".repeat(501); // Exceeds 500 character limit
        DialogRequestDTO dto = new DialogRequestDTO(1, longRequest);

        // When
        Set<ConstraintViolation<DialogRequestDTO>> violations = validator.validate(dto);

        // Then
        assertFalse(violations.isEmpty(), "Request longer than 500 characters should cause validation error");
        assertEquals(1, violations.size(), "Should have exactly one violation");
        
        ConstraintViolation<DialogRequestDTO> violation = violations.iterator().next();
        assertEquals("request", violation.getPropertyPath().toString(), "Violation should be for request field");
        assertTrue(violation.getMessage().contains("500"), "Message should mention 500 character limit");
    }

    @Test
    void testValidationWithMaxLengthRequest() {
        // Given
        String maxLengthRequest = "a".repeat(500); // Exactly 500 characters
        DialogRequestDTO dto = new DialogRequestDTO(1, maxLengthRequest);

        // When
        Set<ConstraintViolation<DialogRequestDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.isEmpty(), "Request with exactly 500 characters should be valid");
    }

    @Test
    void testValidationWithNullRequest() {
        // Given
        DialogRequestDTO dto = new DialogRequestDTO(1, null);

        // When
        Set<ConstraintViolation<DialogRequestDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.isEmpty(), "Null request should be valid (no @NotNull constraint)");
    }

    @Test
    void testValidationWithEmptyRequest() {
        // Given
        DialogRequestDTO dto = new DialogRequestDTO(1, "");

        // When
        Set<ConstraintViolation<DialogRequestDTO>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.isEmpty(), "Empty request should be valid");
    }

    @Test
    void testToString() {
        // Given
        DialogRequestDTO dto = new DialogRequestDTO(123, "Test request");

        // When
        String result = dto.toString();

        // Then
        assertNotNull(result, "toString should not return null");
        assertTrue(result.contains("123"), "toString should contain ID");
        assertTrue(result.contains("Test request"), "toString should contain request text");
        assertTrue(result.contains("DialogRequestDTO"), "toString should contain class name");
    }

    @Test
    void testToStringWithNullRequest() {
        // Given
        DialogRequestDTO dto = new DialogRequestDTO(456, null);

        // When
        String result = dto.toString();

        // Then
        assertNotNull(result, "toString should not return null even with null request");
        assertTrue(result.contains("456"), "toString should contain ID");
        assertTrue(result.contains("null"), "toString should handle null request gracefully");
    }

    @Test
    void testMultipleValidationViolations() {
        // Given
        String longRequest = "a".repeat(501);
        DialogRequestDTO dto = new DialogRequestDTO(-5, longRequest);

        // When
        Set<ConstraintViolation<DialogRequestDTO>> violations = validator.validate(dto);

        // Then
        assertEquals(2, violations.size(), "Should have violations for both negative ID and long request");
        
        boolean hasIdViolation = violations.stream()
                .anyMatch(v -> "id".equals(v.getPropertyPath().toString()));
        boolean hasRequestViolation = violations.stream()
                .anyMatch(v -> "request".equals(v.getPropertyPath().toString()));
        
        assertTrue(hasIdViolation, "Should have ID validation violation");
        assertTrue(hasRequestViolation, "Should have request validation violation");
    }

    @Test
    void testEqualityAndHashCode() {
        // Given
        DialogRequestDTO dto1 = new DialogRequestDTO(1, "Hello");
        DialogRequestDTO dto2 = new DialogRequestDTO(1, "Hello");

        // Note: DialogRequestDTO doesn't override equals/hashCode, so this tests Object.equals()
        // When & Then
        assertNotEquals(dto1, dto2, "Different instances should not be equal (no equals override)");
        assertNotEquals(dto1.hashCode(), dto2.hashCode(), "Different instances should have different hash codes");
    }
}