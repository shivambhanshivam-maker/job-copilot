package com.shivam.jobcopilot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_consents")
public class StudentConsent {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "affiliation_id", nullable = false, unique = true)
    private StudentSchoolAffiliation affiliation;

    @Column(nullable = false)
    private UUID userId;

    private boolean advisorVisibilityEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column
    private AdvisorVisibilityLevel advisorVisibilityLevel = AdvisorVisibilityLevel.NONE;

    private LocalDateTime consentedAt;

    private LocalDateTime revokedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public StudentConsent() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }

    public StudentSchoolAffiliation getAffiliation() { return affiliation; }
    public void setAffiliation(StudentSchoolAffiliation affiliation) { this.affiliation = affiliation; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public boolean isAdvisorVisibilityEnabled() { return advisorVisibilityEnabled; }
    public void setAdvisorVisibilityEnabled(boolean advisorVisibilityEnabled) { this.advisorVisibilityEnabled = advisorVisibilityEnabled; }

    public AdvisorVisibilityLevel getAdvisorVisibilityLevel() {
        if (advisorVisibilityLevel == null) {
            return advisorVisibilityEnabled ? AdvisorVisibilityLevel.FULL : AdvisorVisibilityLevel.NONE;
        }
        return advisorVisibilityLevel;
    }
    public void setAdvisorVisibilityLevel(AdvisorVisibilityLevel advisorVisibilityLevel) { this.advisorVisibilityLevel = advisorVisibilityLevel; }

    public boolean allowsAdvisorVisibility() {
        return revokedAt == null && getAdvisorVisibilityLevel() != AdvisorVisibilityLevel.NONE;
    }

    public LocalDateTime getConsentedAt() { return consentedAt; }
    public void setConsentedAt(LocalDateTime consentedAt) { this.consentedAt = consentedAt; }

    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
