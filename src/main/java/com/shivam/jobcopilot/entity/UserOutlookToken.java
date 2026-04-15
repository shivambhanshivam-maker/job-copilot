package com.shivam.jobcopilot.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_outlook_tokens")
public class UserOutlookToken {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(unique = true)
    private UUID userId;

    // Encrypted serialized MSAL4J token cache (used for refresh)
    @Column(columnDefinition = "TEXT")
    private String tokenCacheData;

    // Encrypted access token — cached for fast path to avoid rebuilding MSAL app every poll
    @Column(columnDefinition = "TEXT")
    private String cachedAccessToken;

    // Expiry of cachedAccessToken — used for fast path check
    private Long expiresAtEpochMs;

    private String outlookAddress;

    private LocalDateTime connectedAt;

    public UserOutlookToken() {}

    @PrePersist
    protected void onCreate() {
        this.connectedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getTokenCacheData() { return tokenCacheData; }
    public void setTokenCacheData(String tokenCacheData) { this.tokenCacheData = tokenCacheData; }

    public String getCachedAccessToken() { return cachedAccessToken; }
    public void setCachedAccessToken(String cachedAccessToken) { this.cachedAccessToken = cachedAccessToken; }

    public Long getExpiresAtEpochMs() { return expiresAtEpochMs; }
    public void setExpiresAtEpochMs(Long expiresAtEpochMs) { this.expiresAtEpochMs = expiresAtEpochMs; }

    public String getOutlookAddress() { return outlookAddress; }
    public void setOutlookAddress(String outlookAddress) { this.outlookAddress = outlookAddress; }

    public LocalDateTime getConnectedAt() { return connectedAt; }
    public void setConnectedAt(LocalDateTime connectedAt) { this.connectedAt = connectedAt; }
}