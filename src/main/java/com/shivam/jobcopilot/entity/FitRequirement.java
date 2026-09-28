package com.shivam.jobcopilot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class FitRequirement {

    @Column(name = "requirement_key")
    private String requirementKey;

    @Column(name = "requirement_text", columnDefinition = "TEXT")
    private String requirementText;

    @Column(name = "capability_phrase")
    private String capabilityPhrase;

    @Column(name = "importance_tier")
    private String importanceTier;

    @Column(name = "relevance_mode")
    private String relevanceMode;

    @Column(name = "evidence_type")
    private String evidenceType;

    @Column(name = "source_excerpt", columnDefinition = "TEXT")
    private String sourceExcerpt;

    /** Normalized percentage contribution to the deterministic fit score. */
    @Column(name = "weight")
    private Integer weight;

    public FitRequirement() {}

    public FitRequirement(String requirementKey, String requirementText, String capabilityPhrase,
                          String importanceTier, String relevanceMode, String evidenceType,
                          String sourceExcerpt) {
        this.requirementKey = requirementKey;
        this.requirementText = requirementText;
        this.capabilityPhrase = capabilityPhrase;
        this.importanceTier = importanceTier;
        this.relevanceMode = relevanceMode;
        this.evidenceType = evidenceType;
        this.sourceExcerpt = sourceExcerpt;
    }

    public String getRequirementKey() { return requirementKey; }
    public void setRequirementKey(String requirementKey) { this.requirementKey = requirementKey; }

    public String getRequirementText() { return requirementText; }
    public void setRequirementText(String requirementText) { this.requirementText = requirementText; }

    public String getCapabilityPhrase() { return capabilityPhrase; }
    public void setCapabilityPhrase(String capabilityPhrase) { this.capabilityPhrase = capabilityPhrase; }

    public String getImportanceTier() { return importanceTier; }
    public void setImportanceTier(String importanceTier) { this.importanceTier = importanceTier; }

    public String getRelevanceMode() { return relevanceMode; }
    public void setRelevanceMode(String relevanceMode) { this.relevanceMode = relevanceMode; }

    public String getEvidenceType() { return evidenceType; }
    public void setEvidenceType(String evidenceType) { this.evidenceType = evidenceType; }

    public String getSourceExcerpt() { return sourceExcerpt; }
    public void setSourceExcerpt(String sourceExcerpt) { this.sourceExcerpt = sourceExcerpt; }

    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }
}
