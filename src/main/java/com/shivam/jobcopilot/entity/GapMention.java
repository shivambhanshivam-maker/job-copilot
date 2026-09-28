package com.shivam.jobcopilot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "gap_mentions")
public class GapMention {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID fitAnalysisId;

    private UUID applicationId;

    private String company;
    private String jobTitle;
    private String roleCategory;

    @Column(columnDefinition = "TEXT")
    private String rawGapText;

    private String rawCategory;
    private String rawSeverity;

    private String normalizationStatus = "PENDING";

    @Column(columnDefinition = "TEXT")
    private String capabilityPhrase;

    @Column(columnDefinition = "TEXT")
    private String domainContext;

    @Column(columnDefinition = "TEXT")
    private String artifact;

    @Column(columnDefinition = "TEXT")
    private String evidenceType;

    private UUID conceptId;
    private String conceptName;
    private Double mappingConfidence;

    private LocalDateTime createdAt;

    public GapMention() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getFitAnalysisId() { return fitAnalysisId; }
    public void setFitAnalysisId(UUID fitAnalysisId) { this.fitAnalysisId = fitAnalysisId; }

    public UUID getApplicationId() { return applicationId; }
    public void setApplicationId(UUID applicationId) { this.applicationId = applicationId; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getRoleCategory() { return roleCategory; }
    public void setRoleCategory(String roleCategory) { this.roleCategory = roleCategory; }

    public String getRawGapText() { return rawGapText; }
    public void setRawGapText(String rawGapText) { this.rawGapText = rawGapText; }

    public String getRawCategory() { return rawCategory; }
    public void setRawCategory(String rawCategory) { this.rawCategory = rawCategory; }

    public String getRawSeverity() { return rawSeverity; }
    public void setRawSeverity(String rawSeverity) { this.rawSeverity = rawSeverity; }

    public String getNormalizationStatus() { return normalizationStatus; }
    public void setNormalizationStatus(String normalizationStatus) { this.normalizationStatus = normalizationStatus; }

    public String getCapabilityPhrase() { return capabilityPhrase; }
    public void setCapabilityPhrase(String capabilityPhrase) { this.capabilityPhrase = capabilityPhrase; }

    public String getDomainContext() { return domainContext; }
    public void setDomainContext(String domainContext) { this.domainContext = domainContext; }

    public String getArtifact() { return artifact; }
    public void setArtifact(String artifact) { this.artifact = artifact; }

    public String getEvidenceType() { return evidenceType; }
    public void setEvidenceType(String evidenceType) { this.evidenceType = evidenceType; }

    public UUID getConceptId() { return conceptId; }
    public void setConceptId(UUID conceptId) { this.conceptId = conceptId; }

    public String getConceptName() { return conceptName; }
    public void setConceptName(String conceptName) { this.conceptName = conceptName; }

    public Double getMappingConfidence() { return mappingConfidence; }
    public void setMappingConfidence(Double mappingConfidence) { this.mappingConfidence = mappingConfidence; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
