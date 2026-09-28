package com.shivam.jobcopilot.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "job_applications")
public class JobApplication {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    private String company;

    @Column(name = "company_name_raw")
    private String companyNameRaw;

    @Column(name = "company_name_canonical")
    private String companyNameCanonical;

    private String jobTitle;

    private String recruiterName;

    private String recruiterEmail;

    private String applicationStatus;

    private String referral;

    private String roleCategory;

    private LocalDateTime interviewDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime snoozedUntil;

    // Set the first time status changes away from Applied/Referral Received — never overwritten
    private LocalDateTime firstRespondedAt;

    // Nullable — linked when a FitAnalysis is found matching this application's company + jobTitle
    private UUID fitAnalysisId;

    // Background JD-fit lifecycle for manually created applications.
    private String fitAnalysisStatus;

    @Column(columnDefinition = "TEXT")
    private String fitAnalysisError;

    // Nullable — CV used when applying for this role
    private UUID cvId;

    @Column(columnDefinition = "TEXT")
    private String jobDescriptionText;

    private String jobDescriptionUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Transient
    private FitSummary fitSummary;

    public record FitSummary(int fitScore, boolean isStale) {}

    @Column(nullable = false)
    private UUID userId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "application_updates", joinColumns = @JoinColumn(name = "application_id"))
    @OrderBy("timestamp ASC")
    private List<ApplicationUpdate> updates = new ArrayList<>();

    public JobApplication() {
    }

    @PreUpdate
    protected void onUpdate() {
        synchronizeCompanyNames();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onPersist() {
        synchronizeCompanyNames();
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PostLoad
    protected void onLoad() {
        synchronizeCompanyNames();
    }

    private void synchronizeCompanyNames() {
        if (!present(companyNameRaw)) companyNameRaw = company;
        if (!present(companyNameCanonical)) companyNameCanonical = companyNameRaw;
        if (!present(company)) company = present(companyNameCanonical) ? companyNameCanonical : companyNameRaw;
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCompany() {
        return present(company) ? company
                : present(companyNameRaw) ? companyNameRaw : companyNameCanonical;
    }

    public void setCompany(String company) {
        this.company = company;
        this.companyNameRaw = company;
        this.companyNameCanonical = company;
    }

    public String getCompanyNameRaw() { return companyNameRaw != null ? companyNameRaw : company; }
    public void setCompanyNameRaw(String companyNameRaw) { this.companyNameRaw = companyNameRaw; }

    public String getCompanyNameCanonical() {
        return companyNameCanonical != null ? companyNameCanonical : getCompany();
    }

    public void setCompanyNameCanonical(String companyNameCanonical) {
        this.companyNameCanonical = companyNameCanonical;
    }

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

    public LocalDateTime getInterviewDate() { return interviewDate; }
    public void setInterviewDate(LocalDateTime interviewDate) { this.interviewDate = interviewDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getSnoozedUntil() { return snoozedUntil; }
    public void setSnoozedUntil(LocalDateTime snoozedUntil) { this.snoozedUntil = snoozedUntil; }

    public LocalDateTime getFirstRespondedAt() { return firstRespondedAt; }
    public void setFirstRespondedAt(LocalDateTime firstRespondedAt) { this.firstRespondedAt = firstRespondedAt; }

    public UUID getFitAnalysisId() { return fitAnalysisId; }
    public void setFitAnalysisId(UUID fitAnalysisId) { this.fitAnalysisId = fitAnalysisId; }

    public String getFitAnalysisStatus() { return fitAnalysisStatus; }
    public void setFitAnalysisStatus(String fitAnalysisStatus) { this.fitAnalysisStatus = fitAnalysisStatus; }

    public String getFitAnalysisError() { return fitAnalysisError; }
    public void setFitAnalysisError(String fitAnalysisError) { this.fitAnalysisError = fitAnalysisError; }

    public UUID getCvId() { return cvId; }
    public void setCvId(UUID cvId) { this.cvId = cvId; }

    public String getJobDescriptionText() { return jobDescriptionText; }
    public void setJobDescriptionText(String jobDescriptionText) { this.jobDescriptionText = jobDescriptionText; }

    public String getJobDescriptionUrl() { return jobDescriptionUrl; }
    public void setJobDescriptionUrl(String jobDescriptionUrl) { this.jobDescriptionUrl = jobDescriptionUrl; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public FitSummary getFitSummary() { return fitSummary; }
    public void setFitSummary(FitSummary fitSummary) { this.fitSummary = fitSummary; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public List<ApplicationUpdate> getUpdates() { return updates; }
}
