package com.shivam.jobcopilot.controller;

import com.shivam.jobcopilot.dto.JobApplicationEmail;
import com.shivam.jobcopilot.dto.MergeDecision;
import com.shivam.jobcopilot.entity.FitAnalysis;
import com.shivam.jobcopilot.entity.JobApplication;
import com.shivam.jobcopilot.repository.FitAnalysisRepository;
import com.shivam.jobcopilot.repository.JobApplicationRepository;
import com.shivam.jobcopilot.service.EmailClassificationService;
import com.shivam.jobcopilot.service.EmailMergeDecisionService;
import com.shivam.jobcopilot.service.JobApplicationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/email-test")
public class EmailTestController {

    private final EmailClassificationService classificationService;
    private final EmailMergeDecisionService mergeDecisionService;
    private final JobApplicationRepository jobApplicationRepository;
    private final FitAnalysisRepository fitAnalysisRepository;
    private final JobApplicationService jobApplicationService;

    public EmailTestController(EmailClassificationService classificationService,
                               EmailMergeDecisionService mergeDecisionService,
                               JobApplicationRepository jobApplicationRepository,
                               FitAnalysisRepository fitAnalysisRepository,
                               JobApplicationService jobApplicationService) {
        this.classificationService = classificationService;
        this.mergeDecisionService = mergeDecisionService;
        this.jobApplicationRepository = jobApplicationRepository;
        this.fitAnalysisRepository = fitAnalysisRepository;
        this.jobApplicationService = jobApplicationService;
    }

    public record EmailTestRequest(String subject, String body, String sender) {}

    public record EmailTestResponse(
            JobApplicationEmail classification,
            List<JobApplication> candidatesFound,
            MergeDecision mergeDecision
    ) {}

    @PostMapping
    public EmailTestResponse test(@RequestBody EmailTestRequest request, Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();

        JobApplicationEmail classification = classificationService.classify(
                request.subject(), request.body(), request.sender(), "test-message-id");

        if (classification == null) {
            return new EmailTestResponse(null, List.of(), null);
        }

        List<JobApplication> candidates = classification.company() != null
                ? jobApplicationRepository.findByUserIdAndCompanyIgnoreCase(userId, classification.company())
                : List.of();

        List<FitAnalysis> fitAnalyses = classification.company() != null
                ? fitAnalysisRepository.findByUserIdAndCompanyIgnoreCase(userId, classification.company())
                : List.of();

        MergeDecision mergeDecision = candidates.isEmpty()
                ? new MergeDecision("CREATE", null, null, null, null, "No existing applications at this company")
                : mergeDecisionService.decide(classification, candidates, fitAnalyses);

        jobApplicationService.upsert(classification, userId);

        return new EmailTestResponse(classification, candidates, mergeDecision);
    }
}
