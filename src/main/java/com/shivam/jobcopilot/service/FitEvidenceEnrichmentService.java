package com.shivam.jobcopilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shivam.jobcopilot.dto.FitAnalysisResponse;
import com.shivam.jobcopilot.dto.FitEvidenceEnrichmentResponse;
import com.shivam.jobcopilot.entity.CV;
import com.shivam.jobcopilot.entity.FitAnalysis;
import com.shivam.jobcopilot.entity.JobApplication;
import com.shivam.jobcopilot.repository.CVRepository;
import com.shivam.jobcopilot.repository.FitAnalysisRepository;
import com.shivam.jobcopilot.repository.JobApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class FitEvidenceEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(FitEvidenceEnrichmentService.class);

    private final FitAnalysisRepository fitAnalysisRepository;
    private final CVRepository cvRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final AIService aiService;
    private final FitAnalysisService fitAnalysisService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FitEvidenceEnrichmentService(FitAnalysisRepository fitAnalysisRepository,
                                        CVRepository cvRepository,
                                        JobApplicationRepository jobApplicationRepository,
                                        AIService aiService,
                                        FitAnalysisService fitAnalysisService) {
        this.fitAnalysisRepository = fitAnalysisRepository;
        this.cvRepository = cvRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.aiService = aiService;
        this.fitAnalysisService = fitAnalysisService;
    }

    @Transactional
    public FitEvidenceEnrichmentResponse enrichOne(UUID fitAnalysisId) {
        FitAnalysis analysis = fitAnalysisRepository.findById(fitAnalysisId).orElse(null);
        if (analysis == null) {
            return new FitEvidenceEnrichmentResponse(fitAnalysisId, "FAILED", "Fit analysis was not found");
        }
        if (analysis.getCvId() == null) {
            return new FitEvidenceEnrichmentResponse(fitAnalysisId, "SKIPPED", "Fit analysis has no linked CV");
        }

        CV cv = cvRepository.findById(analysis.getCvId()).orElse(null);
        String cvText = cv == null ? null : usableText(cv);
        if (cvText == null) {
            return new FitEvidenceEnrichmentResponse(fitAnalysisId, "SKIPPED", "Linked CV has no usable text");
        }
        if (analysis.getJobDescriptionText() == null || analysis.getJobDescriptionText().isBlank()) {
            return new FitEvidenceEnrichmentResponse(fitAnalysisId, "SKIPPED", "Fit analysis has no job description");
        }

        try {
            String json = aiService.extractJdRequirements(cvText, analysis.getJobDescriptionText());
            FitAnalysisResponse response = objectMapper.readValue(json, FitAnalysisResponse.class);
            fitAnalysisService.replaceRequirementEvidence(fitAnalysisId, response.getJdRequirements());
            return new FitEvidenceEnrichmentResponse(fitAnalysisId, "ENRICHED", "JD requirements and CV evidence updated");
        } catch (Exception e) {
            log.error("Failed to enrich fit analysis {}", fitAnalysisId, e);
            return new FitEvidenceEnrichmentResponse(fitAnalysisId, "FAILED", "Requirement extraction failed: " + e.getMessage());
        }
    }

    @Transactional
    public FitEvidenceEnrichmentResponse.Run enrichAppliedAnalyses() {
        Set<UUID> ids = jobApplicationRepository.findAll().stream()
                .map(JobApplication::getFitAnalysisId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        int enriched = 0;
        int skippedNoCv = 0;
        int skippedNoText = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (UUID id : ids) {
            FitAnalysis analysis = fitAnalysisRepository.findById(id).orElse(null);
            if (analysis == null) {
                failed++;
                errors.add(id + ": fit analysis was not found");
                continue;
            }
            if (analysis.getCvId() == null) {
                skippedNoCv++;
                continue;
            }
            CV cv = cvRepository.findById(analysis.getCvId()).orElse(null);
            if (cv == null || usableText(cv) == null) {
                skippedNoText++;
                continue;
            }

            FitEvidenceEnrichmentResponse result = enrichOne(id);
            switch (result.status()) {
                case "ENRICHED" -> enriched++;
                case "SKIPPED" -> {
                    skippedNoText++;
                    errors.add(id + ": " + result.message());
                }
                default -> {
                    failed++;
                    errors.add(id + ": " + result.message());
                }
            }
        }

        return new FitEvidenceEnrichmentResponse.Run(ids.size(), enriched, skippedNoCv, skippedNoText, failed, errors);
    }

    private String usableText(CV cv) {
        if (cv.getContentText() != null && !cv.getContentText().isBlank()) return cv.getContentText();
        if (cv.getContentMarkdown() != null && !cv.getContentMarkdown().isBlank()) return cv.getContentMarkdown();
        return null;
    }
}
