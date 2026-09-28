package com.shivam.jobcopilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shivam.jobcopilot.repository.FitAnalysisRepository;
import com.shivam.jobcopilot.repository.JobApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class FitAnalysisStreamParserTest {

    @Mock
    private FitAnalysisRepository fitAnalysisRepository;

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private StudentInsightService studentInsightService;

    @Test
    void emitsSectionsInUiOrderAsJsonChunksArrive() {
        FitAnalysisService fitAnalysisService = new FitAnalysisService(
                fitAnalysisRepository, jobApplicationRepository, studentInsightService);
        FitAnalysisItemStreamParser parser = new FitAnalysisItemStreamParser(
                new ObjectMapper(), fitAnalysisService, "Built platform tooling for customer onboarding.");

        String response = """
                {"jdRequirements":[{"id":"REQ-1","requirement":"Build implementation tooling","capability":"Implementation tooling","importance":"Core","relevance":"Direct","evidenceType":"artifact","sourceExcerpt":"Build implementation tooling","evidence":{"status":"Strong","evidenceType":"artifact","evidenceText":"Built platform tooling for customer onboarding","artifact":"Platform tooling","confidence":"High"}}],"strengthAlignment":[{"strength":"Hands-on platform delivery","category":"Experience Depth"}],"differentiation":[],"gaps":[{"gap":"Direct B2B implementation ownership","category":"Experience Depth","severity":"High"}],"positioningAngle":"Lead with platform delivery.","cvAdjustments":[{"adjustment":"Clarify implementation ownership","priority":"High","addressesGap":"Direct B2B implementation ownership","action":"rewrite","cvPoint":"Built platform tooling for customer onboarding.","suggestedText":"Built and delivered platform tooling for customer onboarding."}]}
                """.trim();

        List<FitAnalysisItemStreamParser.StreamEvent> events = new ArrayList<>();
        for (int cursor = 0; cursor < response.length(); cursor += 7) {
            events.addAll(parser.accept(response.substring(cursor, Math.min(cursor + 7, response.length()))));
        }

        List<String> types = events.stream().map(FitAnalysisItemStreamParser.StreamEvent::type).toList();
        assertEquals("score", types.get(0));
        assertTrue(types.indexOf("strengths") > types.indexOf("score"));
        assertTrue(types.indexOf("gaps") > types.indexOf("strengths"));
        assertTrue(types.indexOf("breakdown") > types.indexOf("gaps"));
        assertTrue(types.indexOf("positioning") > types.indexOf("breakdown"));
        assertTrue(types.indexOf("adjustments") > types.indexOf("positioning"));
        assertEquals(100, events.get(0).data().get("fitScore"));
        assertTrue(events.stream().anyMatch(event -> event.data().containsKey("items")));
    }
}
