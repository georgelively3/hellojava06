package com.lithespeed.hellojava06.entity;

/**
 * Data Transfer Object for Dialog responses
 * Simplified to contain only id and response
 */
public class DialogResponseDTO {

    private int id;
    private String response;

    // Constructor takes both id and response
    public DialogResponseDTO(int id, String response) {
        this.id = id;
        this.response = response;
    }

    // Getters only - no public setters
    public int getId() {
        return id;
    }

    public String getResponse() {
        return response;
    }

    @Override
    public String toString() {
        return "DialogResponseDTO{" +
                "id=" + id +
                ", response='" + response + '\'' +
                '}';
    }
}