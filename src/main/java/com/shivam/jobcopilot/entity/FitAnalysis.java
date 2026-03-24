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
    private String jobTitle;

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

    @Column(columnDefinition = "TEXT")
    private String positioningAngle;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "fit_analysis_cv_adjustments", joinColumns = @JoinColumn(name = "fit_analysis_id"))
    private List<CvAdjustmentItem> cvAdjustments = new ArrayList<>();

    private LocalDateTime analyzedAt;

    private UUID userId;

    public FitAnalysis() {}

    @PrePersist
    protected void onCreate() {
        this.analyzedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

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

    public String getPositioningAngle() { return positioningAngle; }
    public void setPositioningAngle(String positioningAngle) { this.positioningAngle = positioningAngle; }

    public List<CvAdjustmentItem> getCvAdjustments() { return cvAdjustments; }
    public void setCvAdjustments(List<CvAdjustmentItem> cvAdjustments) { this.cvAdjustments = cvAdjustments; }

    public LocalDateTime getAnalyzedAt() { return analyzedAt; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
}
