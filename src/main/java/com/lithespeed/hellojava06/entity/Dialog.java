package com.lithespeed.hellojava06.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dialog entity representing a conversation dialog with request/response pairs.
 * Uses Lombok to reduce boilerplate code and @JsonIgnoreProperties for Fortify SAST compliance.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true, allowSetters = false)
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
}