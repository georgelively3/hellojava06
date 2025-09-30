package com.lithespeed.hellojava06.service;

import com.lithespeed.hellojava06.entity.Dialog;
import com.lithespeed.hellojava06.dto.DialogResponseDTO;
import com.lithespeed.hellojava06.repository.DialogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DialogServiceTest {

    @Mock
    private DialogRepository dialogRepository;

    @InjectMocks
    private DialogService dialogService;

    private Dialog testDialog;
    private List<Dialog> testDialogs;

    @BeforeEach
    void setUp() {
        testDialog = new Dialog(1, "Hello", "Hello there!");
        testDialogs = Arrays.asList(
                new Dialog(1, "Hello", "Hello there!"),
                new Dialog(2, "How are you?", "I'm doing well!"),
                new Dialog(3, "Goodbye", "See you later!"));
    }

    @Test
    void testGetAllDialogs() {
        // Given
        when(dialogRepository.findAll()).thenReturn(testDialogs);

        // When
        List<Dialog> result = dialogService.getAllDialogs();

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals(3, result.size(), "Should return all 3 dialogs");
        assertEquals(testDialogs, result, "Should return the same dialogs from repository");

        verify(dialogRepository, times(1)).findAll();
    }

    @Test
    void testGetDialogByIdAndRequestWithExistingDialog() throws Exception {
        // Given
        when(dialogRepository.findByIdAndRequest(1, "Hello")).thenReturn(Optional.of(testDialog));

        // When
        DialogResponseDTO result = dialogService.getDialogByIdAndRequest(1, "Hello");

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.getId(), "Response should have correct ID");
        assertEquals("Hello there!", result.getResponse(), "Response should have correct response text");

        verify(dialogRepository, times(1)).findByIdAndRequest(1, "Hello");
    }

    @Test
    void testGetDialogByIdAndRequestWithNonExistentDialog() {
        // Given
        when(dialogRepository.findByIdAndRequest(999, "NonExistent")).thenReturn(Optional.empty());

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            dialogService.getDialogByIdAndRequest(999, "NonExistent");
        });

        assertEquals("Dialog not found", exception.getMessage(), "Should throw exception with correct message");
        verify(dialogRepository, times(1)).findByIdAndRequest(999, "NonExistent");
    }

    @Test
    void testGetDialogByIdAndRequestWithNullRequest() throws Exception {
        // Given
        when(dialogRepository.findByIdAndRequest(1, null)).thenReturn(Optional.of(testDialog));

        // When
        DialogResponseDTO result = dialogService.getDialogByIdAndRequest(1, null);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.getId(), "Response should have correct ID");
        assertEquals("Hello there!", result.getResponse(), "Response should have correct response text");

        verify(dialogRepository, times(1)).findByIdAndRequest(1, null);
    }

    @Test
    void testGetDialogByIdAndRequestWithZeroId() throws Exception {
        // Given
        when(dialogRepository.findByIdAndRequest(0, "Hello")).thenReturn(Optional.of(testDialog));

        // When
        DialogResponseDTO result = dialogService.getDialogByIdAndRequest(0, "Hello");

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.getId(), "Response should have correct ID");
        assertEquals("Hello there!", result.getResponse(), "Response should have correct response text");

        verify(dialogRepository, times(1)).findByIdAndRequest(0, "Hello");
    }

    @Test
    void testGetAllDialogsWithEmptyRepository() {
        // Given
        when(dialogRepository.findAll()).thenReturn(Arrays.asList());

        // When
        List<Dialog> result = dialogService.getAllDialogs();

        // Then
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Result should be empty");

        verify(dialogRepository, times(1)).findAll();
    }

    @Test
    void testServiceCallsRepositoryOnce() {
        // Given
        when(dialogRepository.findAll()).thenReturn(testDialogs);

        // When
        dialogService.getAllDialogs();
        dialogService.getAllDialogs();

        // Then
        verify(dialogRepository, times(2)).findAll();
    }

    @Test
    void testGetDialogByIdAndRequestWithValidDialogContainsAllFields() throws Exception {
        // Given
        Dialog complexDialog = new Dialog(5, "Complex question", "Complex answer with details");
        when(dialogRepository.findByIdAndRequest(5, "Complex")).thenReturn(Optional.of(complexDialog));

        // When
        DialogResponseDTO result = dialogService.getDialogByIdAndRequest(5, "Complex");

        // Then
        assertNotNull(result, "Result should not be null");
        assertEquals(5, result.getId(), "Should preserve dialog ID");
        assertEquals("Complex answer with details", result.getResponse(), "Should preserve dialog response");

        verify(dialogRepository, times(1)).findByIdAndRequest(5, "Complex");
    }

    @Test
    void testRepositoryInteractionWithDifferentParameters() throws Exception {
        // Given
        Dialog dialog1 = new Dialog(1, "Test1", "Response1");
        Dialog dialog2 = new Dialog(2, "Test2", "Response2");

        when(dialogRepository.findByIdAndRequest(1, "Test1")).thenReturn(Optional.of(dialog1));
        when(dialogRepository.findByIdAndRequest(2, "Test2")).thenReturn(Optional.of(dialog2));

        // When
        DialogResponseDTO result1 = dialogService.getDialogByIdAndRequest(1, "Test1");
        DialogResponseDTO result2 = dialogService.getDialogByIdAndRequest(2, "Test2");

        // Then
        assertEquals(1, result1.getId(), "First result should have ID 1");
        assertEquals("Response1", result1.getResponse(), "First result should have correct response");

        assertEquals(2, result2.getId(), "Second result should have ID 2");
        assertEquals("Response2", result2.getResponse(), "Second result should have correct response");

        verify(dialogRepository, times(1)).findByIdAndRequest(1, "Test1");
        verify(dialogRepository, times(1)).findByIdAndRequest(2, "Test2");
    }
}