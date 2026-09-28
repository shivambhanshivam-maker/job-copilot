package com.shivam.jobcopilot.service;

import com.shivam.jobcopilot.dto.PeerEvidencePatternResponse;
import com.shivam.jobcopilot.entity.AffiliationStatus;
import com.shivam.jobcopilot.entity.FitRequirement;
import com.shivam.jobcopilot.entity.School;
import com.shivam.jobcopilot.entity.StudentSchoolAffiliation;
import com.shivam.jobcopilot.repository.FitAnalysisRepository;
import com.shivam.jobcopilot.repository.JobApplicationRepository;
import com.shivam.jobcopilot.repository.StudentSchoolAffiliationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PeerEvidenceAggregatorService {

    private static final int OBSERVATION_WINDOW_DAYS = 45;
    private static final int MIN_INTERVIEW_APPLICATIONS = 5;
    private static final int MIN_INTERVIEW_STUDENTS = 3;
    private static final int MIN_COMPARISON_APPLICATIONS = 5;
    private static final int MIN_COMPARISON_STUDENTS = 3;
    private static final int MIN_DIFFERENCE_PERCENTAGE_POINTS = 20;

    private final StudentSchoolAffiliationRepository affiliationRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final FitAnalysisRepository fitAnalysisRepository;

    public PeerEvidenceAggregatorService(StudentSchoolAffiliationRepository affiliationRepository,
                                         JobApplicationRepository jobApplicationRepository,
                                         FitAnalysisRepository fitAnalysisRepository) {
        this.affiliationRepository = affiliationRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.fitAnalysisRepository = fitAnalysisRepository;
    }

    @Transactional(readOnly = true)
    public PeerEvidencePatternResponse getForStudent(UUID studentUserId) {
        StudentSchoolAffiliation affiliation = affiliationRepository.findByUserId(studentUserId).stream()
                .filter(candidate -> candidate.getStatus() == AffiliationStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active school affiliation"));

        School school = affiliation.getSchool();
        List<UUID> peerIds = affiliationRepository.findUserIdsBySchoolIdAndStatus(
                        school.getId(), AffiliationStatus.ACTIVE)
                .stream()
                .filter(peerId -> !studentUserId.equals(peerId))
                .distinct()
                .toList();

        if (peerIds.isEmpty()) {
            return new PeerEvidencePatternResponse(school.getId(), school.getName(), 0, List.of());
        }

        List<UUID> userIds = new ArrayList<>(peerIds);
        userIds.add(studentUserId);
        List<ApplicationSnapshot> allApplications = jobApplicationRepository
                .findPeerEvidenceApplicationsByUserIds(userIds)
                .stream()
                .map(row -> new ApplicationSnapshot(
                        row.getUserId(),
                        row.getRoleCategory(),
                        row.getApplicationStatus(),
                        row.getInterviewDate(),
                        row.getCreatedAt(),
                        row.getUpdatedAt(),
                        row.getFitAnalysisId()))
                .toList();
        List<ApplicationSnapshot> applications = allApplications.stream()
                .filter(application -> peerIds.contains(application.userId()))
                .toList();
        List<ApplicationSnapshot> currentStudentApplications = allApplications.stream()
                .filter(application -> studentUserId.equals(application.userId()))
                .toList();
        List<UUID> analysisIds = allApplications.stream()
                .map(ApplicationSnapshot::fitAnalysisId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, AnalysisData> analysesById = loadAnalysisData(analysisIds);

        List<ApplicationEvidence> eligibleApplications = applications.stream()
                .map(application -> toEligibleApplication(application, analysesById.get(application.fitAnalysisId())))
                .filter(Objects::nonNull)
                .toList();
        List<ApplicationEvidence> currentStudentEvidence = currentStudentApplications.stream()
                .map(application -> toEvidenceApplication(application, analysesById.get(application.fitAnalysisId()), false))
                .filter(Objects::nonNull)
                .toList();
        Map<String, StudentEvidenceSummary> currentEvidenceByCapability = summarizeCurrentStudentEvidence(currentStudentEvidence);

        Map<String, List<ApplicationEvidence>> byCategory = eligibleApplications.stream()
                .collect(Collectors.groupingBy(ApplicationEvidence::roleCategory, LinkedHashMap::new, Collectors.toList()));

        List<PeerEvidencePatternResponse.Category> categories = byCategory.entrySet().stream()
                .map(entry -> buildCategory(entry.getKey(), entry.getValue(), currentEvidenceByCapability))
                .filter(Objects::nonNull)
                .toList();

        return new PeerEvidencePatternResponse(school.getId(), school.getName(), peerIds.size(), categories);
    }

    private PeerEvidencePatternResponse.Category buildCategory(String roleCategory,
                                                                List<ApplicationEvidence> applications,
                                                                Map<String, StudentEvidenceSummary> currentEvidenceByCapability) {
        Map<Boolean, List<ApplicationEvidence>> byInterview = applications.stream()
                .collect(Collectors.partitioningBy(ApplicationEvidence::interviewReached));
        List<ApplicationEvidence> interviewApplications = byInterview.getOrDefault(true, List.of());
        List<ApplicationEvidence> nonInterviewApplications = byInterview.getOrDefault(false, List.of());

        Set<UUID> interviewStudents = interviewApplications.stream()
                .map(ApplicationEvidence::studentId)
                .collect(Collectors.toSet());
        Set<UUID> nonInterviewStudents = nonInterviewApplications.stream()
                .map(ApplicationEvidence::studentId)
                .collect(Collectors.toSet());

        if (interviewApplications.size() < MIN_INTERVIEW_APPLICATIONS
                || interviewStudents.size() < MIN_INTERVIEW_STUDENTS
                || nonInterviewApplications.size() < MIN_COMPARISON_APPLICATIONS
                || nonInterviewStudents.size() < MIN_COMPARISON_STUDENTS) {
            return null;
        }

        Map<String, List<ApplicationEvidence>> byCapability = applications.stream()
                .flatMap(application -> application.requirements().stream()
                        .map(requirement -> new CapabilityMention(application, requirement)))
                .collect(Collectors.groupingBy(
                         mention -> normalizeCapability(mention.requirement().requirement().getCapabilityPhrase()),
                        LinkedHashMap::new,
                        Collectors.mapping(CapabilityMention::application, Collectors.toList())
                ));

        List<PeerEvidencePatternResponse.Pattern> patterns = byCapability.entrySet().stream()
                .map(entry -> buildPattern(roleCategory, entry.getKey(), entry.getValue(), currentEvidenceByCapability))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(PeerEvidencePatternResponse.Pattern::differencePercentagePoints).reversed())
                .limit(5)
                .toList();

        if (patterns.isEmpty()) return null;
        return new PeerEvidencePatternResponse.Category(
                roleCategory,
                interviewApplications.size(),
                interviewStudents.size(),
                nonInterviewApplications.size(),
                nonInterviewStudents.size(),
                patterns
        );
    }

    private PeerEvidencePatternResponse.Pattern buildPattern(String roleCategory,
                                                              String normalizedCapability,
                                                              List<ApplicationEvidence> mentions,
                                                              Map<String, StudentEvidenceSummary> currentEvidenceByCapability) {
        String displayCapability = mentions.stream()
                .map(ApplicationEvidence::requirements)
                .flatMap(List::stream)
                 .filter(requirement -> normalizeCapability(requirement.requirement().getCapabilityPhrase()).equals(normalizedCapability))
                 .map(requirement -> requirement.requirement().getCapabilityPhrase())
                .filter(this::present)
                .findFirst()
                .orElse(normalizedCapability);

        Map<Boolean, List<ApplicationEvidence>> byInterview = mentions.stream()
                .collect(Collectors.partitioningBy(ApplicationEvidence::interviewReached));
        List<ApplicationEvidence> interviewMentions = byInterview.getOrDefault(true, List.of());
        List<ApplicationEvidence> nonInterviewMentions = byInterview.getOrDefault(false, List.of());

        EvidenceStats interviewStats = evidenceStats(interviewMentions, normalizedCapability);
        EvidenceStats nonInterviewStats = evidenceStats(nonInterviewMentions, normalizedCapability);
        if (interviewStats.requirementApplications() == 0 || nonInterviewStats.requirementApplications() == 0) return null;

        int difference = interviewStats.evidenceRatePercentage() - nonInterviewStats.evidenceRatePercentage();
        if (difference < MIN_DIFFERENCE_PERCENTAGE_POINTS) return null;

        StudentEvidenceSummary currentEvidence = currentEvidenceByCapability.getOrDefault(
                evidenceKey(roleCategory, normalizedCapability), StudentEvidenceSummary.notAssessed());

        return new PeerEvidencePatternResponse.Pattern(
                displayCapability,
                interviewStats.requirementApplications(),
                interviewStats.requirementStudents(),
                interviewStats.evidenceStudents(),
                interviewStats.evidenceRate(),
                nonInterviewStats.requirementApplications(),
                nonInterviewStats.requirementStudents(),
                nonInterviewStats.evidenceStudents(),
                nonInterviewStats.evidenceRate(),
                difference,
                currentEvidence.status(),
                currentEvidence.applicationsWithRequirement(),
                currentEvidence.evidenceRate()
        );
    }

    private EvidenceStats evidenceStats(List<ApplicationEvidence> applications, String normalizedCapability) {
        Map<UUID, List<ApplicationEvidence>> byStudent = applications.stream()
                .collect(Collectors.groupingBy(ApplicationEvidence::studentId));
        int evidenceStudents = (int) byStudent.values().stream()
                .filter(studentApplications -> studentApplications.stream()
                        .flatMap(application -> application.requirements().stream())
                        .filter(requirement -> normalizeCapability(requirement.requirement().getCapabilityPhrase()).equals(normalizedCapability))
                        .map(ApplicationRequirement::evidenceStatus)
                        .anyMatch(this::hasCredibleEvidence))
                .count();
        return new EvidenceStats(
                applications.size(),
                byStudent.size(),
                evidenceStudents,
                byStudent.isEmpty() ? 0 : (double) evidenceStudents / byStudent.size()
        );
    }

    private Map<UUID, AnalysisData> loadAnalysisData(List<UUID> analysisIds) {
        if (analysisIds.isEmpty()) return Map.of();

        Map<UUID, Map<String, ApplicationRequirement>> requirementsByAnalysis = new LinkedHashMap<>();
        Map<UUID, UUID> userIdsByAnalysis = new HashMap<>();
        for (FitAnalysisRepository.PeerEvidenceRequirementRow row : fitAnalysisRepository
                .findPeerEvidenceRequirementsByAnalysisIds(analysisIds)) {
            if (row.getFitAnalysisId() == null || !present(row.getCapabilityPhrase())) continue;
            userIdsByAnalysis.putIfAbsent(row.getFitAnalysisId(), row.getUserId());
            requirementsByAnalysis
                    .computeIfAbsent(row.getFitAnalysisId(), ignored -> new LinkedHashMap<>())
                    .putIfAbsent(row.getRequirementKey(), new ApplicationRequirement(
                            new FitRequirement(
                                    row.getRequirementKey(),
                                    row.getRequirementText(),
                                    row.getCapabilityPhrase(),
                                    row.getImportanceTier(),
                                    row.getRelevanceMode(),
                                    row.getEvidenceType(),
                                    row.getSourceExcerpt()),
                            row.getEvidenceStatus()));
        }

        Map<UUID, AnalysisData> result = new HashMap<>();
        requirementsByAnalysis.forEach((analysisId, requirements) -> result.put(
                analysisId,
                new AnalysisData(analysisId, userIdsByAnalysis.get(analysisId), List.copyOf(requirements.values()))));
        return result;
    }

    private ApplicationEvidence toEligibleApplication(ApplicationSnapshot application, AnalysisData analysis) {
        return toEvidenceApplication(application, analysis, true);
    }

    private ApplicationEvidence toEvidenceApplication(ApplicationSnapshot application, AnalysisData analysis,
                                                       boolean requireResolvedOutcome) {
        if (!present(application.roleCategory()) || analysis == null || analysis.requirements() == null) return null;
        if (!application.userId().equals(analysis.userId()) && analysis.userId() != null) return null;
        Outcome outcome = classifyOutcome(application);
        if (requireResolvedOutcome && outcome == Outcome.EXCLUDED) return null;

        List<ApplicationRequirement> requirements = analysis.requirements();
        if (requirements.isEmpty()) return null;

        return new ApplicationEvidence(
                application.userId(),
                application.roleCategory().trim(),
                outcome == Outcome.INTERVIEW,
                requirements
        );
    }

    private Map<String, StudentEvidenceSummary> summarizeCurrentStudentEvidence(
            List<ApplicationEvidence> applications) {
        Map<String, List<ApplicationEvidence>> applicationsByCapability = new HashMap<>();
        for (ApplicationEvidence application : applications) {
            Set<String> capabilitiesInApplication = new java.util.HashSet<>();
            for (ApplicationRequirement requirement : application.requirements()) {
                String normalizedCapability = normalizeCapability(requirement.requirement().getCapabilityPhrase());
                if (!present(normalizedCapability) || !capabilitiesInApplication.add(normalizedCapability)) continue;
                applicationsByCapability
                        .computeIfAbsent(evidenceKey(application.roleCategory(), normalizedCapability), ignored -> new ArrayList<>())
                        .add(application);
            }
        }

        Map<String, StudentEvidenceSummary> summaries = new HashMap<>();
        applicationsByCapability.forEach((key, matchingApplications) -> {
            int applicationsWithEvidence = 0;
            boolean hasStrong = false;
            boolean hasPartial = false;
            boolean hasMissing = false;
            boolean hasUnknown = false;
            for (ApplicationEvidence application : matchingApplications) {
                boolean applicationHasEvidence = false;
                for (ApplicationRequirement requirement : application.requirements()) {
                    if (!normalizeCapability(requirement.requirement().getCapabilityPhrase())
                            .equals(key.substring(key.indexOf('|') + 1))) continue;
                    String status = safe(requirement.evidenceStatus());
                    hasStrong |= "Strong".equalsIgnoreCase(status);
                    hasPartial |= isIntermediateEvidence(status);
                    hasMissing |= "Missing".equalsIgnoreCase(status);
                    hasUnknown |= status.isBlank();
                    applicationHasEvidence |= hasCredibleEvidence(status);
                }
                if (applicationHasEvidence) applicationsWithEvidence++;
            }
            summaries.put(key, new StudentEvidenceSummary(
                    evidenceStatus(hasStrong, hasPartial, hasMissing, hasUnknown),
                    matchingApplications.size(),
                    matchingApplications.isEmpty() ? 0 : (double) applicationsWithEvidence / matchingApplications.size()
            ));
        });
        return summaries;
    }

    private String evidenceStatus(boolean hasStrong, boolean hasPartial, boolean hasMissing, boolean hasUnknown) {
        int observedStatuses = (hasStrong ? 1 : 0) + (hasPartial ? 1 : 0) + (hasMissing ? 1 : 0) + (hasUnknown ? 1 : 0);
        if (observedStatuses == 0 || hasUnknown) return "Not assessed";
        if (observedStatuses > 1) return "Mixed";
        if (hasStrong) return "Strong";
        if (hasPartial) return "Partial";
        return "Missing";
    }

    private String evidenceKey(String roleCategory, String normalizedCapability) {
        return roleCategory.trim().toLowerCase(Locale.ROOT) + "|" + normalizedCapability;
    }

    private Outcome classifyOutcome(ApplicationSnapshot application) {
        String status = safe(application.applicationStatus());
        if ("Interview".equalsIgnoreCase(status) || "Offer".equalsIgnoreCase(status)
                || application.interviewDate() != null) {
            return Outcome.INTERVIEW;
        }
        if ("Rejected".equalsIgnoreCase(status) || "Closed".equalsIgnoreCase(status)) {
            return Outcome.NO_INTERVIEW;
        }
        if (("Applied".equalsIgnoreCase(status) || "Referral Received".equalsIgnoreCase(status))
                && applicationDate(application) != null
                && applicationDate(application).isBefore(LocalDateTime.now().minusDays(OBSERVATION_WINDOW_DAYS))) {
            return Outcome.NO_INTERVIEW;
        }
        return Outcome.EXCLUDED;
    }

    private LocalDateTime applicationDate(ApplicationSnapshot application) {
        return application.createdAt() != null ? application.createdAt() : application.updatedAt();
    }

    private boolean hasCredibleEvidence(String status) {
        return "Strong".equalsIgnoreCase(safe(status))
                || isIntermediateEvidence(status);
    }

    private boolean isIntermediateEvidence(String status) {
        String normalized = safe(status);
        return "Good".equalsIgnoreCase(normalized)
                || "Weak".equalsIgnoreCase(normalized)
                || "Partial".equalsIgnoreCase(normalized);
    }

    private String normalizeCapability(String capability) {
        return safe(capability).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private enum Outcome { INTERVIEW, NO_INTERVIEW, EXCLUDED }

    private record ApplicationSnapshot(UUID userId, String roleCategory, String applicationStatus,
                                       LocalDateTime interviewDate, LocalDateTime createdAt,
                                       LocalDateTime updatedAt, UUID fitAnalysisId) {}

    private record AnalysisData(UUID id, UUID userId, List<ApplicationRequirement> requirements) {}

    private record ApplicationEvidence(UUID studentId, String roleCategory, boolean interviewReached,
                                       List<ApplicationRequirement> requirements) {}

    private record ApplicationRequirement(FitRequirement requirement, String evidenceStatus) {}

    private record CapabilityMention(ApplicationEvidence application, ApplicationRequirement requirement) {}

    private record EvidenceStats(int requirementApplications, int requirementStudents,
                                 int evidenceStudents, double evidenceRate) {
        int evidenceRatePercentage() { return (int) Math.round(evidenceRate * 100); }
    }

    private record StudentEvidenceSummary(String status, int applicationsWithRequirement,
                                          double evidenceRate) {
        static StudentEvidenceSummary notAssessed() {
            return new StudentEvidenceSummary("Not assessed", 0, 0);
        }
    }
}
