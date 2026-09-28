package com.shivam.jobcopilot.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "user_gmail_processed_messages",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_gmail_processed_message",
                columnNames = {"user_id", "gmail_message_id"}
        )
)
public class UserGmailProcessedMessage {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "gmail_message_id", nullable = false)
    private String gmailMessageId;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    public UserGmailProcessedMessage() {}

    public UserGmailProcessedMessage(UUID userId, String gmailMessageId) {
        this.userId = userId;
        this.gmailMessageId = gmailMessageId;
        this.processedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getGmailMessageId() { return gmailMessageId; }
    public LocalDateTime getProcessedAt() { return processedAt; }
}
