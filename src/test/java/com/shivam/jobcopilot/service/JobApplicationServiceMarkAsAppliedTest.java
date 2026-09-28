package com.shivam.jobcopilot.service;

import com.shivam.jobcopilot.dto.JobApplicationEmail;
import com.shivam.jobcopilot.entity.FitAnalysis;
import com.shivam.jobcopilot.entity.EmailReviewItem;
import com.shivam.jobcopilot.entity.JobApplication;
import com.shivam.jobcopilot.repository.FitAnalysisRepository;
import com.shivam.jobcopilot.repository.EmailReviewItemRepository;
import com.shivam.jobcopilot.repository.JobApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceMarkAsAppliedTest {

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
    void createsAppliedApplicationWithFitAnalysisDocuments() {
        UUID userId = UUID.randomUUID();
        UUID fitAnalysisId = UUID.randomUUID();
        UUID cvId = UUID.randomUUID();
        FitAnalysis fitAnalysis = fitAnalysis(fitAnalysisId, userId, cvId);

        when(fitAnalysisRepository.findById(fitAnalysisId)).thenReturn(Optional.of(fitAnalysis));
        when(repository.findByUserIdAndFitAnalysisId(userId, fitAnalysisId)).thenReturn(List.of());
        when(repository.findByUserIdAndCompanyCanonicalIgnoreCaseAndJobTitleIgnoreCase(userId, "Bain & Company", "Consultant"))
                .thenReturn(Optional.empty());
        when(repository.save(any(JobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        JobApplication result = service.markAsApplied(fitAnalysisId, userId);

        assertEquals(userId, result.getUserId());
        assertEquals("Bain & Company - Paris", result.getCompany());
        assertEquals("Bain & Company", result.getCompanyNameCanonical());
        assertEquals("Consultant", result.getJobTitle());
        assertEquals("Strategy & Operations", result.getRoleCategory());
        assertEquals("Applied", result.getApplicationStatus());
        assertEquals(fitAnalysisId, result.getFitAnalysisId());
        assertEquals(cvId, result.getCvId());
        assertEquals("JD text", result.getJobDescriptionText());
        verify(studentInsightService).refreshForApplication(result);
    }

    @Test
    void repeatedActionReturnsExistingLinkedApplicationWithoutCreatingAnother() {
        UUID userId = UUID.randomUUID();
        UUID fitAnalysisId = UUID.randomUUID();
        FitAnalysis fitAnalysis = fitAnalysis(fitAnalysisId, userId, UUID.randomUUID());
        JobApplication existing = new JobApplication();
        existing.setId(UUID.randomUUID());
        existing.setFitAnalysisId(fitAnalysisId);
        existing.setRoleCategory("Strategy & Operations");

        when(fitAnalysisRepository.findById(fitAnalysisId)).thenReturn(Optional.of(fitAnalysis));
        when(repository.findByUserIdAndFitAnalysisId(userId, fitAnalysisId)).thenReturn(List.of(existing));

        JobApplication result = service.markAsApplied(fitAnalysisId, userId);

        assertEquals(existing.getId(), result.getId());
        verify(repository, never()).save(any(JobApplication.class));
    }

    @Test
    void rejectsAnotherUsersFitAnalysis() {
        UUID ownerId = UUID.randomUUID();
        UUID requestingUserId = UUID.randomUUID();
        UUID fitAnalysisId = UUID.randomUUID();
        when(fitAnalysisRepository.findById(fitAnalysisId))
                .thenReturn(Optional.of(fitAnalysis(fitAnalysisId, ownerId, UUID.randomUUID())));

        assertThrows(SecurityException.class,
                () -> service.markAsApplied(fitAnalysisId, requestingUserId));
        verify(repository, never()).save(any(JobApplication.class));
    }

    @Test
    void manualApplicationRequiresCvAndJobDescription() {
        UUID userId = UUID.randomUUID();
        JobApplication app = application(userId, "Amazon", "Product Manager");

        assertThrows(IllegalArgumentException.class, () -> service.create(app, userId));
        verify(repository, never()).save(any(JobApplication.class));
        verify(manualApplicationFitAnalysisService, never()).queue(any(), any());
    }

    @Test
    void manualApplicationIsSavedPendingAndQueuedForBackgroundAnalysis() {
        UUID userId = UUID.randomUUID();
        UUID cvId = UUID.randomUUID();
        JobApplication app = application(userId, "Amazon", "Product Manager");
        app.setCvId(cvId);
        app.setJobDescriptionText("Product Manager job description");
        when(repository.save(any(JobApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobApplication result = service.create(app, userId);

        assertEquals("PENDING", result.getFitAnalysisStatus());
        verify(manualApplicationFitAnalysisService).queue(result.getId(), userId);
    }

    @Test
    void exactCompanyAndRoleEmailEnrichesMarkedApplicationWithoutCallingMergeLlm() {
        UUID userId = UUID.randomUUID();
        UUID fitAnalysisId = UUID.randomUUID();
        JobApplication existing = new JobApplication();
        existing.setId(UUID.randomUUID());
        existing.setUserId(userId);
        existing.setCompany("Amazon");
        existing.setJobTitle("Product Manager");
        existing.setApplicationStatus("Applied");
        existing.setFitAnalysisId(fitAnalysisId);

        when(repository.findByUserIdAndCompanyCanonicalIgnoreCase(userId, "Amazon")).thenReturn(List.of(existing));
        when(fitAnalysisRepository.findByUserIdAndCompanyCanonicalIgnoreCase(userId, "Amazon")).thenReturn(List.of());
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(repository.save(any(JobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.upsert(new JobApplicationEmail(
                "Amazon", "Amazon", "Product Manager", "Jane Smith", "jane@amazon.com",
                "Interview", null, null, null, "message-1", "Recruiter contacted you."), userId);

        assertEquals("Jane Smith", existing.getRecruiterName());
        assertEquals("jane@amazon.com", existing.getRecruiterEmail());
        assertEquals("Interview", existing.getApplicationStatus());
        assertEquals(fitAnalysisId, existing.getFitAnalysisId());
        verify(mergeDecisionService, never()).decide(any(), any(), any());
        verify(studentInsightService).refreshForApplication(existing);
    }

    @Test
    void canonicalCompanyMatchLinksEmailVariantToExistingApplication() {
        UUID userId = UUID.randomUUID();
        JobApplication existing = application(userId, "Bain & Company", "Consultant");
        existing.setCompanyNameRaw("Bain & Company");
        existing.setCompanyNameCanonical("Bain & Company");

        when(repository.findByUserIdAndCompanyCanonicalIgnoreCase(userId, "Bain & Company"))
                .thenReturn(List.of(existing));
        when(fitAnalysisRepository.findByUserIdAndCompanyCanonicalIgnoreCase(userId, "Bain & Company"))
                .thenReturn(List.of());
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(repository.save(any(JobApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.upsert(new JobApplicationEmail(
                "Bain", "Bain & Company", "Consultant", "Jane Smith", "jane@bain.com",
                "Interview", null, null, null, "message-bain", "Interview invitation."), userId);

        assertEquals("Bain & Company", existing.getCompanyNameRaw());
        assertEquals("Bain & Company", existing.getCompanyNameCanonical());
        assertEquals("Jane Smith", existing.getRecruiterName());
        verify(mergeDecisionService, never()).decide(any(), any(), any());
    }

    @Test
    void unresolvedMergeDoesNotCreateAnotherApplication() {
        UUID userId = UUID.randomUUID();
        JobApplication first = application(userId, "Amazon", "Product Manager: Wealth");
        JobApplication second = application(userId, "Amazon", "Product Manager: Platform");
        when(repository.findByUserIdAndCompanyCanonicalIgnoreCase(userId, "Amazon"))
                .thenReturn(List.of(first, second));
        when(fitAnalysisRepository.findByUserIdAndCompanyCanonicalIgnoreCase(userId, "Amazon"))
                .thenReturn(List.of());
        when(mergeDecisionService.decide(any(), any(), any()))
                .thenReturn(new com.shivam.jobcopilot.dto.MergeDecision(
                        "UNRESOLVED", null, null, null, null, "Two possible roles"));

        service.upsert(new JobApplicationEmail(
                "Amazon", "Amazon", "Product Manager", "Jane Smith", "jane@amazon.com",
                "Interview", null, null, null, "message-2", "Application update."), userId);

        verify(repository, never()).save(any(JobApplication.class));
        verify(emailReviewItemRepository).save(any(com.shivam.jobcopilot.entity.EmailReviewItem.class));
    }

    @Test
    void updateDecisionOutsideCandidateSetIsDeferred() {
        UUID userId = UUID.randomUUID();
        JobApplication candidate = application(userId, "Amazon", "Product Manager: Wealth");
        UUID hallucinatedApplicationId = UUID.randomUUID();
        when(repository.findByUserIdAndCompanyCanonicalIgnoreCase(userId, "Amazon"))
                .thenReturn(List.of(candidate));
        when(fitAnalysisRepository.findByUserIdAndCompanyCanonicalIgnoreCase(userId, "Amazon"))
                .thenReturn(List.of());
        when(mergeDecisionService.decide(any(), any(), any()))
                .thenReturn(new com.shivam.jobcopilot.dto.MergeDecision(
                        "UPDATE", hallucinatedApplicationId, null,
                        java.util.Map.of("applicationStatus", "Interview"), List.of(), "Selected application"));

        service.upsert(new JobApplicationEmail(
                "Amazon", "Amazon", null, null, null,
                "Interview", null, null, null, "message-3", "Interview update."), userId);

        verify(repository, never()).findById(hallucinatedApplicationId);
        verify(repository, never()).save(any(JobApplication.class));
    }

    @Test
    void resolvesEmailReviewAgainstOneOfTheCompanyApplications() {
        UUID userId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        JobApplication candidate = application(userId, "Amazon", "Product Manager: Wealth");
        EmailReviewItem review = new EmailReviewItem();
        review.setId(reviewId);
        review.setUserId(userId);
        review.setSourceMessageId("message-review");
        review.setCompanyNameRaw("Amazon");
        review.setCompanyNameCanonical("Amazon");
        review.setJobTitle("Product Manager");
        review.setApplicationStatus("Interview");
        review.setUpdateSummary("Thank you for interviewing with us.");

        when(emailReviewItemRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(repository.findByUserIdAndCompanyCanonicalIgnoreCase(userId, "Amazon"))
                .thenReturn(List.of(candidate));
        when(repository.findById(candidate.getId())).thenReturn(Optional.of(candidate));
        when(repository.save(any(JobApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobApplication result = service.resolveEmailReview(reviewId, candidate.getId(), userId);

        assertEquals("Interview", result.getApplicationStatus());
        assertEquals("RESOLVED", review.getReviewStatus());
        assertEquals(candidate.getId(), review.getResolvedApplicationId());
        verify(emailReviewItemRepository).save(review);
        verify(studentInsightService).refreshForApplication(candidate);
    }

    private JobApplication application(UUID userId, String company, String jobTitle) {
        JobApplication app = new JobApplication();
        app.setId(UUID.randomUUID());
        app.setUserId(userId);
        app.setCompany(company);
        app.setJobTitle(jobTitle);
        app.setApplicationStatus("Applied");
        return app;
    }

    private FitAnalysis fitAnalysis(UUID id, UUID userId, UUID cvId) {
        FitAnalysis fitAnalysis = new FitAnalysis();
        fitAnalysis.setCompany("Bain & Company - Paris");
        fitAnalysis.setCompanyNameCanonical("Bain & Company");
        fitAnalysis.setJobTitle("Consultant");
        fitAnalysis.setRoleCategory("Strategy & Operations");
        fitAnalysis.setJobDescriptionText("JD text");
        fitAnalysis.setCvId(cvId);
        fitAnalysis.setUserId(userId);
        return fitAnalysis;
    }
}
