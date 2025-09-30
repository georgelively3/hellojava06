package com.lithespeed.hellojava06.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for dialog search operations
 * This prevents mass assignment vulnerabilities by only exposing needed fields
 */
public class DialogRequestDTO {

    @Min(value = 0, message = "ID must be non-negative")
    private int id;

    @Size(max = 500, message = "Request text must not exceed 500 characters")
    private String request;

    // Default constructor
    public DialogRequestDTO() {
    }

    // Constructor
    public DialogRequestDTO(int id, String request) {
        this.id = id;
        this.request = request;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    @Override
    public String toString() {
        return "DialogRequestDTO{" +
                "id=" + id +
                ", request='" + request + '\'' +
                '}';
    }
}