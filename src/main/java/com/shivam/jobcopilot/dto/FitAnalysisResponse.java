package com.shivam.jobcopilot.dto;

import java.util.List;

public class FitAnalysisResponse {

    private int fitScore;
    private String recommendation;
    private String confidence;
    private SubScores subScores;
    private String weightageReasoning;
    private List<StrengthItem> strengthAlignment;
    private List<String> differentiation;
    private List<GapItem> gaps;
    private String positioningAngle;
    private List<CvAdjustmentItem> cvAdjustments;

    // --- Nested types (used for Jackson deserialization from LLM response) ---

    public record SubScores(
            SubScoreDetail skillsMatch,
            SubScoreDetail experienceMatch,
            SubScoreDetail domainMatch,
            SubScoreDetail impactMatch,
            SubScoreDetail cvPresentation
    ) {}

    public record SubScoreDetail(int score, int weight) {}

    public record StrengthItem(String strength, String category) {}

    public record GapItem(String gap, String category, String severity) {}

    public record CvAdjustmentItem(String adjustment, String priority, String addressesGap, String action, String cvPoint, String suggestedText) {}

    // --- Getters / Setters ---

    public int getFitScore() { return fitScore; }
    public void setFitScore(int fitScore) { this.fitScore = fitScore; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }

    public SubScores getSubScores() { return subScores; }
    public void setSubScores(SubScores subScores) { this.subScores = subScores; }

    public String getWeightageReasoning() { return weightageReasoning; }
    public void setWeightageReasoning(String weightageReasoning) { this.weightageReasoning = weightageReasoning; }

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
}
