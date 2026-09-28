package com.shivam.jobcopilot.service;

import com.shivam.jobcopilot.dto.PeerEvidencePatternResponse;
import com.shivam.jobcopilot.entity.AffiliationStatus;
import com.shivam.jobcopilot.entity.JobApplication;
import com.shivam.jobcopilot.entity.School;
import com.shivam.jobcopilot.entity.StudentSchoolAffiliation;
import com.shivam.jobcopilot.repository.FitAnalysisRepository;
import com.shivam.jobcopilot.repository.JobApplicationRepository;
import com.shivam.jobcopilot.repository.StudentSchoolAffiliationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PeerEvidenceAggregatorServiceTest {

    @Mock
    private StudentSchoolAffiliationRepository affiliationRepository;

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private FitAnalysisRepository fitAnalysisRepository;

    private PeerEvidenceAggregatorService service;

    @BeforeEach
    void setUp() {
        service = new PeerEvidenceAggregatorService(
                affiliationRepository,
                jobApplicationRepository,
                fitAnalysisRepository
        );
    }

    @Test
    void comparesEvidenceAcrossInterviewAndNonInterviewGroupsAtStudentLevel() {
        UUID currentStudent = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        School school = mock(School.class);
        when(school.getId()).thenReturn(schoolId);
        when(school.getName()).thenReturn("Example School");

        StudentSchoolAffiliation currentAffiliation = new StudentSchoolAffiliation();
        currentAffiliation.setUserId(currentStudent);
        currentAffiliation.setSchool(school);
        currentAffiliation.setStatus(AffiliationStatus.ACTIVE);
        when(affiliationRepository.findByUserId(currentStudent)).thenReturn(List.of(currentAffiliation));

        List<UUID> interviewStudents = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        List<UUID> nonInterviewStudents = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        List<UUID> peerIds = new ArrayList<>();
        peerIds.addAll(interviewStudents);
        peerIds.addAll(nonInterviewStudents);
        List<UUID> allSchoolStudents = new ArrayList<>();
        allSchoolStudents.add(currentStudent);
        allSchoolStudents.addAll(peerIds);
        when(affiliationRepository.findUserIdsBySchoolIdAndStatus(schoolId, AffiliationStatus.ACTIVE))
                .thenReturn(allSchoolStudents);

        List<JobApplication> applications = new ArrayList<>();
        List<FitAnalysisRepository.PeerEvidenceRequirementRow> requirementRows = new ArrayList<>();
        addApplications(applications, requirementRows, interviewStudents, "Interview", "Strong");
        addApplications(applications, requirementRows, nonInterviewStudents, "Rejected", "Missing");

        UUID currentAnalysisId = UUID.randomUUID();
        JobApplication currentApplication = new JobApplication();
        currentApplication.setUserId(currentStudent);
        currentApplication.setRoleCategory("Strategy & Operations");
        currentApplication.setApplicationStatus("Applied");
        currentApplication.setFitAnalysisId(currentAnalysisId);
        requirementRows.add(requirementRow(currentAnalysisId, currentStudent, "Missing"));

        List<JobApplicationRepository.PeerEvidenceApplicationRow> applicationRows = new ArrayList<>();
        applications.forEach(application -> applicationRows.add(applicationRow(application)));
        applicationRows.add(applicationRow(currentApplication));
        when(jobApplicationRepository.findPeerEvidenceApplicationsByUserIds(any())).thenReturn(applicationRows);
        when(fitAnalysisRepository.findPeerEvidenceRequirementsByAnalysisIds(any())).thenReturn(requirementRows);

        PeerEvidencePatternResponse response = service.getForStudent(currentStudent);

        assertEquals(6, response.peerStudentsIncluded());
        assertEquals(1, response.categories().size());
        PeerEvidencePatternResponse.Category category = response.categories().get(0);
        assertEquals(5, category.interviewApplications());
        assertEquals(3, category.interviewStudents());
        assertEquals(5, category.nonInterviewApplications());
        assertEquals(3, category.nonInterviewStudents());
        assertEquals(1, category.patterns().size());
        PeerEvidencePatternResponse.Pattern pattern = category.patterns().get(0);
        assertEquals("Stakeholder management", pattern.capability());
        assertEquals(1.0, pattern.interviewEvidenceRate());
        assertEquals(0.0, pattern.nonInterviewEvidenceRate());
        assertEquals(100, pattern.differencePercentagePoints());
        assertEquals("Missing", pattern.currentStudentEvidenceStatus());
        assertEquals(1, pattern.currentStudentApplicationsWithRequirement());
        assertEquals(0.0, pattern.currentStudentEvidenceRate());
        assertTrue(pattern.interviewStudentsWithEvidence() > pattern.nonInterviewStudentsWithEvidence());
    }

    private void addApplications(List<JobApplication> applications,
                                 List<FitAnalysisRepository.PeerEvidenceRequirementRow> requirementRows,
                                 List<UUID> students,
                                 String status,
                                 String evidenceStatus) {
        int applicationCount = 0;
        for (UUID student : students) {
            int applicationsForStudent = applicationCount < 3 ? 2 : 1;
            for (int i = 0; i < applicationsForStudent; i++) {
                UUID analysisId = UUID.randomUUID();
                JobApplication application = new JobApplication();
                application.setUserId(student);
                application.setRoleCategory("Strategy & Operations");
                application.setApplicationStatus(status);
                application.setFitAnalysisId(analysisId);
                applications.add(application);

                requirementRows.add(requirementRow(analysisId, student, evidenceStatus));
                applicationCount++;
            }
        }
    }

    private JobApplicationRepository.PeerEvidenceApplicationRow applicationRow(JobApplication application) {
        JobApplicationRepository.PeerEvidenceApplicationRow row = mock(JobApplicationRepository.PeerEvidenceApplicationRow.class);
        when(row.getUserId()).thenReturn(application.getUserId());
        when(row.getRoleCategory()).thenReturn(application.getRoleCategory());
        when(row.getApplicationStatus()).thenReturn(application.getApplicationStatus());
        when(row.getInterviewDate()).thenReturn(application.getInterviewDate());
        when(row.getCreatedAt()).thenReturn(application.getCreatedAt());
        when(row.getUpdatedAt()).thenReturn(application.getUpdatedAt());
        when(row.getFitAnalysisId()).thenReturn(application.getFitAnalysisId());
        return row;
    }

    private FitAnalysisRepository.PeerEvidenceRequirementRow requirementRow(
            UUID analysisId, UUID userId, String evidenceStatus) {
        FitAnalysisRepository.PeerEvidenceRequirementRow row = mock(FitAnalysisRepository.PeerEvidenceRequirementRow.class);
        when(row.getFitAnalysisId()).thenReturn(analysisId);
        when(row.getUserId()).thenReturn(userId);
        when(row.getRequirementKey()).thenReturn("REQ-1");
        when(row.getRequirementText()).thenReturn("Work with senior stakeholders");
        when(row.getCapabilityPhrase()).thenReturn("Stakeholder management");
        when(row.getImportanceTier()).thenReturn("Core");
        when(row.getRelevanceMode()).thenReturn("Inferred");
        when(row.getEvidenceType()).thenReturn("ownership");
        when(row.getSourceExcerpt()).thenReturn("Work with senior stakeholders");
        when(row.getEvidenceStatus()).thenReturn(evidenceStatus);
        return row;
    }
}
