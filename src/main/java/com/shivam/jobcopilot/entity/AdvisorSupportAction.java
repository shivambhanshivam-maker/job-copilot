package com.shivam.jobcopilot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "advisor_support_actions", uniqueConstraints = @UniqueConstraint(
        name = "uk_advisor_support_action_signal",
        columnNames = {"advisor_user_id", "student_user_id", "signal_type", "signal_fingerprint"}
))
public class AdvisorSupportAction {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private UUID advisorUserId;

    @Column(nullable = false)
    private UUID studentUserId;

    @Column(nullable = false)
    private String signalType;

    @Column(nullable = false, length = 100)
    private String signalFingerprint;

    private LocalDateTime reviewedAt;

    private LocalDateTime snoozedUntil;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public AdvisorSupportAction() {}

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }

    public UUID getAdvisorUserId() { return advisorUserId; }
    public void setAdvisorUserId(UUID advisorUserId) { this.advisorUserId = advisorUserId; }

    public UUID getStudentUserId() { return studentUserId; }
    public void setStudentUserId(UUID studentUserId) { this.studentUserId = studentUserId; }

    public String getSignalType() { return signalType; }
    public void setSignalType(String signalType) { this.signalType = signalType; }

    public String getSignalFingerprint() { return signalFingerprint; }
    public void setSignalFingerprint(String signalFingerprint) { this.signalFingerprint = signalFingerprint; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public LocalDateTime getSnoozedUntil() { return snoozedUntil; }
    public void setSnoozedUntil(LocalDateTime snoozedUntil) { this.snoozedUntil = snoozedUntil; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
