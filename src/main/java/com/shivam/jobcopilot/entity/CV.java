package com.shivam.jobcopilot.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cvs")
public class CV {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String contentText;

    private boolean isDefaultCv;

    private LocalDateTime createdAt;

    private UUID userId;

    public CV() {
    }

    public CV(UUID id, String name, String contentText, boolean isDefaultCv, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.contentText = contentText;
        this.isDefaultCv = isDefaultCv;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContentText() { return contentText; }
    public void setContentText(String contentText) { this.contentText = contentText; }

    public boolean isDefaultCv() { return isDefaultCv; }
    public void setDefaultCv(boolean isDefaultCv) { this.isDefaultCv = isDefaultCv; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
}
