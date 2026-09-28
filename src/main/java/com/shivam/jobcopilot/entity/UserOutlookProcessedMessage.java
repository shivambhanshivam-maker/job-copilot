package com.shivam.jobcopilot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "user_outlook_processed_messages",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_outlook_processed_message",
                columnNames = {"user_id", "outlook_message_id"}
        )
)
public class UserOutlookProcessedMessage {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "outlook_message_id", nullable = false)
    private String outlookMessageId;

    private LocalDateTime processedAt;

    public UserOutlookProcessedMessage() {}

    public UserOutlookProcessedMessage(UUID userId, String outlookMessageId) {
        this.userId = userId;
        this.outlookMessageId = outlookMessageId;
    }

    @PrePersist
    protected void onCreate() {
        processedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getOutlookMessageId() { return outlookMessageId; }
    public void setOutlookMessageId(String outlookMessageId) { this.outlookMessageId = outlookMessageId; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
