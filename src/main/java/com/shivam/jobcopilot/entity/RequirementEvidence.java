package com.shivam.jobcopilot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class RequirementEvidence {

    @Column(name = "requirement_key")
    private String requirementKey;

    @Column(name = "evidence_status")
    private String evidenceStatus;

    @Column(name = "evidence_type")
    private String evidenceType;

    @Column(name = "evidence_text", columnDefinition = "TEXT")
    private String evidenceText;

    @Column(name = "artifact", columnDefinition = "TEXT")
    private String artifact;

    @Column(name = "confidence")
    private String confidence;

    public RequirementEvidence() {}

    public RequirementEvidence(String requirementKey, String evidenceStatus, String evidenceType,
                               String evidenceText, String artifact, String confidence) {
        this.requirementKey = requirementKey;
        this.evidenceStatus = evidenceStatus;
        this.evidenceType = evidenceType;
        this.evidenceText = evidenceText;
        this.artifact = artifact;
        this.confidence = confidence;
    }

    public String getRequirementKey() { return requirementKey; }
    public void setRequirementKey(String requirementKey) { this.requirementKey = requirementKey; }

    public String getEvidenceStatus() { return evidenceStatus; }
    public void setEvidenceStatus(String evidenceStatus) { this.evidenceStatus = evidenceStatus; }

    public String getEvidenceType() { return evidenceType; }
    public void setEvidenceType(String evidenceType) { this.evidenceType = evidenceType; }

    public String getEvidenceText() { return evidenceText; }
    public void setEvidenceText(String evidenceText) { this.evidenceText = evidenceText; }

    public String getArtifact() { return artifact; }
    public void setArtifact(String artifact) { this.artifact = artifact; }

    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
}
