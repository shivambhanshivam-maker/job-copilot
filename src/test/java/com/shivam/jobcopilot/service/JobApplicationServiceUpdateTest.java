package com.shivam.jobcopilot.service;

import com.shivam.jobcopilot.entity.JobApplication;
import com.shivam.jobcopilot.repository.EmailReviewItemRepository;
import com.shivam.jobcopilot.repository.FitAnalysisRepository;
import com.shivam.jobcopilot.repository.JobApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceUpdateTest {

    @Mock
    private JobApplicationRepository repository;

    @Mock
    private FitAnalysisRepository fitAnalysisRepository;

    @Mock
    private EmailReviewItemRepository emailReviewItemRepository;

    @Mock
    private EmailMergeDecisionService mergeDecisionService;

    @Mock
    private PostApplicationInsightService insightService;

    @Mock
    private StudentInsightService studentInsightService;

    @Mock
    private ManualApplicationFitAnalysisService manualApplicationFitAnalysisService;

    private JobApplicationService service;

    @BeforeEach
    void setUp() {
        service = new JobApplicationService(
                repository, fitAnalysisRepository, emailReviewItemRepository, mergeDecisionService, insightService,
                studentInsightService, manualApplicationFitAnalysisService);
    }

    @Test
    void persistsCategoryCompanyAndClearableFields() {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        JobApplication existing = new JobApplication();
        existing.setId(applicationId);
        existing.setUserId(userId);
        existing.setCompany("Old Company");
        existing.setJobTitle("Old title");
        existing.setRoleCategory("Old category");
        existing.setApplicationStatus("Interview");
        existing.setInterviewDate(LocalDateTime.now());
        existing.setJobDescriptionText("Old JD");
        existing.setJobDescriptionUrl("https://old.example/jobs/1");
        existing.setNotes("Old notes");

        // Simulate the frontend sending both the edited display value and the old raw company value.
        JobApplication updated = new JobApplication();
        updated.setCompany("New Company");
        updated.setCompanyNameRaw("Old Company");
        updated.setCompanyNameCanonical("Old Company");
        updated.setJobTitle("New title");
        updated.setRoleCategory("New category");
        updated.setApplicationStatus("Applied");
        updated.setInterviewDate(null);
        updated.setJobDescriptionText(null);
        updated.setJobDescriptionUrl(null);
        updated.setNotes("");

        when(repository.findById(applicationId)).thenReturn(Optional.of(existing));
        when(repository.save(any(JobApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobApplication result = service.update(applicationId, updated, userId);

        assertEquals("New Company", result.getCompany());
        assertEquals("New Company", result.getCompanyNameRaw());
        assertEquals("New Company", result.getCompanyNameCanonical());
        assertEquals("New category", result.getRoleCategory());
        assertEquals("Applied", result.getApplicationStatus());
        assertNull(result.getInterviewDate());
        assertNull(result.getJobDescriptionText());
        assertNull(result.getJobDescriptionUrl());
        assertNull(result.getNotes());
        verify(studentInsightService).refreshForApplication(result);
    }

    @Test
    void rejectsUpdatesForAnotherUser() {
        UUID ownerId = UUID.randomUUID();
        UUID requestingUserId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        JobApplication existing = new JobApplication();
        existing.setId(applicationId);
        existing.setUserId(ownerId);

        when(repository.findById(applicationId)).thenReturn(Optional.of(existing));

        assertThrows(SecurityException.class,
                () -> service.update(applicationId, new JobApplication(), requestingUserId));
        verify(repository, never()).save(any(JobApplication.class));
        verify(studentInsightService, never()).refreshForApplication(any(JobApplication.class));
    }
}
