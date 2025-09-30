package com.lithespeed.hellojava06.entity;

import com.fasterxml.jackson.annotation.JsonView;

public class Dialog {

    public interface RequestView {
    }

    public interface ResponseView {
    }

    public interface RequestResponseView {
    }

    @JsonView({ RequestView.class, ResponseView.class, RequestResponseView.class })
    private int id;

    @JsonView({ RequestView.class, RequestResponseView.class })
    private String request;

    @JsonView({ ResponseView.class, RequestResponseView.class })
    private String response;

    // Default constructor
    public Dialog() {
    }

    // Constructor for creating dialogs with data
    public Dialog(int id, String request, String response) {
        this.id = id;
        this.request = request;
        this.response = response;
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

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    @Override
    public String toString() {
        return "Dialog{" +
                "id=" + id +
                ", request='" + request + '\'' +
                ", response='" + response + '\'' +
                '}';
    }
}