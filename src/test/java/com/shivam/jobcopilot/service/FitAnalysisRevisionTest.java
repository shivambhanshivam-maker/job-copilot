package com.shivam.jobcopilot.service;

import com.shivam.jobcopilot.entity.FitAnalysis;
import com.shivam.jobcopilot.entity.FitRequirement;
import com.shivam.jobcopilot.repository.FitAnalysisRepository;
import com.shivam.jobcopilot.repository.JobApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FitAnalysisRevisionTest {

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
    void reanalysisKeepsTheExistingRubricAndScoresTheUpdatedEvidence() {
        FitAnalysis previous = new FitAnalysis();
        previous.setRevisionNumber(1);
        previous.setScoringVersion(FitScoringService.SCORING_VERSION);
        FitRequirement requirement = new FitRequirement("REQ-1", "Own the roadmap", "Roadmap ownership",
                "CORE", "INFERRED", "ownership", "Own the roadmap from discovery to launch");
        requirement.setWeight(100);
        previous.setJdRequirements(List.of(requirement));

        Optional<FitAnalysis> result = service.createRevisionFromJson(
                """
                {
                  "jdRequirements": [
                    {"id":"REQ-1","evidence":{"status":"Strong","evidenceType":"ownership","evidenceText":"Owned the roadmap","artifact":"Roadmap","confidence":"High"}},
                    {"id":"REQ-99","evidence":{"status":"Strong","evidenceText":"invented evidence"}}
                  ],
                  "confidence":"High"
                }
                """,
                previous,
                null,
                "Owned the roadmap",
                "Acme",
                "Product Manager",
                "Product Management",
                "JD",
                true,
                null);

        FitAnalysis revision = result.orElseThrow();
        assertEquals(1, revision.getJdRequirements().size());
        assertEquals("REQ-1", revision.getJdRequirements().get(0).getRequirementKey());
        assertEquals(100, revision.getFitScore());
        assertEquals("Strong", revision.getRequirementEvidence().get(0).getEvidenceStatus());
        assertEquals("Apply", revision.getRecommendation());
    }
}
