package com.lithespeed.hellojava06.controller;

import com.lithespeed.hellojava06.dto.DialogRequestDTO;
import com.lithespeed.hellojava06.entity.Dialog;
import com.lithespeed.hellojava06.entity.DialogResponseDTO;
import com.lithespeed.hellojava06.service.DialogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dialogs")
@Tag(name = "Dialog Controller", description = "API for managing dialogs")
public class DialogController {

    private static final Logger logger = LoggerFactory.getLogger(DialogController.class);

    private final DialogService dialogService;

    @Autowired
    public DialogController(DialogService dialogService) {
        this.dialogService = dialogService;
    }

    @PostMapping("/get")
    @Operation(summary = "Get dialog by criteria", description = "Retrieve a dialog based on the provided search criteria (ID or request text)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dialog found or search completed"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<DialogResponseDTO> getDialog(
            @Parameter(description = "Dialog search criteria", required = true) @RequestBody @Valid DialogRequestDTO dialogRequest)
            throws Exception {

        logger.info("Received getDialog request: {}", dialogRequest);

        try {
            DialogResponseDTO response = dialogService.getDialogByIdAndRequest(dialogRequest.getId(),
                    dialogRequest.getRequest());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error processing getDialog request: {}", e.getMessage(), e);
            DialogResponseDTO errorResponse = new DialogResponseDTO(0, "Dialog not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    @GetMapping
    @Operation(summary = "Get all dialogs", description = "Retrieve all active dialogs ordered by priority and creation date")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all dialogs"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Dialog>> getAllDialogs() {
        logger.info("Received getAllDialogs request");

        try {
            List<Dialog> dialogs = dialogService.getAllDialogs();
            logger.info("Successfully retrieved {} dialogs", dialogs.size());
            return ResponseEntity.ok(dialogs);

        } catch (Exception e) {
            logger.error("Error retrieving all dialogs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}