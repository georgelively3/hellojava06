package com.lithespeed.hellojava06.repository;

import com.lithespeed.hellojava06.entity.Dialog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DialogRepositoryTest {

    private DialogRepository dialogRepository;

    @BeforeEach
    void setUp() {
        dialogRepository = new DialogRepository();
    }

    @Test
    void testConstructorLoadsSevenDialogs() {
        // When
        List<Dialog> dialogs = dialogRepository.findAll();

        // Then
        assertEquals(7, dialogs.size(), "Repository should contain exactly 7 dialogs");
    }

    @Test
    void testFindAllReturnsAllDialogs() {
        // When
        List<Dialog> dialogs = dialogRepository.findAll();

        // Then
        assertNotNull(dialogs, "Dialog list should not be null");
        assertEquals(7, dialogs.size(), "Should return all 7 dialogs");
        
        // Verify first dialog matches expected data
        Dialog firstDialog = dialogs.get(0);
        assertEquals(1, firstDialog.getId(), "First dialog should have ID 1");
        assertEquals("Hello", firstDialog.getRequest(), "First dialog request should be 'Hello'");
        assertEquals("Hello", firstDialog.getResponse(), "First dialog response should be 'Hello'");
    }

    @Test
    void testFindAllReturnsNewListInstance() {
        // When
        List<Dialog> dialogs1 = dialogRepository.findAll();
        List<Dialog> dialogs2 = dialogRepository.findAll();

        // Then
        assertNotSame(dialogs1, dialogs2, "Should return new list instances to prevent external modification");
        assertEquals(dialogs1.size(), dialogs2.size(), "Both lists should have same size");
    }

    @Test
    void testFindByIdAndRequestWithValidId() {
        // When
        Optional<Dialog> result = dialogRepository.findByIdAndRequest(1, null);

        // Then
        assertTrue(result.isPresent(), "Should find dialog with ID 1");
        assertEquals(1, result.get().getId(), "Found dialog should have ID 1");
        assertEquals("Hello", result.get().getRequest(), "Found dialog should have correct request");
    }

    @Test
    void testFindByIdAndRequestWithValidIdAndMatchingRequest() {
        // When
        Optional<Dialog> result = dialogRepository.findByIdAndRequest(2, "How are you?");

        // Then
        assertTrue(result.isPresent(), "Should find dialog with ID 2 and matching request");
        assertEquals(2, result.get().getId(), "Found dialog should have ID 2");
        assertEquals("How are you?", result.get().getRequest(), "Found dialog should have correct request");
    }

    @Test
    void testFindByIdAndRequestWithValidIdAndNonMatchingRequest() {
        // When
        Optional<Dialog> result = dialogRepository.findByIdAndRequest(1, "Goodbye");

        // Then
        assertFalse(result.isPresent(), "Should not find dialog with ID 1 and non-matching request");
    }

    @Test
    void testFindByIdAndRequestWithInvalidId() {
        // When
        Optional<Dialog> result = dialogRepository.findByIdAndRequest(999, null);

        // Then
        assertFalse(result.isPresent(), "Should not find dialog with non-existent ID");
    }

    @Test
    void testFindByIdAndRequestWithPartialTextMatch() {
        // When - Search by ID 1 with partial text match
        Optional<Dialog> result = dialogRepository.findByIdAndRequest(1, "hello");

        // Then
        assertTrue(result.isPresent(), "Should find dialog with ID 1 and partial text match (case insensitive)");
        assertEquals(1, result.get().getId(), "Found dialog should have ID 1");
    }

    @Test
    void testFindByIdAndRequestWithCaseInsensitiveMatch() {
        // When - Search by ID 1 with case insensitive match
        Optional<Dialog> result = dialogRepository.findByIdAndRequest(1, "HELLO");

        // Then
        assertTrue(result.isPresent(), "Should find dialog with ID 1 and case insensitive match");
        assertEquals(1, result.get().getId(), "Found dialog should have ID 1");
    }

    @Test
    void testFindByIdAndRequestWithNullRequest() {
        // When
        Optional<Dialog> result = dialogRepository.findByIdAndRequest(3, null);

        // Then
        assertTrue(result.isPresent(), "Should find dialog by ID when request is null");
        assertEquals(3, result.get().getId(), "Found dialog should have ID 3");
    }

    @Test
    void testFindByIdAndRequestWithEmptyRequest() {
        // When
        Optional<Dialog> result = dialogRepository.findByIdAndRequest(1, "");

        // Then
        assertTrue(result.isPresent(), "Should find dialog by ID when request is empty (empty string contains in any string)");
        assertEquals(1, result.get().getId(), "Found dialog should have ID 1");
    }

    @Test
    void testFindByIdAndRequestSearchByTextOnly() {
        // Note: The current implementation requires ID match, so we test with existing IDs
        // When - Search for "How" which should match dialog ID 2 or 5
        Optional<Dialog> result1 = dialogRepository.findByIdAndRequest(2, "How");
        Optional<Dialog> result2 = dialogRepository.findByIdAndRequest(5, "How");

        // Then
        assertTrue(result1.isPresent(), "Should find dialog ID 2 with 'How' in request");
        assertEquals(2, result1.get().getId(), "First result should have ID 2");
        
        assertTrue(result2.isPresent(), "Should find dialog ID 5 with 'How' in request");
        assertEquals(5, result2.get().getId(), "Second result should have ID 5");
    }

    @Test
    void testRepositoryContainsExpectedDialogs() {
        // When
        List<Dialog> dialogs = dialogRepository.findAll();

        // Then
        assertEquals(7, dialogs.size(), "Should have exactly 7 dialogs");
        
        // Verify specific dialogs exist
        assertTrue(dialogs.stream().anyMatch(d -> d.getId() == 1 && "Hello".equals(d.getRequest())), 
                   "Should contain Hello dialog");
        assertTrue(dialogs.stream().anyMatch(d -> d.getId() == 2 && "How are you?".equals(d.getRequest())), 
                   "Should contain How are you dialog");
        assertTrue(dialogs.stream().anyMatch(d -> d.getId() == 7 && "Thank you".equals(d.getRequest())), 
                   "Should contain Thank you dialog");
    }
}