package com.shivam.jobcopilot.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_gmail_tokens")
public class UserGmailToken {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(unique = true)
    private UUID userId;

    @Column(columnDefinition = "TEXT")
    private String accessToken;

    @Column(columnDefinition = "TEXT")
    private String refreshToken;

    private Long expiresAtEpochMs;

    private String gmailAddress;

    private LocalDateTime connectedAt;

    public UserGmailToken() {}

    @PrePersist
    protected void onCreate() {
        this.connectedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public Long getExpiresAtEpochMs() { return expiresAtEpochMs; }
    public void setExpiresAtEpochMs(Long expiresAtEpochMs) { this.expiresAtEpochMs = expiresAtEpochMs; }

    public String getGmailAddress() { return gmailAddress; }
    public void setGmailAddress(String gmailAddress) { this.gmailAddress = gmailAddress; }

    public LocalDateTime getConnectedAt() { return connectedAt; }
    public void setConnectedAt(LocalDateTime connectedAt) { this.connectedAt = connectedAt; }
}
