package com.shivam.jobcopilot.service;

import com.shivam.jobcopilot.entity.FitRequirement;
import com.shivam.jobcopilot.entity.RequirementEvidence;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FitScoringServiceTest {

    private final FitScoringService service = new FitScoringService();

    @Test
    void scoresCoreRequirementsMoreHeavilyThanSupportingAndPreferredRequirements() {
        FitRequirement core = requirement("REQ-1", "CORE", 50);
        FitRequirement supporting = requirement("REQ-2", "SUPPORTING", 33);
        FitRequirement preferred = requirement("REQ-3", "PREFERRED", 17);

        FitScoringService.ScoringResult result = service.score(
                List.of(core, supporting, preferred),
                List.of(
                        evidence("REQ-1", "Missing"),
                        evidence("REQ-2", "Strong"),
                        evidence("REQ-3", "Strong")
                ));

        assertEquals(50, result.fitScore());
        assertEquals("Ignore", result.recommendation());
    }

    @Test
    void producesTheSameScoreForTheSamePersistedEvidence() {
        FitRequirement first = requirement("REQ-1", "CORE", 60);
        FitRequirement second = requirement("REQ-2", "SUPPORTING", 40);
        List<RequirementEvidence> evidence = List.of(
                evidence("REQ-1", "Partial"),
                evidence("REQ-2", "Strong")
        );

        assertEquals(service.score(List.of(first, second), evidence),
                service.score(List.of(first, second), evidence));
    }

    @Test
    void usesDifferentSimpleLevelsForCurrentEvidenceAndKeepsLegacyPartialStable() {
        assertEquals(1.0, service.evidencePoints(evidence("REQ-1", "Strong")));
        assertEquals(0.75, service.evidencePoints(evidence("REQ-2", "Good")));
        assertEquals(0.40, service.evidencePoints(evidence("REQ-3", "Weak")));
        assertEquals(0.6, service.evidencePoints(evidence("REQ-4", "Partial")));
        assertEquals(0.0, service.evidencePoints(evidence("REQ-5", "Missing")));
    }

    private FitRequirement requirement(String key, String importance, int weight) {
        FitRequirement requirement = new FitRequirement(key, key + " text", key + " capability",
                importance, "DIRECT", "other", "JD excerpt");
        requirement.setWeight(weight);
        return requirement;
    }

    private RequirementEvidence evidence(String key, String status) {
        return new RequirementEvidence(key, status, "ownership", "CV evidence", "artifact", "High");
    }
}
