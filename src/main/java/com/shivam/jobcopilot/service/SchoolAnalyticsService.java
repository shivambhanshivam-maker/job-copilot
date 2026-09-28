package com.shivam.jobcopilot.service;

import com.shivam.jobcopilot.dto.SchoolOverviewResponse;
import com.shivam.jobcopilot.entity.Advisor;
import com.shivam.jobcopilot.entity.AdvisorVisibilityLevel;
import com.shivam.jobcopilot.entity.AffiliationStatus;
import com.shivam.jobcopilot.entity.School;
import com.shivam.jobcopilot.entity.StudentConsent;
import com.shivam.jobcopilot.repository.AdvisorRepository;
import com.shivam.jobcopilot.repository.JobApplicationRepository;
import com.shivam.jobcopilot.repository.StudentConsentRepository;
import com.shivam.jobcopilot.repository.StudentSchoolAffiliationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SchoolAnalyticsService {

    private static final List<String> INTERVIEW_STATUSES = List.of("Interview", "Offer");
    private static final List<String> OFFER_STATUSES = List.of("Offer");

    private final AdvisorRepository advisorRepository;
    private final StudentSchoolAffiliationRepository affiliationRepository;
    private final StudentConsentRepository consentRepository;
    private final JobApplicationRepository jobApplicationRepository;

    public SchoolAnalyticsService(AdvisorRepository advisorRepository,
                                  StudentSchoolAffiliationRepository affiliationRepository,
                                  StudentConsentRepository consentRepository,
                                  JobApplicationRepository jobApplicationRepository) {
        this.advisorRepository = advisorRepository;
        this.affiliationRepository = affiliationRepository;
        this.consentRepository = consentRepository;
        this.jobApplicationRepository = jobApplicationRepository;
    }

    @Transactional(readOnly = true)
    public SchoolOverviewResponse getOverview(UUID advisorUserId) {
        Advisor advisor = advisorRepository.findByUserIdAndIsActiveTrue(advisorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No active advisor access"));

        School school = advisor.getSchool();
        List<UUID> studentIds = affiliationRepository.findUserIdsBySchoolIdAndStatus(
                school.getId(), AffiliationStatus.ACTIVE);

        if (studentIds.isEmpty()) {
            return new SchoolOverviewResponse(
                    school.getId(), school.getName(),
                    0, 0, 0, 0, 0,
                    0, 0, null,
                    List.of(), List.of()
            );
        }

        List<JobApplicationRepository.SchoolApplicationRow> applications =
                jobApplicationRepository.findSchoolApplicationsByUserIds(studentIds);
        Map<UUID, StudentConsent> consentsByUser = consentRepository
                .findByAffiliationSchoolId(school.getId())
                .stream()
                .collect(Collectors.toMap(StudentConsent::getUserId, Function.identity(), (left, right) -> left));
        List<JobApplicationRepository.SchoolApplicationRow> fullVisibilityApplications = applications.stream()
                .filter(app -> allowsFullApplicationDetails(consentsByUser.get(app.getUserId())))
                .toList();
        long applicationsTracked = applications.size();
        long recruiterResponses = applications.stream()
                .filter(app -> app.getFirstRespondedAt() != null)
                .count();
        long interviews = applications.stream()
                .filter(app -> isStatusIn(app.getApplicationStatus(), INTERVIEW_STATUSES))
                .count();
        long offers = applications.stream()
                .filter(app -> isStatusIn(app.getApplicationStatus(), OFFER_STATUSES))
                .count();

        LocalDateTime latestActivityAt = applications.stream()
                .map(JobApplicationRepository.SchoolApplicationRow::getUpdatedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return new SchoolOverviewResponse(
                school.getId(),
                school.getName(),
                studentIds.size(),
                applicationsTracked,
                recruiterResponses,
                interviews,
                offers,
                ratio(recruiterResponses, applicationsTracked),
                ratio(interviews, applicationsTracked),
                latestActivityAt,
                topBreakdown(applications, JobApplicationRepository.SchoolApplicationRow::getRoleCategory),
                topBreakdown(fullVisibilityApplications, JobApplicationRepository.SchoolApplicationRow::getCompany)
        );
    }

    private boolean allowsFullApplicationDetails(StudentConsent consent) {
        return consent != null
                && consent.getRevokedAt() == null
                && consent.getAdvisorVisibilityLevel() == AdvisorVisibilityLevel.FULL;
    }

    private boolean isStatusIn(String status, List<String> statuses) {
        if (status == null) return false;
        return statuses.stream().anyMatch(s -> s.equalsIgnoreCase(status));
    }

    private double ratio(long numerator, long denominator) {
        return denominator > 0 ? (double) numerator / denominator : 0;
    }

    private List<SchoolOverviewResponse.BreakdownItem> topBreakdown(
            List<JobApplicationRepository.SchoolApplicationRow> applications,
            Function<JobApplicationRepository.SchoolApplicationRow, String> classifier) {
        return applications.stream()
                .map(classifier)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(5)
                .map(entry -> new SchoolOverviewResponse.BreakdownItem(entry.getKey(), entry.getValue()))
                .toList();
    }
}
