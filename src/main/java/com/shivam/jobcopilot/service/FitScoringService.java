package com.shivam.jobcopilot.service;

import com.shivam.jobcopilot.entity.FitRequirement;
import com.shivam.jobcopilot.entity.RequirementEvidence;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculates fit from the persisted JD rubric and evidence statuses.
 * The model may suggest evidence, but it never owns the final score.
 */
@Service
public class FitScoringService {

    public static final String SCORING_VERSION = "REQUIREMENT_EVIDENCE_V5";

    public ScoringResult score(List<FitRequirement> requirements,
                               List<RequirementEvidence> evidence) {
        if (requirements == null || requirements.isEmpty()) {
            return new ScoringResult(0, "Insufficient evidence", "No structured JD requirements were available to score this role.");
        }

        Map<String, RequirementEvidence> evidenceByKey = new HashMap<>();
        if (evidence != null) {
            evidence.stream()
                    .filter(item -> item != null && item.getRequirementKey() != null)
                    .forEach(item -> evidenceByKey.put(item.getRequirementKey(), item));
        }

        double weightedScore = 0.0;
        int totalWeight = 0;
        for (FitRequirement requirement : requirements) {
            if (requirement == null) continue;
            int weight = requirement.getWeight() == null ? 0 : requirement.getWeight();
            if (weight <= 0) continue;
            totalWeight += weight;
            weightedScore += weight * evidencePoints(evidenceByKey.get(requirement.getRequirementKey()));
        }

        if (totalWeight == 0) {
            return new ScoringResult(0, "Insufficient evidence", "The role criteria did not contain usable scoring weights.");
        }

        int score = (int) Math.round(weightedScore / totalWeight * 100.0);
        return new ScoringResult(score, recommendation(score),
                "Required criteria carry the most weight. Unique responsibility signals and preferred criteria are lower-weight signals.");
    }

    public double evidencePoints(RequirementEvidence evidence) {
        if (evidence == null || evidence.getEvidenceStatus() == null) return 0.0;
        return switch (evidence.getEvidenceStatus().trim().toUpperCase()) {
            case "STRONG" -> 1.0;
            case "GOOD" -> 0.75;
            case "WEAK" -> 0.40;
            // Kept for analyses created before the evidence-level update.
            case "PARTIAL" -> 0.6;
            default -> 0.0;
        };
    }

    public String recommendation(int score) {
        if (score >= 80) return "Apply";
        if (score >= 60) return "Optimize & Apply";
        return "Ignore";
    }

    public record ScoringResult(int fitScore, String recommendation, String weightageReasoning) {}
}
