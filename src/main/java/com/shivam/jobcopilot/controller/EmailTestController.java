package com.shivam.jobcopilot.controller;

import com.shivam.jobcopilot.dto.JobApplicationEmail;
import com.shivam.jobcopilot.dto.MergeDecision;
import com.shivam.jobcopilot.entity.JobApplication;
import com.shivam.jobcopilot.service.EmailClassificationService;
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
    private final JobApplicationService jobApplicationService;

    public EmailTestController(EmailClassificationService classificationService,
                               JobApplicationService jobApplicationService) {
        this.classificationService = classificationService;
        this.jobApplicationService = jobApplicationService;
    }

    public record EmailTestRequest(String subject, String body, String sender, String messageId) {}

    public record EmailTestResponse(
            JobApplicationEmail classification,
            List<JobApplication> candidatesFound,
            MergeDecision mergeDecision
    ) {}

    @PostMapping
    public EmailTestResponse test(@RequestBody EmailTestRequest request, Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();

        JobApplicationEmail classification = classificationService.classify(
                request.subject(), request.body(), request.sender(),
                request.messageId() != null && !request.messageId().isBlank()
                        ? request.messageId() : "test-" + UUID.randomUUID());

        if (classification == null) {
            return new EmailTestResponse(null, List.of(), null);
        }

        List<JobApplication> candidates = jobApplicationService.getEmailCandidates(classification, userId);
        MergeDecision mergeDecision = jobApplicationService.upsert(classification, userId);

        return new EmailTestResponse(classification, candidates, mergeDecision);
    }
}
