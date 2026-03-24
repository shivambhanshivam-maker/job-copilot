package com.shivam.jobcopilot.controller;

import com.shivam.jobcopilot.entity.CV;
import com.shivam.jobcopilot.service.AIService;
import com.shivam.jobcopilot.service.CVService;
import com.shivam.jobcopilot.service.FitAnalysisService;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;

@RestController
public class FitAnalysisController {

    private final CVService cvService;
    private final AIService aiService;
    private final FitAnalysisService fitAnalysisService;

    public FitAnalysisController(CVService cvService, AIService aiService, FitAnalysisService fitAnalysisService) {
        this.cvService = cvService;
        this.aiService = aiService;
        this.fitAnalysisService = fitAnalysisService;
    }

    private UUID currentUserId(Authentication auth) {
        return (UUID) auth.getPrincipal();
    }

    @PostMapping(value = "/match/analyze", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> analyze(@RequestBody Map<String, String> request, Authentication auth) {
        UUID userId = currentUserId(auth);
        UUID cvId = UUID.fromString(request.get("cvId"));
        String jobDescription = request.get("jobDescription");
        String companyName = request.get("companyName");
        String roleTitle = request.get("jobTitle");

        CV cv = cvService.getById(cvId);

        StringBuilder buffer = new StringBuilder();

        return aiService.analyzeStream(cv.getContentText(), jobDescription, companyName, roleTitle)
                .doOnNext(buffer::append)
                .doOnComplete(() -> fitAnalysisService.persistFromJson(
                        buffer.toString(), jobDescription, cvId, companyName, roleTitle, userId
                ));
    }
}
