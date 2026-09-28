package com.shivam.jobcopilot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_review_items", uniqueConstraints = @UniqueConstraint(
        name = "uk_email_review_user_message",
        columnNames = {"user_id", "source_message_id"}
))
public class EmailReviewItem {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(name = "source_message_id", nullable = false)
    private String sourceMessageId;

    private String companyNameRaw;
    private String companyNameCanonical;
    private String jobTitle;
    private String recruiterName;
    private String recruiterEmail;
    private String applicationStatus;
    private String referral;
    private String roleCategory;
    private String interviewDateAndTime;

    @Column(columnDefinition = "TEXT")
    private String updateSummary;

    @Column(columnDefinition = "TEXT")
    private String reviewReason;

    @Column(nullable = false)
    private String reviewStatus = "PENDING_REVIEW";

    private UUID resolvedApplicationId;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onPersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getSourceMessageId() { return sourceMessageId; }
    public void setSourceMessageId(String sourceMessageId) { this.sourceMessageId = sourceMessageId; }
    public String getCompanyNameRaw() { return companyNameRaw; }
    public void setCompanyNameRaw(String companyNameRaw) { this.companyNameRaw = companyNameRaw; }
    public String getCompanyNameCanonical() { return companyNameCanonical; }
    public void setCompanyNameCanonical(String companyNameCanonical) { this.companyNameCanonical = companyNameCanonical; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getRecruiterName() { return recruiterName; }
    public void setRecruiterName(String recruiterName) { this.recruiterName = recruiterName; }
    public String getRecruiterEmail() { return recruiterEmail; }
    public void setRecruiterEmail(String recruiterEmail) { this.recruiterEmail = recruiterEmail; }
    public String getApplicationStatus() { return applicationStatus; }
    public void setApplicationStatus(String applicationStatus) { this.applicationStatus = applicationStatus; }
    public String getReferral() { return referral; }
    public void setReferral(String referral) { this.referral = referral; }
    public String getRoleCategory() { return roleCategory; }
    public void setRoleCategory(String roleCategory) { this.roleCategory = roleCategory; }
    public String getInterviewDateAndTime() { return interviewDateAndTime; }
    public void setInterviewDateAndTime(String interviewDateAndTime) { this.interviewDateAndTime = interviewDateAndTime; }
    public String getUpdateSummary() { return updateSummary; }
    public void setUpdateSummary(String updateSummary) { this.updateSummary = updateSummary; }
    public String getReviewReason() { return reviewReason; }
    public void setReviewReason(String reviewReason) { this.reviewReason = reviewReason; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public UUID getResolvedApplicationId() { return resolvedApplicationId; }
    public void setResolvedApplicationId(UUID resolvedApplicationId) { this.resolvedApplicationId = resolvedApplicationId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
