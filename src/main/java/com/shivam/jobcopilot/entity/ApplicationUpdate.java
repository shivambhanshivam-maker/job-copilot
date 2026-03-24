package com.shivam.jobcopilot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDateTime;

@Embeddable
public class ApplicationUpdate {

    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT")
    private String summary;

    public ApplicationUpdate() {}

    public ApplicationUpdate(LocalDateTime timestamp, String summary) {
        this.timestamp = timestamp;
        this.summary = summary;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public String getSummary() { return summary; }
}