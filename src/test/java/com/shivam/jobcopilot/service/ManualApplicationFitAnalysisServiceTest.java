package com.shivam.jobcopilot.service;

import com.shivam.jobcopilot.entity.CV;
import com.shivam.jobcopilot.entity.FitAnalysis;
import com.shivam.jobcopilot.entity.JobApplication;
import com.shivam.jobcopilot.repository.JobApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManualApplicationFitAnalysisServiceTest {

    @Mock
    private JobApplicationRepository repository;

    @Mock
    private CVService cvService;

    @Mock
    private AIService aiService;

    @Mock
    private FitAnalysisService fitAnalysisService;

    @Test
    void completesApplicationAfterFitAnalysisIsPersisted() {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID cvId = UUID.randomUUID();
        JobApplication app = application(userId, applicationId, cvId);
        CV cv = cv(userId, cvId);
        UUID analysisId = UUID.randomUUID();
        FitAnalysis analysis = org.mockito.Mockito.mock(FitAnalysis.class);
        when(analysis.getId()).thenReturn(analysisId);

        when(repository.findById(applicationId)).thenReturn(Optional.of(app));
        when(cvService.getById(cvId)).thenReturn(cv);
        when(aiService.analyze("CV text", "JD text", "Amazon", "Product Manager"))
                .thenReturn("fit-json");
        when(fitAnalysisService.persistFromJson("fit-json", "JD text", cvId,
                "Amazon", "Product Manager", null, userId, "CV text")).thenReturn(Optional.of(analysis));

        new ManualApplicationFitAnalysisService(repository, cvService, aiService, fitAnalysisService)
                .queue(applicationId, userId);

        assertEquals("COMPLETED", app.getFitAnalysisStatus());
        assertEquals(analysisId, app.getFitAnalysisId());
        verify(repository).save(app);
    }

    @Test
    void marksApplicationFailedWhenAnalysisCannotBeSaved() {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID cvId = UUID.randomUUID();
        JobApplication app = application(userId, applicationId, cvId);

        when(repository.findById(applicationId)).thenReturn(Optional.of(app));
        when(cvService.getById(cvId)).thenThrow(new IllegalStateException("CV unavailable"));

        new ManualApplicationFitAnalysisService(repository, cvService, aiService, fitAnalysisService)
                .queue(applicationId, userId);

        assertEquals("FAILED", app.getFitAnalysisStatus());
        assertEquals("CV unavailable", app.getFitAnalysisError());
        verify(repository).save(app);
    }

    private JobApplication application(UUID userId, UUID applicationId, UUID cvId) {
        JobApplication app = new JobApplication();
        app.setId(applicationId);
        app.setUserId(userId);
        app.setCompany("Amazon");
        app.setJobTitle("Product Manager");
        app.setCvId(cvId);
        app.setJobDescriptionText("JD text");
        app.setFitAnalysisStatus("PENDING");
        return app;
    }

    private CV cv(UUID userId, UUID cvId) {
        CV cv = new CV();
        cv.setId(cvId);
        cv.setUserId(userId);
        cv.setContentText("CV text");
        return cv;
    }
}
