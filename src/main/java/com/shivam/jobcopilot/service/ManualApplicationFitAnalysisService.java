package com.shivam.jobcopilot.service;

import com.shivam.jobcopilot.entity.CV;
import com.shivam.jobcopilot.entity.FitAnalysis;
import com.shivam.jobcopilot.entity.JobApplication;
import com.shivam.jobcopilot.repository.JobApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ManualApplicationFitAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ManualApplicationFitAnalysisService.class);

    private final JobApplicationRepository jobApplicationRepository;
    private final CVService cvService;
    private final AIService aiService;
    private final FitAnalysisService fitAnalysisService;

    public ManualApplicationFitAnalysisService(JobApplicationRepository jobApplicationRepository,
                                               CVService cvService,
                                               AIService aiService,
                                               FitAnalysisService fitAnalysisService) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.cvService = cvService;
        this.aiService = aiService;
        this.fitAnalysisService = fitAnalysisService;
    }

    @Async("applicationAnalysisExecutor")
    public void queue(UUID applicationId, UUID userId) {
        analyze(applicationId, userId);
    }

    private void analyze(UUID applicationId, UUID userId) {
        try {
            JobApplication app = jobApplicationRepository.findById(applicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
            if (!userId.equals(app.getUserId())) {
                throw new SecurityException("Application does not belong to the current user");
            }

            CV cv = cvService.getById(app.getCvId());
            if (!userId.equals(cv.getUserId())) {
                throw new SecurityException("CV does not belong to the current user");
            }

            String rawJson = aiService.analyze(
                    cv.getContentText(),
                    app.getJobDescriptionText(),
                    app.getCompany(),
                    app.getJobTitle());

            Optional<FitAnalysis> analysis = fitAnalysisService.persistFromJson(
                    rawJson,
                    app.getJobDescriptionText(),
                    cv.getId(),
                    app.getCompany(),
                    app.getJobTitle(),
                    null,
                    userId,
                    cv.getContentText());

            FitAnalysis fitAnalysis = analysis.orElseThrow(
                    () -> new IllegalStateException("Fit analysis response could not be saved"));

            JobApplication completed = jobApplicationRepository.findById(applicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Application not found after analysis: " + applicationId));
            completed.setFitAnalysisId(fitAnalysis.getId());
            completed.setFitAnalysisStatus("COMPLETED");
            completed.setFitAnalysisError(null);
            jobApplicationRepository.save(completed);
        } catch (Exception e) {
            log.error("Manual application fit analysis failed for {}", applicationId, e);
            jobApplicationRepository.findById(applicationId).ifPresent(app -> {
                app.setFitAnalysisStatus("FAILED");
                app.setFitAnalysisError(errorMessage(e));
                jobApplicationRepository.save(app);
            });
        }
    }

    private String errorMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) return "Fit analysis failed. Please try again.";
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
