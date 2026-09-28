package com.shivam.jobcopilot.service;

import com.shivam.jobcopilot.entity.JobApplication;
import com.shivam.jobcopilot.entity.StudentInsight;
import com.shivam.jobcopilot.repository.FitAnalysisRepository;
import com.shivam.jobcopilot.repository.GapMentionRepository;
import com.shivam.jobcopilot.repository.JobApplicationRepository;
import com.shivam.jobcopilot.repository.StudentInsightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentInsightServiceTest {

    @Mock
    private FitAnalysisRepository fitAnalysisRepository;

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private GapMentionRepository gapMentionRepository;

    @Mock
    private StudentInsightRepository studentInsightRepository;

    private StudentInsightService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new StudentInsightService(
                fitAnalysisRepository,
                jobApplicationRepository,
                gapMentionRepository,
                studentInsightRepository
        );
        userId = UUID.randomUUID();
    }

    @Test
    void interviewSevenCalendarDaysAwayBecomesPrimaryFocus() {
        JobApplication application = application("Acme", "Strategy Manager", "Strategy & Operations", 0);
        application.setApplicationStatus("Interview");
        application.setInterviewDate(LocalDateTime.now().plusDays(7).withHour(9));
        when(jobApplicationRepository.findByUserId(userId)).thenReturn(List.of(application));

        service.rebuildStoredInsights(userId);

        List<StudentInsight> saved = savedInsights();
        StudentInsight upcoming = saved.stream()
                .filter(insight -> "UPCOMING_INTERVIEW".equals(insight.getInsightType()))
                .findFirst()
                .orElseThrow();
        StudentInsightService.PrimaryFocus focus = service.selectPrimaryFocus(saved).orElseThrow();

        assertEquals("UPCOMING_INTERVIEW", focus.type());
        assertTrue(upcoming.getTitle().contains("Strategy Manager"));
    }

    @Test
    void applicationsOlderThanFortyFiveDaysDoNotCreateCategorySignals() {
        List<JobApplication> applications = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            JobApplication strong = application("Strong " + i, "Manager", "Strategy & Operations", 46);
            strong.setFirstRespondedAt(LocalDateTime.now().minusDays(40));
            applications.add(strong);
            applications.add(application("Weak " + i, "Manager", "Product Management", 46));
        }
        when(jobApplicationRepository.findByUserId(userId)).thenReturn(applications);

        service.rebuildStoredInsights(userId);

        List<String> types = savedInsights().stream().map(StudentInsight::getInsightType).toList();
        assertFalse(types.contains("ROLE_CATEGORY_TRACTION"));
        assertFalse(types.contains("ROLE_CATEGORY_FIT_COMPARISON"));
    }

    @Test
    void tractionRequiresFiveRecentApplicationsInEachComparedCategory() {
        List<JobApplication> applications = categoryApplications(4);
        when(jobApplicationRepository.findByUserId(userId)).thenReturn(applications);

        service.rebuildStoredInsights(userId);

        assertFalse(savedInsights().stream()
                .anyMatch(insight -> "ROLE_CATEGORY_TRACTION".equals(insight.getInsightType())));
    }

    @Test
    void tractionIsGeneratedForFiveRecentApplicationsAndTwentyPointDifference() {
        List<JobApplication> applications = categoryApplications(5);
        when(jobApplicationRepository.findByUserId(userId)).thenReturn(applications);

        service.rebuildStoredInsights(userId);

        assertTrue(savedInsights().stream()
                .anyMatch(insight -> "ROLE_CATEGORY_TRACTION".equals(insight.getInsightType())));
    }

    @Test
    void conflictingFitAndTractionSignalsDoNotBecomePrimaryFocus() {
        StudentInsight fit = insight(
                "ROLE_CATEGORY_FIT_COMPARISON",
                "{\"bestCategory\":\"Product Management\",\"comparedCategory\":\"Strategy & Operations\"}"
        );
        StudentInsight traction = insight(
                "ROLE_CATEGORY_TRACTION",
                "{\"bestCategory\":\"Strategy & Operations\",\"worstCategory\":\"Product Management\","
                        + "\"bestApplications\":5,\"worstApplications\":5}"
        );

        assertTrue(service.selectPrimaryFocus(List.of(fit, traction)).isEmpty());
    }

    @Test
    void recurringGapOutranksCategoryPerformance() {
        StudentInsight recurring = insight(
                "RECURRING_CAPABILITY_GAP",
                "{\"capability\":\"Executive synthesis\",\"applicationCount\":3}"
        );
        recurring.setRecommendation("Add a concrete example to your CV.");
        StudentInsight traction = insight(
                "ROLE_CATEGORY_TRACTION",
                "{\"bestCategory\":\"Strategy & Operations\",\"worstCategory\":\"Product Management\"}"
        );

        StudentInsightService.PrimaryFocus focus = service.selectPrimaryFocus(List.of(traction, recurring)).orElseThrow();

        assertEquals("RECURRING_CAPABILITY_GAP", focus.type());
        assertTrue(focus.title().contains("executive synthesis"));
    }

    private List<JobApplication> categoryApplications(int applicationsPerCategory) {
        List<JobApplication> applications = new ArrayList<>();
        for (int i = 0; i < applicationsPerCategory; i++) {
            JobApplication strong = application("Strong " + i, "Manager", "Strategy & Operations", 5);
            if (i < 3) strong.setFirstRespondedAt(LocalDateTime.now().minusDays(1));
            applications.add(strong);

            JobApplication weak = application("Weak " + i, "Product Manager", "Product Management", 5);
            if (i < 1) weak.setFirstRespondedAt(LocalDateTime.now().minusDays(1));
            applications.add(weak);
        }
        return applications;
    }

    private JobApplication application(String company, String title, String category, int daysAgo) {
        JobApplication application = new JobApplication();
        application.setId(UUID.randomUUID());
        application.setUserId(userId);
        application.setCompany(company);
        application.setJobTitle(title);
        application.setRoleCategory(category);
        application.setApplicationStatus("Applied");
        application.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        application.setUpdatedAt(LocalDateTime.now().minusDays(daysAgo));
        return application;
    }

    private StudentInsight insight(String type, String evidenceJson) {
        StudentInsight insight = new StudentInsight();
        insight.setUserId(userId);
        insight.setInsightType(type);
        insight.setTitle(type);
        insight.setRecommendation("Recommendation");
        insight.setEvidenceJson(evidenceJson);
        return insight;
    }

    @SuppressWarnings("unchecked")
    private List<StudentInsight> savedInsights() {
        ArgumentCaptor<List<StudentInsight>> captor = ArgumentCaptor.forClass(List.class);
        verify(studentInsightRepository).saveAll(captor.capture());
        return captor.getValue();
    }
}
