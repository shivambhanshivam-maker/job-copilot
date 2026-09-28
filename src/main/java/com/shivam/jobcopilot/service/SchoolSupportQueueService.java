package com.shivam.jobcopilot.service;

import com.shivam.jobcopilot.dto.SchoolSupportQueueResponse;
import com.shivam.jobcopilot.dto.SchoolSupportQueueActionRequest;
import com.shivam.jobcopilot.dto.SchoolStudentApplicationsResponse;
import com.shivam.jobcopilot.dto.SchoolStudentRosterResponse;
import com.shivam.jobcopilot.entity.Advisor;
import com.shivam.jobcopilot.entity.AdvisorVisibilityLevel;
import com.shivam.jobcopilot.entity.AdvisorSupportAction;
import com.shivam.jobcopilot.entity.AffiliationStatus;
import com.shivam.jobcopilot.entity.JobSearchStatus;
import com.shivam.jobcopilot.entity.School;
import com.shivam.jobcopilot.entity.StudentConsent;
import com.shivam.jobcopilot.entity.StudentSchoolAffiliation;
import com.shivam.jobcopilot.entity.User;
import com.shivam.jobcopilot.repository.AdvisorRepository;
import com.shivam.jobcopilot.repository.AdvisorSupportActionRepository;
import com.shivam.jobcopilot.repository.JobApplicationRepository;
import com.shivam.jobcopilot.repository.StudentConsentRepository;
import com.shivam.jobcopilot.repository.StudentSchoolAffiliationRepository;
import com.shivam.jobcopilot.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class SchoolSupportQueueService {

    private static final List<String> INTERVIEW_STATUSES = List.of("Interview", "Offer");
    private static final List<String> OFFER_STATUSES = List.of("Offer");

    private final AdvisorRepository advisorRepository;
    private final AdvisorSupportActionRepository supportActionRepository;
    private final StudentSchoolAffiliationRepository affiliationRepository;
    private final StudentConsentRepository consentRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final UserRepository userRepository;

    public SchoolSupportQueueService(AdvisorRepository advisorRepository,
                                     AdvisorSupportActionRepository supportActionRepository,
                                     StudentSchoolAffiliationRepository affiliationRepository,
                                     StudentConsentRepository consentRepository,
                                     JobApplicationRepository jobApplicationRepository,
                                     UserRepository userRepository) {
        this.advisorRepository = advisorRepository;
        this.supportActionRepository = supportActionRepository;
        this.affiliationRepository = affiliationRepository;
        this.consentRepository = consentRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public SchoolSupportQueueResponse getSupportQueue(UUID advisorUserId) {
        Advisor advisor = advisorRepository.findByUserIdAndIsActiveTrue(advisorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No active advisor access"));

        School school = advisor.getSchool();
        Map<UUID, StudentConsent> consentsByUser = consentMap(school.getId());
        List<StudentSchoolAffiliation> eligibleAffiliations = affiliationRepository
                .findBySchoolIdAndStatusAndJobSearchStatus(
                        school.getId(), AffiliationStatus.ACTIVE, JobSearchStatus.ACTIVE)
                .stream()
                .filter(affiliation -> allowsVisibility(consentsByUser.get(affiliation.getUserId())))
                .toList();

        if (eligibleAffiliations.isEmpty()) {
            return new SchoolSupportQueueResponse(school.getId(), school.getName(), 0, List.of());
        }

        List<UUID> studentIds = eligibleAffiliations.stream()
                .map(StudentSchoolAffiliation::getUserId)
                .toList();
        Map<UUID, User> usersById = StreamSupport.stream(userRepository.findAllById(studentIds).spliterator(), false)
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<UUID, List<com.shivam.jobcopilot.repository.JobApplicationRepository.SchoolApplicationRow>> applicationsByUser =
                jobApplicationRepository.findSchoolApplicationsByUserIds(studentIds)
                .stream()
                .collect(Collectors.groupingBy(com.shivam.jobcopilot.repository.JobApplicationRepository.SchoolApplicationRow::getUserId));
        Map<String, AdvisorSupportAction> actionsBySignal = supportActionRepository
                .findByAdvisorUserIdAndStudentUserIdIn(advisorUserId, studentIds)
                .stream()
                .collect(Collectors.toMap(
                        action -> actionKey(action.getStudentUserId(), action.getSignalType(), action.getSignalFingerprint()),
                        Function.identity(),
                        (left, right) -> left
                ));

        List<SchoolSupportQueueResponse.SupportQueueItem> items = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (StudentSchoolAffiliation affiliation : eligibleAffiliations) {
            UUID studentId = affiliation.getUserId();
            List<com.shivam.jobcopilot.repository.JobApplicationRepository.SchoolApplicationRow> applications = applicationsByUser.getOrDefault(studentId, List.of());
            List<SchoolSupportQueueResponse.SupportSignal> signals = buildSignals(applications, now)
                    .stream()
                    .filter(signal -> !isSuppressed(studentId, signal, actionsBySignal, now))
                    .toList();
            if (signals.isEmpty()) continue;

            User user = usersById.get(studentId);
            StudentConsent consent = consentsByUser.get(studentId);
            items.add(new SchoolSupportQueueResponse.SupportQueueItem(
                    studentId,
                    user != null ? user.getName() : null,
                    user != null ? user.getEmail() : null,
                    consent != null ? consent.getAdvisorVisibilityLevel().name() : "NONE",
                    maxSeverity(signals),
                    latestActivityAt(applications),
                    buildSummary(applications, now),
                    signals
            ));
        }

        items.sort(Comparator
                .comparingInt((SchoolSupportQueueResponse.SupportQueueItem item) -> severityRank(item.severity()))
                .thenComparing(SchoolSupportQueueResponse.SupportQueueItem::lastActivityAt,
                        Comparator.nullsLast(Comparator.reverseOrder())));

        return new SchoolSupportQueueResponse(
                school.getId(),
                school.getName(),
                eligibleAffiliations.size(),
                items
        );
    }

    @Transactional
    public void markSupportSignalReviewed(UUID advisorUserId, SchoolSupportQueueActionRequest request) {
        AdvisorSupportAction action = getOrCreateAction(advisorUserId, request);
        action.setReviewedAt(LocalDateTime.now());
        action.setSnoozedUntil(null);
        supportActionRepository.save(action);
    }

    @Transactional
    public void snoozeSupportSignal(UUID advisorUserId, SchoolSupportQueueActionRequest request) {
        if (request.snoozeDays() == null || !List.of(7, 14, 30).contains(request.snoozeDays())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Snooze duration must be 7, 14, or 30 days");
        }

        AdvisorSupportAction action = getOrCreateAction(advisorUserId, request);
        action.setReviewedAt(null);
        action.setSnoozedUntil(LocalDateTime.now().plusDays(request.snoozeDays()));
        supportActionRepository.save(action);
    }

    private AdvisorSupportAction getOrCreateAction(UUID advisorUserId,
                                                   SchoolSupportQueueActionRequest request) {
        if (request == null
                || request.studentUserId() == null
                || request.signalType() == null || request.signalType().isBlank()
                || request.signalFingerprint() == null || request.signalFingerprint().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student, signal type, and signal fingerprint are required");
        }

        Advisor advisor = advisorRepository.findByUserIdAndIsActiveTrue(advisorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No active advisor access"));
        School school = advisor.getSchool();
        StudentSchoolAffiliation affiliation = affiliationRepository
                .findByUserIdAndSchoolId(request.studentUserId(), school.getId())
                .filter(candidate -> candidate.getStatus() == AffiliationStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found in advisor school"));
        consentRepository.findByUserIdAndAffiliationSchoolId(affiliation.getUserId(), school.getId())
                .filter(StudentConsent::allowsAdvisorVisibility)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Student has not shared support visibility"));

        return supportActionRepository
                .findByAdvisorUserIdAndStudentUserIdAndSignalTypeAndSignalFingerprint(
                        advisorUserId,
                        request.studentUserId(),
                        request.signalType(),
                        request.signalFingerprint()
                )
                .orElseGet(() -> {
                    AdvisorSupportAction action = new AdvisorSupportAction();
                    action.setAdvisorUserId(advisorUserId);
                    action.setStudentUserId(request.studentUserId());
                    action.setSignalType(request.signalType());
                    action.setSignalFingerprint(request.signalFingerprint());
                    return action;
                });
    }

    @Transactional(readOnly = true)
    public SchoolStudentRosterResponse getStudentRoster(UUID advisorUserId) {
        Advisor advisor = advisorRepository.findByUserIdAndIsActiveTrue(advisorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No active advisor access"));

        School school = advisor.getSchool();
        Map<UUID, StudentConsent> consentsByUser = consentMap(school.getId());
        List<StudentSchoolAffiliation> eligibleAffiliations = affiliationRepository
                .findBySchoolIdAndStatus(school.getId(), AffiliationStatus.ACTIVE)
                .stream()
                .filter(affiliation -> allowsVisibility(consentsByUser.get(affiliation.getUserId())))
                .toList();

        if (eligibleAffiliations.isEmpty()) {
            return new SchoolStudentRosterResponse(school.getId(), school.getName(), List.of());
        }

        List<UUID> studentIds = eligibleAffiliations.stream()
                .map(StudentSchoolAffiliation::getUserId)
                .toList();
        Map<UUID, User> usersById = StreamSupport.stream(userRepository.findAllById(studentIds).spliterator(), false)
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<UUID, List<com.shivam.jobcopilot.repository.JobApplicationRepository.SchoolApplicationRow>> applicationsByUser =
                jobApplicationRepository.findSchoolApplicationsByUserIds(studentIds)
                .stream()
                .collect(Collectors.groupingBy(com.shivam.jobcopilot.repository.JobApplicationRepository.SchoolApplicationRow::getUserId));

        LocalDateTime now = LocalDateTime.now();
        List<SchoolStudentRosterResponse.StudentRosterItem> students = eligibleAffiliations.stream()
                .map(affiliation -> {
                    UUID studentId = affiliation.getUserId();
                    User user = usersById.get(studentId);
                    StudentConsent consent = consentsByUser.get(studentId);
                    List<com.shivam.jobcopilot.repository.JobApplicationRepository.SchoolApplicationRow> applications = applicationsByUser.getOrDefault(studentId, List.of());
                    List<SchoolSupportQueueResponse.SupportSignal> signals = buildSignals(applications, now);

                    return new SchoolStudentRosterResponse.StudentRosterItem(
                            studentId,
                            user != null ? user.getName() : null,
                            user != null ? user.getEmail() : null,
                            affiliation.getProgram() != null ? affiliation.getProgram().getName() : null,
                            affiliation.getCohort() != null ? affiliation.getCohort().getName() : null,
                            affiliation.getJobSearchStatus() != null ? affiliation.getJobSearchStatus().name() : null,
                            consent != null ? consent.getAdvisorVisibilityLevel().name() : "NONE",
                            signals.isEmpty() ? "NONE" : maxSeverity(signals),
                            signals.size(),
                            latestActivityAt(applications),
                            buildSummary(applications, now)
                    );
                })
                .sorted(Comparator
                        .comparing((SchoolStudentRosterResponse.StudentRosterItem item) -> item.studentName() == null ? "" : item.studentName()))
                .toList();

        return new SchoolStudentRosterResponse(school.getId(), school.getName(), students);
    }

    @Transactional(readOnly = true)
    public SchoolStudentApplicationsResponse getStudentApplications(UUID advisorUserId, UUID studentUserId) {
        Advisor advisor = advisorRepository.findByUserIdAndIsActiveTrue(advisorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No active advisor access"));

        School school = advisor.getSchool();
        StudentSchoolAffiliation affiliation = affiliationRepository
                .findByUserIdAndSchoolId(studentUserId, school.getId())
                .filter(candidate -> candidate.getStatus() == AffiliationStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found in advisor school"));

        StudentConsent consent = consentRepository
                .findByUserIdAndAffiliationSchoolId(affiliation.getUserId(), school.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No advisor visibility consent"));

        if (consent.getRevokedAt() != null || consent.getAdvisorVisibilityLevel() != AdvisorVisibilityLevel.FULL) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student has not shared application details");
        }

        User user = userRepository.findById(studentUserId).orElse(null);
        List<SchoolStudentApplicationsResponse.ApplicationItem> applications = jobApplicationRepository
                .findSchoolApplicationsByUserIds(List.of(studentUserId))
                .stream()
                .sorted(Comparator.comparing(JobApplicationRepository.SchoolApplicationRow::getUpdatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(app -> new SchoolStudentApplicationsResponse.ApplicationItem(
                        app.getId(),
                        app.getCompany(),
                        app.getJobTitle(),
                        app.getRoleCategory(),
                        app.getApplicationStatus(),
                        app.getCreatedAt(),
                        app.getUpdatedAt(),
                        app.getFirstRespondedAt(),
                        app.getInterviewDate()
                ))
                .toList();

        return new SchoolStudentApplicationsResponse(
                studentUserId,
                user != null ? user.getName() : null,
                user != null ? user.getEmail() : null,
                applications
        );
    }

    private List<SchoolSupportQueueResponse.SupportSignal> buildSignals(
                                                                        List<JobApplicationRepository.SchoolApplicationRow> applications,
                                                                        LocalDateTime now) {
        if (applications.isEmpty()) {
            return List.of(new SchoolSupportQueueResponse.SupportSignal(
                    "NO_RECENT_ACTIVITY",
                    "HIGH",
                    "No tracked application activity yet for an actively searching student.",
                    List.of("0 tracked applications"),
                    "Check in and confirm whether the student wants help starting or tracking their search."
            ));
        }

        List<SchoolSupportQueueResponse.SupportSignal> signals = new ArrayList<>();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);
        LocalDateTime twentyOneDaysAgo = now.minusDays(21);

        long appsLast30 = applications.stream()
                .filter(app -> app.getCreatedAt() != null && !app.getCreatedAt().isBefore(thirtyDaysAgo))
                .count();
        long responsesLast30 = applications.stream()
                .filter(app -> app.getFirstRespondedAt() != null && !app.getFirstRespondedAt().isBefore(thirtyDaysAgo))
                .count();
        if (appsLast30 >= 10 && responsesLast30 == 0) {
            String severity = appsLast30 >= 15 ? "HIGH" : "MEDIUM";
            signals.add(new SchoolSupportQueueResponse.SupportSignal(
                    "HIGH_ACTIVITY_NO_RESPONSES",
                    severity,
                    "High application activity with no recruiter responses in the last 30 days.",
                    List.of(appsLast30 + " applications in the last 30 days", "0 recruiter responses in the last 30 days"),
                    "Offer a resume targeting or application strategy review."
            ));
        }

        long interviews = applications.stream()
                .filter(app -> isStatusIn(app.getApplicationStatus(), INTERVIEW_STATUSES))
                .count();
        long offers = applications.stream()
                .filter(app -> isStatusIn(app.getApplicationStatus(), OFFER_STATUSES))
                .count();
        if (interviews >= 3 && offers == 0) {
            String severity = interviews >= 5 ? "HIGH" : "MEDIUM";
            signals.add(new SchoolSupportQueueResponse.SupportSignal(
                    "INTERVIEWS_NO_OFFERS",
                    severity,
                    "The student is reaching interviews but has no offers recorded.",
                    List.of(interviews + " interviews tracked", "0 offers recorded"),
                    "Offer mock interview practice or an interview debrief."
            ));
        }

        long staleApplied = applications.stream()
                .filter(app -> "Applied".equalsIgnoreCase(app.getApplicationStatus()))
                .filter(app -> app.getUpdatedAt() != null && app.getUpdatedAt().isBefore(thirtyDaysAgo))
                .count();
        if (staleApplied >= 5) {
            String severity = staleApplied >= 10 ? "HIGH" : "MEDIUM";
            signals.add(new SchoolSupportQueueResponse.SupportSignal(
                    "STALE_APPLIED_PIPELINE",
                    severity,
                    "Several applications have been in Applied with no update for 30+ days.",
                    List.of(staleApplied + " stale Applied applications"),
                    "Review follow-up strategy, channel mix, and referral opportunities."
            ));
        }

        LocalDateTime latestActivityAt = latestActivityAt(applications);
        if (latestActivityAt == null || latestActivityAt.isBefore(twentyOneDaysAgo)) {
            long days = latestActivityAt == null
                    ? 0
                    : java.time.Duration.between(latestActivityAt, now).toDays();
            String severity = latestActivityAt == null || latestActivityAt.isBefore(now.minusDays(30)) ? "HIGH" : "MEDIUM";
            List<String> evidence = latestActivityAt == null
                    ? List.of("No tracked application activity")
                    : List.of("Last tracked activity was " + days + " days ago");
            signals.add(new SchoolSupportQueueResponse.SupportSignal(
                    "NO_RECENT_ACTIVITY",
                    severity,
                    "The student is marked active but has no recent tracked activity.",
                    evidence,
                    "Check in and confirm whether they are still actively searching or need support."
            ));
        }

        return signals;
    }

    private SchoolSupportQueueResponse.SupportSummary buildSummary(
                                                                   List<JobApplicationRepository.SchoolApplicationRow> applications,
                                                                   LocalDateTime now) {
        LocalDateTime thirtyDaysAgo = now.minusDays(30);
        long applicationsLast30 = applications.stream()
                .filter(app -> app.getCreatedAt() != null && !app.getCreatedAt().isBefore(thirtyDaysAgo))
                .count();
        long responsesLast30 = applications.stream()
                .filter(app -> app.getFirstRespondedAt() != null && !app.getFirstRespondedAt().isBefore(thirtyDaysAgo))
                .count();
        long interviews = applications.stream()
                .filter(app -> isStatusIn(app.getApplicationStatus(), INTERVIEW_STATUSES))
                .count();
        long offers = applications.stream()
                .filter(app -> isStatusIn(app.getApplicationStatus(), OFFER_STATUSES))
                .count();

        return new SchoolSupportQueueResponse.SupportSummary(
                applications.size(),
                applicationsLast30,
                responsesLast30,
                interviews,
                offers,
                latestActivityAt(applications),
                topBuckets(applications, JobApplicationRepository.SchoolApplicationRow::getRoleCategory),
                topBuckets(applications, JobApplicationRepository.SchoolApplicationRow::getApplicationStatus)
        );
    }

    private List<SchoolSupportQueueResponse.SummaryBucket> topBuckets(
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
                .map(entry -> new SchoolSupportQueueResponse.SummaryBucket(entry.getKey(), entry.getValue()))
                .toList();
    }

    private LocalDateTime latestActivityAt(List<JobApplicationRepository.SchoolApplicationRow> applications) {
        return applications.stream()
                .map(JobApplicationRepository.SchoolApplicationRow::getUpdatedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private Map<UUID, StudentConsent> consentMap(UUID schoolId) {
        return consentRepository.findByAffiliationSchoolId(schoolId)
                .stream()
                .collect(Collectors.toMap(StudentConsent::getUserId, Function.identity(), (left, right) -> left));
    }

    private boolean allowsVisibility(StudentConsent consent) {
        return consent != null && consent.allowsAdvisorVisibility();
    }

    private boolean isSuppressed(UUID studentUserId,
                                 SchoolSupportQueueResponse.SupportSignal signal,
                                 Map<String, AdvisorSupportAction> actionsBySignal,
                                 LocalDateTime now) {
        AdvisorSupportAction action = actionsBySignal.get(actionKey(studentUserId, signal.type(), signal.fingerprint()));
        if (action == null) return false;
        if (action.getReviewedAt() != null) return true;
        return action.getSnoozedUntil() != null && action.getSnoozedUntil().isAfter(now);
    }

    private String actionKey(UUID studentUserId, String signalType, String signalFingerprint) {
        return studentUserId + "|" + signalType + "|" + signalFingerprint;
    }

    private boolean isStatusIn(String status, List<String> statuses) {
        if (status == null) return false;
        return statuses.stream().anyMatch(s -> s.equalsIgnoreCase(status));
    }

    private String maxSeverity(List<SchoolSupportQueueResponse.SupportSignal> signals) {
        Map<String, Integer> ranks = new HashMap<>();
        ranks.put("HIGH", 0);
        ranks.put("MEDIUM", 1);
        ranks.put("LOW", 2);
        return signals.stream()
                .map(SchoolSupportQueueResponse.SupportSignal::severity)
                .min(Comparator.comparingInt(severity -> ranks.getOrDefault(severity, 99)))
                .orElse("LOW");
    }

    private int severityRank(String severity) {
        return switch (severity) {
            case "HIGH" -> 0;
            case "MEDIUM" -> 1;
            case "LOW" -> 2;
            default -> 99;
        };
    }
}
