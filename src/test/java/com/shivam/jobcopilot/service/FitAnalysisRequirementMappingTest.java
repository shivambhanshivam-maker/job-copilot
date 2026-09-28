package com.shivam.jobcopilot.service;

import com.shivam.jobcopilot.dto.FitAnalysisResponse;
import com.shivam.jobcopilot.entity.FitAnalysis;
import com.shivam.jobcopilot.repository.FitAnalysisRepository;
import com.shivam.jobcopilot.repository.JobApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FitAnalysisRequirementMappingTest {

    @Mock
    private FitAnalysisRepository fitAnalysisRepository;

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private StudentInsightService studentInsightService;

    private FitAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new FitAnalysisService(fitAnalysisRepository, jobApplicationRepository, studentInsightService);
        when(fitAnalysisRepository.save(any(FitAnalysis.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void retainsPreferredRequirementsAndDoesNotInventMissingEvidence() {
        when(jobApplicationRepository.findByCompanyCanonicalIgnoreCaseAndJobTitleIgnoreCase(any(), any()))
                .thenReturn(Optional.empty());

        FitAnalysisResponse response = new FitAnalysisResponse();
        response.setJdRequirements(new ArrayList<>(List.of(
                requirement("REQ-1", "SQL experience is preferred", "SQL analysis", "Preferred", "Direct",
                        new FitAnalysisResponse.RequirementEvidence("Missing", "tool", null, null, "High")),
                requirement("REQ-1", "Duplicate should be ignored", "Duplicate", "Core", "Direct", null),
                requirement("REQ-2", "Own roadmap delivery", "Product roadmap ownership", "Core", "Inferred",
                        new FitAnalysisResponse.RequirementEvidence("Strong", "ownership", "Owned roadmap", "Roadmap", "High")),
                requirement("REQ-3", "No source", "Invalid", "Core", "Direct", null)
        )));
        // A missing JD excerpt makes REQ-3 invalid; it must not enter persisted role evidence.
        response.getJdRequirements().set(3, new FitAnalysisResponse.JdRequirement(
                "REQ-3", "No source", "Invalid", "Core", "Direct", "other", "", null));

        FitAnalysis saved = service.save("Acme", "Product Manager", "JD", null, response, null);

        assertEquals(2, saved.getJdRequirements().size());
        assertEquals("SQL analysis", saved.getJdRequirements().get(0).getCapabilityPhrase());
        assertEquals("PREFERRED", saved.getJdRequirements().get(0).getImportanceTier());
        assertEquals(67, saved.getJdRequirements().get(0).getWeight());
        assertEquals(33, saved.getJdRequirements().get(1).getWeight());
        assertEquals(2, saved.getRequirementEvidence().size());
        assertNull(saved.getRequirementEvidence().get(0).getEvidenceText());
        assertNull(saved.getRequirementEvidence().get(0).getArtifact());
    }

    @Test
    void enrichmentReplacesOnlyRequirementsAndPreservesFitResult() {
        UUID id = UUID.randomUUID();
        FitAnalysis existing = new FitAnalysis();
        existing.setFitScore(87);
        existing.setRecommendation("Apply");
        existing.setJobDescriptionText("JD");
        existing.setJdRequirements(List.of());
        when(fitAnalysisRepository.findById(id)).thenReturn(Optional.of(existing));

        FitAnalysis saved = service.replaceRequirementEvidence(id, List.of(
                requirement("REQ-1", "Lead stakeholder workshops", "Stakeholder management", "Supporting", "Inferred",
                        new FitAnalysisResponse.RequirementEvidence("Partial", "artifact", "Workshop delivery", "Workshop", "Medium"))
        ));

        assertEquals(87, saved.getFitScore());
        assertEquals("Apply", saved.getRecommendation());
        assertEquals(1, saved.getJdRequirements().size());
        assertEquals("Stakeholder management", saved.getJdRequirements().get(0).getCapabilityPhrase());
    }

    @Test
    void keepsEvidenceWhenFormattingDiffersFromTheCvText() {
        when(jobApplicationRepository.findByCompanyCanonicalIgnoreCaseAndJobTitleIgnoreCase(any(), any()))
                .thenReturn(Optional.empty());

        FitAnalysisResponse response = new FitAnalysisResponse();
        response.setJdRequirements(new ArrayList<>(List.of(
                requirement("REQ-1", "Manage partner operations", "Partner operations", "Core", "Direct",
                        new FitAnalysisResponse.RequirementEvidence(
                                "Good", "ownership", "Owned end-to-end partner operations", "Partner pipeline", "High"))
        )));

        FitAnalysis saved = service.save(
                "Acme", "Implementation Manager", "Customer Success", "JD", null, response, null,
                "Owned end to end partner operations across multiple markets.");

        assertEquals("Good", saved.getRequirementEvidence().get(0).getEvidenceStatus());
        assertEquals("Owned end-to-end partner operations", saved.getRequirementEvidence().get(0).getEvidenceText());
    }

    @Test
    void givesExplicitExperienceQualificationsMoreWeightThanResponsibilitySignals() {
        when(jobApplicationRepository.findByCompanyCanonicalIgnoreCaseAndJobTitleIgnoreCase(any(), any()))
                .thenReturn(Optional.empty());

        FitAnalysisResponse response = new FitAnalysisResponse();
        response.setJdRequirements(new ArrayList<>(List.of(
                requirement("REQ-1", "Bring 3-4 years of experience in implementation", "Implementation experience",
                        "Preferred", "Direct", new FitAnalysisResponse.RequirementEvidence("Good", "experience", "Four years of implementation", null, "High")),
                requirement("REQ-2", "Run discovery sessions with customers", "Customer discovery",
                        "Core", "Inferred", new FitAnalysisResponse.RequirementEvidence("Strong", "ownership", "Ran discovery sessions", null, "High"))
        )));

        FitAnalysis saved = service.save("Acme", "Implementation Manager", "Customer Success", "JD", null, response, null,
                "Four years of implementation. Ran discovery sessions.");

        assertEquals("CORE", saved.getJdRequirements().get(0).getImportanceTier());
        assertEquals("SUPPORTING", saved.getJdRequirements().get(1).getImportanceTier());
        assertEquals(75, saved.getJdRequirements().get(0).getWeight());
        assertEquals(25, saved.getJdRequirements().get(1).getWeight());
    }

    private FitAnalysisResponse.JdRequirement requirement(String id, String text, String capability,
                                                          String importance, String relevance,
                                                          FitAnalysisResponse.RequirementEvidence evidence) {
        return new FitAnalysisResponse.JdRequirement(
                id, text, capability, importance, relevance, "ownership", "JD evidence", evidence);
    }
}
