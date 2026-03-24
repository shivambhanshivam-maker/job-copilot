package com.shivam.jobcopilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shivam.jobcopilot.dto.FitAnalysisResponse;
import com.shivam.jobcopilot.entity.CvAdjustmentItem;
import com.shivam.jobcopilot.entity.FitAnalysis;
import com.shivam.jobcopilot.entity.GapItem;
import com.shivam.jobcopilot.entity.StrengthItem;
import com.shivam.jobcopilot.repository.FitAnalysisRepository;
import com.shivam.jobcopilot.repository.JobApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FitAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(FitAnalysisService.class);

    private final FitAnalysisRepository fitAnalysisRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FitAnalysisService(FitAnalysisRepository fitAnalysisRepository,
                              JobApplicationRepository jobApplicationRepository) {
        this.fitAnalysisRepository = fitAnalysisRepository;
        this.jobApplicationRepository = jobApplicationRepository;
    }

    // Called by the controller's doOnComplete — parses the fully assembled JSON then persists.
    // company and jobTitle are supplied by the caller directly — not extracted from the LLM response.
    // Returns empty if parsing fails so the scheduler can handle the failure gracefully.
    public Optional<FitAnalysis> persistFromJson(String json, String jobDescriptionText, UUID cvId,
                                                 String company, String jobTitle, UUID userId) {
        try {
            FitAnalysisResponse parsed = objectMapper.readValue(json, FitAnalysisResponse.class);
            return Optional.of(save(company, jobTitle, jobDescriptionText, cvId, parsed, userId));
        } catch (Exception e) {
            log.error("Failed to persist fit analysis after stream completed", e);
            return Optional.empty();
        }
    }

    // Overload without userId for scheduler (job listings) where there is no user context
    public Optional<FitAnalysis> persistFromJson(String json, String jobDescriptionText, UUID cvId,
                                                 String company, String jobTitle) {
        return persistFromJson(json, jobDescriptionText, cvId, company, jobTitle, null);
    }

    @Transactional
    public FitAnalysis save(String company, String jobTitle, String jobDescriptionText,
                            UUID cvId, FitAnalysisResponse response, UUID userId) {
        FitAnalysis fa = new FitAnalysis();
        fa.setCompany(company);
        fa.setJobTitle(jobTitle);
        fa.setJobDescriptionText(jobDescriptionText);
        fa.setCvId(cvId);
        fa.setFitScore(response.getFitScore());
        fa.setRecommendation(response.getRecommendation());
        fa.setConfidence(response.getConfidence());
        fa.setWeightageReasoning(response.getWeightageReasoning());

        FitAnalysisResponse.SubScores ss = response.getSubScores();
        if (ss != null) {
            if (ss.skillsMatch() != null)     { fa.setSkillsMatchScore(ss.skillsMatch().score());         fa.setSkillsMatchWeight(ss.skillsMatch().weight()); }
            if (ss.experienceMatch() != null) { fa.setExperienceMatchScore(ss.experienceMatch().score()); fa.setExperienceMatchWeight(ss.experienceMatch().weight()); }
            if (ss.domainMatch() != null)     { fa.setDomainMatchScore(ss.domainMatch().score());         fa.setDomainMatchWeight(ss.domainMatch().weight()); }
            if (ss.impactMatch() != null)     { fa.setImpactMatchScore(ss.impactMatch().score());         fa.setImpactMatchWeight(ss.impactMatch().weight()); }
            if (ss.cvPresentation() != null)  { fa.setCvPresentationScore(ss.cvPresentation().score());  fa.setCvPresentationWeight(ss.cvPresentation().weight()); }
        }

        if (response.getStrengthAlignment() != null) {
            List<StrengthItem> strengths = response.getStrengthAlignment().stream()
                    .map(s -> new StrengthItem(s.strength(), s.category()))
                    .toList();
            fa.setStrengthAlignment(strengths);
        }

        fa.setDifferentiation(response.getDifferentiation());

        if (response.getGaps() != null) {
            List<GapItem> gaps = response.getGaps().stream()
                    .map(g -> new GapItem(g.gap(), g.category(), g.severity()))
                    .toList();
            fa.setGaps(gaps);
        }

        fa.setPositioningAngle(response.getPositioningAngle());

        if (response.getCvAdjustments() != null) {
            List<CvAdjustmentItem> adjustments = response.getCvAdjustments().stream()
                    .map(a -> new CvAdjustmentItem(a.adjustment(), a.priority(), a.addressesGap()))
                    .toList();
            fa.setCvAdjustments(adjustments);
        }

        fa.setUserId(userId);

        FitAnalysis saved = fitAnalysisRepository.save(fa);

        // Auto-link: if a JobApplication already exists for this company + title, attach the fit analysis
        if (userId != null) {
            jobApplicationRepository
                    .findByUserIdAndCompanyIgnoreCaseAndJobTitleIgnoreCase(userId, company, jobTitle)
                    .ifPresent(app -> {
                        app.setFitAnalysisId(saved.getId());
                        jobApplicationRepository.save(app);
                    });
        } else {
            jobApplicationRepository
                    .findByCompanyIgnoreCaseAndJobTitleIgnoreCase(company, jobTitle)
                    .ifPresent(app -> {
                        app.setFitAnalysisId(saved.getId());
                        jobApplicationRepository.save(app);
                    });
        }

        return saved;
    }

    // Overload without userId — for backwards compat with scheduler
    @Transactional
    public FitAnalysis save(String company, String jobTitle, String jobDescriptionText,
                            UUID cvId, FitAnalysisResponse response) {
        return save(company, jobTitle, jobDescriptionText, cvId, response, null);
    }
}
