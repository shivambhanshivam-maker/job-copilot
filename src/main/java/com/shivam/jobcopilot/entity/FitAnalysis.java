package com.shivam.jobcopilot.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "fit_analyses")
public class FitAnalysis {

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

    private String roleCategory;

    @Column(columnDefinition = "TEXT")
    private String jobDescriptionText;

    private UUID cvId;

    private int fitScore;

    private String recommendation;
    private String confidence;

    @Column(columnDefinition = "TEXT")
    private String weightageReasoning;

    // Sub-scores
    private Integer skillsMatchScore;
    private Integer skillsMatchWeight;
    private Integer experienceMatchScore;
    private Integer experienceMatchWeight;
    private Integer domainMatchScore;
    private Integer domainMatchWeight;
    private Integer impactMatchScore;
    private Integer impactMatchWeight;
    private Integer cvPresentationScore;
    private Integer cvPresentationWeight;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "fit_analysis_strengths", joinColumns = @JoinColumn(name = "fit_analysis_id"))
    private List<StrengthItem> strengthAlignment = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "fit_analysis_differentiation", joinColumns = @JoinColumn(name = "fit_analysis_id"))
    @Column(name = "differentiation_point", columnDefinition = "TEXT")
    private List<String> differentiation = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "fit_analysis_gaps", joinColumns = @JoinColumn(name = "fit_analysis_id"))
    private List<GapItem> gaps = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "fit_analysis_requirements", joinColumns = @JoinColumn(name = "fit_analysis_id"))
    private List<FitRequirement> jdRequirements = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "fit_analysis_requirement_evidence", joinColumns = @JoinColumn(name = "fit_analysis_id"))
    private List<RequirementEvidence> requirementEvidence = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String positioningAngle;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "fit_analysis_cv_adjustments", joinColumns = @JoinColumn(name = "fit_analysis_id"))
    private List<CvAdjustmentItem> cvAdjustments = new ArrayList<>();

    private LocalDateTime analyzedAt;

    private UUID userId;

    @Column(name = "previous_analysis_id")
    private UUID previousAnalysisId;

    @Column(name = "revision_number")
    private Integer revisionNumber;

    @Column(name = "cv_content_hash", length = 64)
    private String cvContentHash;

    @Column(name = "cv_text_snapshot", columnDefinition = "TEXT")
    private String cvTextSnapshot;

    @Column(name = "jd_content_hash", length = 64)
    private String jdContentHash;

    @Column(name = "scoring_version", length = 64)
    private String scoringVersion;


    public FitAnalysis() {}

    @PrePersist
    protected void onCreate() {
        synchronizeCompanyNames();
        this.analyzedAt = LocalDateTime.now();
        if (this.revisionNumber == null) this.revisionNumber = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        synchronizeCompanyNames();
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

    public String getCompany() {
        return present(companyNameRaw) ? companyNameRaw
                : present(company) ? company : companyNameCanonical;
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

    public String getRoleCategory() { return roleCategory; }
    public void setRoleCategory(String roleCategory) { this.roleCategory = roleCategory; }

    public String getJobDescriptionText() { return jobDescriptionText; }
    public void setJobDescriptionText(String jobDescriptionText) { this.jobDescriptionText = jobDescriptionText; }

    public UUID getCvId() { return cvId; }
    public void setCvId(UUID cvId) { this.cvId = cvId; }

    public int getFitScore() { return fitScore; }
    public void setFitScore(int fitScore) { this.fitScore = fitScore; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }

    public String getWeightageReasoning() { return weightageReasoning; }
    public void setWeightageReasoning(String weightageReasoning) { this.weightageReasoning = weightageReasoning; }

    public Integer getSkillsMatchScore() { return skillsMatchScore; }
    public void setSkillsMatchScore(Integer skillsMatchScore) { this.skillsMatchScore = skillsMatchScore; }

    public Integer getSkillsMatchWeight() { return skillsMatchWeight; }
    public void setSkillsMatchWeight(Integer skillsMatchWeight) { this.skillsMatchWeight = skillsMatchWeight; }

    public Integer getExperienceMatchScore() { return experienceMatchScore; }
    public void setExperienceMatchScore(Integer experienceMatchScore) { this.experienceMatchScore = experienceMatchScore; }

    public Integer getExperienceMatchWeight() { return experienceMatchWeight; }
    public void setExperienceMatchWeight(Integer experienceMatchWeight) { this.experienceMatchWeight = experienceMatchWeight; }

    public Integer getDomainMatchScore() { return domainMatchScore; }
    public void setDomainMatchScore(Integer domainMatchScore) { this.domainMatchScore = domainMatchScore; }

    public Integer getDomainMatchWeight() { return domainMatchWeight; }
    public void setDomainMatchWeight(Integer domainMatchWeight) { this.domainMatchWeight = domainMatchWeight; }

    public Integer getImpactMatchScore() { return impactMatchScore; }
    public void setImpactMatchScore(Integer impactMatchScore) { this.impactMatchScore = impactMatchScore; }

    public Integer getImpactMatchWeight() { return impactMatchWeight; }
    public void setImpactMatchWeight(Integer impactMatchWeight) { this.impactMatchWeight = impactMatchWeight; }

    public Integer getCvPresentationScore() { return cvPresentationScore; }
    public void setCvPresentationScore(Integer cvPresentationScore) { this.cvPresentationScore = cvPresentationScore; }

    public Integer getCvPresentationWeight() { return cvPresentationWeight; }
    public void setCvPresentationWeight(Integer cvPresentationWeight) { this.cvPresentationWeight = cvPresentationWeight; }

    public List<StrengthItem> getStrengthAlignment() { return strengthAlignment; }
    public void setStrengthAlignment(List<StrengthItem> strengthAlignment) { this.strengthAlignment = strengthAlignment; }

    public List<String> getDifferentiation() { return differentiation; }
    public void setDifferentiation(List<String> differentiation) { this.differentiation = differentiation; }

    public List<GapItem> getGaps() { return gaps; }
    public void setGaps(List<GapItem> gaps) { this.gaps = gaps; }

    public List<FitRequirement> getJdRequirements() { return jdRequirements; }
    public void setJdRequirements(List<FitRequirement> jdRequirements) { this.jdRequirements = jdRequirements; }

    public List<RequirementEvidence> getRequirementEvidence() { return requirementEvidence; }
    public void setRequirementEvidence(List<RequirementEvidence> requirementEvidence) { this.requirementEvidence = requirementEvidence; }

    public String getPositioningAngle() { return positioningAngle; }
    public void setPositioningAngle(String positioningAngle) { this.positioningAngle = positioningAngle; }

    public List<CvAdjustmentItem> getCvAdjustments() { return cvAdjustments; }
    public void setCvAdjustments(List<CvAdjustmentItem> cvAdjustments) { this.cvAdjustments = cvAdjustments; }

    public LocalDateTime getAnalyzedAt() { return analyzedAt; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getPreviousAnalysisId() { return previousAnalysisId; }
    public void setPreviousAnalysisId(UUID previousAnalysisId) { this.previousAnalysisId = previousAnalysisId; }

    public Integer getRevisionNumber() { return revisionNumber; }
    public void setRevisionNumber(Integer revisionNumber) { this.revisionNumber = revisionNumber; }

    public String getCvContentHash() { return cvContentHash; }
    public void setCvContentHash(String cvContentHash) { this.cvContentHash = cvContentHash; }

    public String getCvTextSnapshot() { return cvTextSnapshot; }
    public void setCvTextSnapshot(String cvTextSnapshot) { this.cvTextSnapshot = cvTextSnapshot; }

    public String getJdContentHash() { return jdContentHash; }
    public void setJdContentHash(String jdContentHash) { this.jdContentHash = jdContentHash; }

    public String getScoringVersion() { return scoringVersion; }
    public void setScoringVersion(String scoringVersion) { this.scoringVersion = scoringVersion; }

}
