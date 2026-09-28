package com.shivam.jobcopilot.service;

import com.shivam.jobcopilot.dto.JobApplicationEmail;
import com.shivam.jobcopilot.dto.EmailReviewResponse;
import com.shivam.jobcopilot.dto.MergeDecision;
import com.shivam.jobcopilot.dto.PendingAction;
import com.shivam.jobcopilot.entity.ApplicationUpdate;
import com.shivam.jobcopilot.entity.EmailReviewItem;
import com.shivam.jobcopilot.entity.FitAnalysis;
import com.shivam.jobcopilot.entity.JobApplication;
import com.shivam.jobcopilot.entity.PostApplicationInsight;
import com.shivam.jobcopilot.repository.FitAnalysisRepository;
import com.shivam.jobcopilot.repository.EmailReviewItemRepository;
import com.shivam.jobcopilot.repository.JobApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class JobApplicationService {

    private static final Logger log = LoggerFactory.getLogger(JobApplicationService.class);

    private final JobApplicationRepository repository;
    private final FitAnalysisRepository fitAnalysisRepository;
    private final EmailReviewItemRepository emailReviewItemRepository;
    private final EmailMergeDecisionService mergeDecisionService;
    private final PostApplicationInsightService insightService;
    private final StudentInsightService studentInsightService;
    private final ManualApplicationFitAnalysisService manualApplicationFitAnalysisService;

    public JobApplicationService(JobApplicationRepository repository,
                                 FitAnalysisRepository fitAnalysisRepository,
                                 EmailReviewItemRepository emailReviewItemRepository,
                                 EmailMergeDecisionService mergeDecisionService,
                                 PostApplicationInsightService insightService,
                                 StudentInsightService studentInsightService,
                                 ManualApplicationFitAnalysisService manualApplicationFitAnalysisService) {
        this.repository = repository;
        this.fitAnalysisRepository = fitAnalysisRepository;
        this.emailReviewItemRepository = emailReviewItemRepository;
        this.mergeDecisionService = mergeDecisionService;
        this.insightService = insightService;
        this.studentInsightService = studentInsightService;
        this.manualApplicationFitAnalysisService = manualApplicationFitAnalysisService;
    }

    public MergeDecision upsert(JobApplicationEmail email, UUID userId) {
        try {
            if (email.company() == null || email.company().isBlank()) {
                log.warn("Email has no company, skipping upsert");
                return new MergeDecision("IGNORE", null, null, null, null, "Email has no identifiable company");
            }

            String sourceMessageId = sourceMessageId(email);
            if (emailReviewItemRepository.findByUserIdAndSourceMessageId(userId, sourceMessageId).isPresent()) {
                log.info("Email {} was already handled or queued for review", sourceMessageId);
                return new MergeDecision("IGNORE", null, null, null, null, "This message was already handled or queued for review");
            }

            List<JobApplication> candidates = findCompanyCandidates(userId, email.company(), email.companyNameRaw());

            List<com.shivam.jobcopilot.entity.FitAnalysis> fitAnalyses = new ArrayList<>(
                    fitAnalysisRepository.findByUserIdAndCompanyCanonicalIgnoreCase(userId, email.company()));
            if (isPresent(email.companyNameRaw()) && !sameText(email.companyNameRaw(), email.company())) {
                fitAnalyses.addAll(fitAnalysisRepository.findByUserIdAndCompanyIgnoreCase(userId, email.companyNameRaw()));
            }
            fitAnalyses = fitAnalyses.stream().filter(distinctFitAnalysisById()).toList();

            MergeDecision decision;
            Optional<JobApplication> exactRoleCandidate = findExactRoleCandidate(candidates, email.jobTitle());
            if (exactRoleCandidate.isPresent() && isActiveApplication(exactRoleCandidate.get())) {
                JobApplication app = exactRoleCandidate.get();
                UUID matchingFitAnalysisId = app.getFitAnalysisId();
                if (matchingFitAnalysisId == null) {
                    matchingFitAnalysisId = fitAnalyses.stream()
                            .filter(fit -> sameText(fit.getJobTitle(), email.jobTitle()))
                            .map(FitAnalysis::getId)
                            .findFirst()
                            .orElse(null);
                }
                decision = new MergeDecision(
                        "UPDATE", app.getId(), matchingFitAnalysisId, fieldsFromEmail(email), fieldsToClearFromEmail(email),
                        "Deterministic exact company and role match");
            } else if (candidates.isEmpty()) {
                // No candidates — create directly, no LLM call needed
                decision = new MergeDecision("CREATE", null, null, null, null, "No existing applications at this company");
            } else {
                decision = mergeDecisionService.decide(email, candidates, fitAnalyses);
            }

            java.util.Set<UUID> candidateIds = candidates.stream()
                    .map(JobApplication::getId)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());
            applyDecision(decision, email, userId, candidateIds);
            return decision;

        } catch (Exception e) {
            log.error("Upsert failed for {} at {}: {}", email.jobTitle(), email.company(), e.getMessage(), e);
            if (email != null && isPresent(email.company())) {
                try {
                    deferEmail(email, userId, "Email processing failed and needs review before it can be attached.");
                } catch (Exception deferError) {
                    log.error("Could not save failed email for review: {}", deferError.getMessage(), deferError);
                }
            }
            return new MergeDecision("UNRESOLVED", null, null, null, null, "Email processing failed and was deferred");
        }
    }

    private void applyDecision(MergeDecision decision,
                               JobApplicationEmail email,
                               UUID userId,
                               java.util.Set<UUID> candidateIds) {
        switch (decision.action()) {
            case "UPDATE" -> {
                if (decision.applicationId() == null) {
                    deferEmail(email, userId, "The email was meaningful, but no application was selected.");
                    return;
                }
                if (!candidateIds.contains(decision.applicationId())) {
                    log.warn("UPDATE decision selected application {} outside the candidate set; deferring email",
                            decision.applicationId());
                    deferEmail(email, userId, "The selected application was not one of the user's matching company applications.");
                    return;
                }
                Optional<JobApplication> found = repository.findById(decision.applicationId());
                if (found.isEmpty()) {
                    log.warn("UPDATE decision references unknown id {}; deferring email instead of creating a duplicate", decision.applicationId());
                    deferEmail(email, userId, "The selected application no longer exists.");
                    return;
                }
                JobApplication app = found.get();
                if (!userId.equals(app.getUserId())) {
                    log.warn("UPDATE decision referenced application {} owned by another user; deferring email", decision.applicationId());
                    deferEmail(email, userId, "The selected application does not belong to the current user.");
                    return;
                }
                applyFieldsToSet(app, decision.fieldsToSet());
                applyFieldsToClear(app, decision.fieldsToClear());
                if (decision.fitAnalysisId() != null && app.getFitAnalysisId() == null) {
                    app.setFitAnalysisId(decision.fitAnalysisId());
                }
                JobApplication saved = repository.save(app);
                appendUpdate(app, email.updateSummary());
                studentInsightService.refreshForApplication(saved);
                log.info("Updated job application for {} at {}", app.getJobTitle(), app.getCompany());
            }
            case "CREATE" -> createFromEmail(email, decision.fitAnalysisId(), userId);
            case "IGNORE" -> log.info("Merge decision: IGNORE — {}", decision.reasoning());
            case "UNRESOLVED" -> {
                log.warn("Merge decision unresolved; email deferred: {}", decision.reasoning());
                deferEmail(email, userId, decision.reasoning());
            }
            default -> {
                log.warn("Unknown merge decision action '{}'; deferring email instead of creating a duplicate", decision.action());
                deferEmail(email, userId, "The email could not be safely matched to an application.");
            }
        }
    }

    private List<JobApplication> findCompanyCandidates(UUID userId, String canonicalCompany, String rawCompany) {
        List<JobApplication> candidates = new ArrayList<>(
                repository.findByUserIdAndCompanyCanonicalIgnoreCase(userId, canonicalCompany));
        if (isPresent(rawCompany) && !sameText(rawCompany, canonicalCompany)) {
            candidates.addAll(repository.findByUserIdAndCompanyIgnoreCase(userId, rawCompany));
        }
        return candidates.stream().filter(distinctById()).toList();
    }

    public List<JobApplication> getEmailCandidates(JobApplicationEmail email, UUID userId) {
        if (email == null || !isPresent(email.company())) return List.of();
        return findCompanyCandidates(userId, email.company(), email.companyNameRaw());
    }

    private String sourceMessageId(JobApplicationEmail email) {
        return isPresent(email.gmailMessageId()) ? email.gmailMessageId() : "unidentified-" + UUID.randomUUID();
    }

    private void deferEmail(JobApplicationEmail email, UUID userId, String reason) {
        String sourceMessageId = sourceMessageId(email);
        if (emailReviewItemRepository.findByUserIdAndSourceMessageId(userId, sourceMessageId).isPresent()) return;

        EmailReviewItem item = new EmailReviewItem();
        item.setUserId(userId);
        item.setSourceMessageId(sourceMessageId);
        item.setCompanyNameRaw(email.companyNameRaw());
        item.setCompanyNameCanonical(email.company());
        item.setJobTitle(email.jobTitle());
        item.setRecruiterName(email.recruiterName());
        item.setRecruiterEmail(email.recruiterEmail());
        item.setApplicationStatus(email.applicationStatus());
        item.setReferral(email.referral());
        item.setRoleCategory(email.roleCategory());
        item.setInterviewDateAndTime(email.interviewDateAndTime());
        item.setUpdateSummary(email.updateSummary());
        item.setReviewReason(isPresent(reason) ? reason : "The email needs your confirmation before it can be attached.");
        item.setReviewStatus("PENDING_REVIEW");
        emailReviewItemRepository.save(item);
    }

    public List<EmailReviewResponse> getEmailReviews(UUID userId) {
        return emailReviewItemRepository.findByUserIdAndReviewStatusOrderByCreatedAtDesc(userId, "PENDING_REVIEW")
                .stream()
                .map(item -> toEmailReviewResponse(item, userId))
                .toList();
    }

    @Transactional
    public JobApplication resolveEmailReview(UUID reviewId, UUID applicationId, UUID userId) {
        EmailReviewItem item = emailReviewItemRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Email review item not found"));
        if (!userId.equals(item.getUserId())) {
            throw new SecurityException("Email review item does not belong to the current user");
        }
        if (!"PENDING_REVIEW".equals(item.getReviewStatus())) {
            throw new IllegalArgumentException("Email review item has already been handled");
        }

        List<JobApplication> candidates = findCompanyCandidates(userId, item.getCompanyNameCanonical(), item.getCompanyNameRaw());
        if (candidates.stream().noneMatch(app -> applicationId.equals(app.getId()))) {
            throw new IllegalArgumentException("Choose one of the applications shown for this company");
        }

        JobApplication app = repository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        JobApplicationEmail email = emailFromReview(item);
        applyFieldsToSet(app, fieldsFromEmail(email));
        applyFieldsToClear(app, fieldsToClearFromEmail(email));
        JobApplication saved = repository.save(app);
        appendUpdate(app, email.updateSummary());
        studentInsightService.refreshForApplication(saved);

        item.setReviewStatus("RESOLVED");
        item.setResolvedApplicationId(applicationId);
        item.setResolvedAt(LocalDateTime.now());
        emailReviewItemRepository.save(item);
        return saved;
    }

    public void ignoreEmailReview(UUID reviewId, UUID userId) {
        EmailReviewItem item = emailReviewItemRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Email review item not found"));
        if (!userId.equals(item.getUserId())) {
            throw new SecurityException("Email review item does not belong to the current user");
        }
        if ("PENDING_REVIEW".equals(item.getReviewStatus())) {
            item.setReviewStatus("IGNORED");
            item.setResolvedAt(LocalDateTime.now());
            emailReviewItemRepository.save(item);
        }
    }

    private EmailReviewResponse toEmailReviewResponse(EmailReviewItem item, UUID userId) {
        List<EmailReviewResponse.Candidate> candidates = findCompanyCandidates(
                        userId, item.getCompanyNameCanonical(), item.getCompanyNameRaw()).stream()
                .map(app -> new EmailReviewResponse.Candidate(
                        app.getId(), app.getCompany(), app.getJobTitle(), app.getApplicationStatus(),
                        app.getCreatedAt(), app.getUpdatedAt()))
                .toList();
        return new EmailReviewResponse(
                item.getId(), item.getCompanyNameRaw(), item.getCompanyNameCanonical(), item.getJobTitle(),
                item.getRecruiterName(), item.getRecruiterEmail(), item.getApplicationStatus(), item.getReferral(),
                item.getRoleCategory(), item.getInterviewDateAndTime(), item.getUpdateSummary(), item.getReviewReason(),
                item.getCreatedAt(), candidates);
    }

    private JobApplicationEmail emailFromReview(EmailReviewItem item) {
        return new JobApplicationEmail(
                item.getCompanyNameRaw(), item.getCompanyNameCanonical(), item.getJobTitle(), item.getRecruiterName(),
                item.getRecruiterEmail(), item.getApplicationStatus(), item.getReferral(), item.getRoleCategory(),
                item.getInterviewDateAndTime(), item.getSourceMessageId(), item.getUpdateSummary());
    }

    private Optional<JobApplication> findExactRoleCandidate(List<JobApplication> candidates, String jobTitle) {
        if (!isPresent(jobTitle)) return Optional.empty();
        List<JobApplication> exactMatches = candidates.stream()
                .filter(app -> sameText(app.getJobTitle(), jobTitle))
                .toList();
        return exactMatches.size() == 1 ? Optional.of(exactMatches.get(0)) : Optional.empty();
    }

    private Map<String, String> fieldsFromEmail(JobApplicationEmail email) {
        Map<String, String> fields = new HashMap<>();
        putIfPresent(fields, "recruiterName", email.recruiterName());
        putIfPresent(fields, "recruiterEmail", email.recruiterEmail());
        putIfPresent(fields, "applicationStatus", email.applicationStatus());
        putIfPresent(fields, "referral", email.referral());
        putIfPresent(fields, "roleCategory", email.roleCategory());
        putIfPresent(fields, "interviewDate", email.interviewDateAndTime());
        return fields;
    }

    private void putIfPresent(Map<String, String> fields, String field, String value) {
        if (isPresent(value)) fields.put(field, value);
    }

    private List<String> fieldsToClearFromEmail(JobApplicationEmail email) {
        String status = email.applicationStatus();
        if (isPresent(status)
                && (status.equalsIgnoreCase("Rejected")
                || status.equalsIgnoreCase("Offer")
                || status.equalsIgnoreCase("Closed"))) {
            return List.of("interviewDate");
        }
        return List.of();
    }

    private boolean sameText(String left, String right) {
        return isPresent(left) && isPresent(right) && left.trim().equalsIgnoreCase(right.trim());
    }

    private java.util.function.Predicate<JobApplication> distinctById() {
        java.util.Set<UUID> seen = new java.util.HashSet<>();
        return app -> app.getId() == null || seen.add(app.getId());
    }

    private java.util.function.Predicate<FitAnalysis> distinctFitAnalysisById() {
        java.util.Set<UUID> seen = new java.util.HashSet<>();
        return fit -> fit.getId() == null || seen.add(fit.getId());
    }

    private boolean isActiveApplication(JobApplication app) {
        String status = app.getApplicationStatus();
        return !isPresent(status)
                || (!status.equalsIgnoreCase("Rejected")
                && !status.equalsIgnoreCase("Offer")
                && !status.equalsIgnoreCase("Closed"));
    }

    private void createFromEmail(JobApplicationEmail email, UUID fitAnalysisId, UUID userId) {
        JobApplication app = new JobApplication();
        app.setCompany(email.companyNameRaw() != null ? email.companyNameRaw() : email.company());
        app.setCompanyNameCanonical(email.company());
        app.setJobTitle(email.jobTitle());
        app.setRecruiterName(email.recruiterName());
        app.setRecruiterEmail(email.recruiterEmail());
        app.setApplicationStatus(email.applicationStatus());
        app.setReferral(email.referral());
        app.setRoleCategory(email.roleCategory());
        app.setInterviewDate(parseDateTime(email.interviewDateAndTime()));
        app.setFitAnalysisId(fitAnalysisId);
        app.setUserId(userId);
        if (email.applicationStatus() != null
                && !email.applicationStatus().equalsIgnoreCase("Applied")
                && !email.applicationStatus().equalsIgnoreCase("Referral Received")
                && !email.applicationStatus().equalsIgnoreCase("Rejected")
                && !email.applicationStatus().equalsIgnoreCase("Closed")
                && !email.applicationStatus().equalsIgnoreCase("Offer")) {
            app.setFirstRespondedAt(LocalDateTime.now());
        }
        JobApplication saved = repository.save(app);
        appendUpdate(app, email.updateSummary());
        studentInsightService.refreshForApplication(saved);
        log.info("Created job application for {} at {}", email.jobTitle(), email.company());
    }

    private void applyFieldsToSet(JobApplication app, Map<String, String> fieldsToSet) {
        if (fieldsToSet == null) return;
        fieldsToSet.forEach((field, value) -> {
            if (value == null || value.isBlank()) return;
            switch (field) {
                case "company", "jobTitle" -> log.warn("Ignoring protected identity field from email merge: {}", field);
                case "recruiterName"     -> app.setRecruiterName(value);
                case "recruiterEmail"    -> app.setRecruiterEmail(value);
                case "applicationStatus" -> {
                    app.setApplicationStatus(value);
                    if (app.getFirstRespondedAt() == null
                            && !value.equalsIgnoreCase("Applied")
                            && !value.equalsIgnoreCase("Referral Received")) {
                        app.setFirstRespondedAt(LocalDateTime.now());
                    }
                }
                case "referral"          -> app.setReferral(value);
                // Only overwrite an existing category with "Other" if there is nothing stored yet
                case "roleCategory"      -> { if (!value.equalsIgnoreCase("Other") || !isPresent(app.getRoleCategory())) app.setRoleCategory(value); }
                case "interviewDate"     -> app.setInterviewDate(parseDateTime(value));
                default -> log.warn("Unknown field in fieldsToSet: {}", field);
            }
        });
    }

    private void applyFieldsToClear(JobApplication app, List<String> fieldsToClear) {
        if (fieldsToClear == null) return;
        for (String field : fieldsToClear) {
            switch (field) {
                case "recruiterName"     -> app.setRecruiterName(null);
                case "recruiterEmail"    -> app.setRecruiterEmail(null);
                case "applicationStatus" -> app.setApplicationStatus(null);
                case "referral"          -> app.setReferral(null);
                case "roleCategory"      -> app.setRoleCategory(null);
                case "interviewDate"     -> app.setInterviewDate(null);
                default -> log.warn("Unknown or protected field in fieldsToClear: {}", field);
            }
        }
    }

    private static final DateTimeFormatter[] DATE_TIME_FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            // "2026-02-22 8PM IST", "2026-02-22 8PM"
            new DateTimeFormatterBuilder()
                    .append(DateTimeFormatter.ISO_LOCAL_DATE)
                    .appendLiteral(' ')
                    .appendValue(ChronoField.CLOCK_HOUR_OF_AMPM)
                    .appendPattern("a")
                    .optionalStart().appendLiteral(' ').appendPattern("z").optionalEnd()
                    .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                    .toFormatter(Locale.ENGLISH),
            // "2026-02-22 8:30PM IST", "2026-02-22 8:30 PM"
            new DateTimeFormatterBuilder()
                    .append(DateTimeFormatter.ISO_LOCAL_DATE)
                    .appendLiteral(' ')
                    .appendValue(ChronoField.CLOCK_HOUR_OF_AMPM)
                    .appendLiteral(':')
                    .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                    .optionalStart().appendLiteral(' ').optionalEnd()
                    .appendPattern("a")
                    .optionalStart().appendLiteral(' ').appendPattern("z").optionalEnd()
                    .toFormatter(Locale.ENGLISH),
            // "2026-02-22 20:00"
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            // "2026-02-22 20:00:00"
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
    };

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        for (DateTimeFormatter fmt : DATE_TIME_FORMATS) {
            try {
                return LocalDateTime.parse(trimmed, fmt);
            } catch (DateTimeParseException ignored) {
            }
        }
        // Last resort: date-only → start of day
        try {
            return LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (DateTimeParseException e) {
            log.warn("Could not parse interview date '{}': {}", value, e.getMessage());
            return null;
        }
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private void appendUpdate(JobApplication app, String summary) {
        if (!isPresent(summary)) return;
        app.getUpdates().add(new ApplicationUpdate(LocalDateTime.now(), summary));
        repository.save(app);
    }

    public List<PendingAction> getPendingActions(UUID userId) {
        LocalDateTime now = LocalDateTime.now();
        List<PendingAction> actions = new ArrayList<>();

        // Referral received but not yet applied (stale after 5 minutes in dev, would be ~1 day in prod)
        List<JobApplication> pendingReferrals = repository.findByUserIdAndApplicationStatusAndUpdatedAtBefore(
                userId, "Referral Received", now.minusMinutes(5));
        pendingReferrals.stream()
                .filter(app -> app.getSnoozedUntil() == null || !app.getSnoozedUntil().isAfter(now))
                .map(app -> {
                    long daysAgo = ChronoUnit.DAYS.between(app.getUpdatedAt(), now);
                    String message = String.format(
                            "Referral for %s received %d days ago. Have you applied?",
                            app.getCompany(), daysAgo);
                    return new PendingAction(app.getId(), app.getCompany(), app.getJobTitle(), message, "Applied", "FOLLOW_UP_REFERRAL", 0);
                })
                .forEach(actions::add);

        // Applied but no update in 30+ days — prompt to close
        List<JobApplication> staleApplied = repository.findByUserIdAndApplicationStatusAndUpdatedAtBefore(
                userId, "Applied", now.minusDays(30));
        staleApplied.stream()
                .filter(app -> app.getSnoozedUntil() == null || !app.getSnoozedUntil().isAfter(now))
                .map(app -> {
                    long daysAgo = ChronoUnit.DAYS.between(app.getUpdatedAt(), now);
                    String message = String.format(
                            "No update from %s in %d days. Should we close this application?",
                            app.getCompany(), daysAgo);
                    return new PendingAction(app.getId(), app.getCompany(), app.getJobTitle(), message, "Closed", "STALE_APPLICATION", 0);
                })
                .forEach(actions::add);

        return actions;
    }

    public List<JobApplication> listAll(UUID userId) {
        List<JobApplication> apps = repository.findByUserId(userId);
        if (apps.isEmpty()) return apps;

        Map<UUID, PostApplicationInsight> insightMap = insightService.findAllByApplicationIds(
                apps.stream().map(JobApplication::getId).toList());

        apps.forEach(app -> {
            PostApplicationInsight insight = insightMap.get(app.getId());
            if (insight != null) {
                boolean stale = insightService.isStale(insight, app.getCvId(), app.getJobDescriptionText());
                app.setFitSummary(new JobApplication.FitSummary(insight.getFitScore(), stale));
            }
        });

        return apps;
    }

    public JobApplication getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job application not found: " + id));
    }

    public JobApplication create(JobApplication app, UUID userId) {
        if (!isPresent(app.getCompany()) || !isPresent(app.getJobTitle())) {
            throw new IllegalArgumentException("Company and job title are required");
        }
        if (app.getCvId() == null) {
            throw new IllegalArgumentException("A CV is required for a manual application");
        }
        if (!isPresent(app.getJobDescriptionText())) {
            throw new IllegalArgumentException("A job description is required for a manual application");
        }
        app.setUserId(userId);
        app.setFitAnalysisStatus("PENDING");
        app.setFitAnalysisError(null);
        JobApplication saved = repository.save(app);
        studentInsightService.refreshForApplication(saved);
        manualApplicationFitAnalysisService.queue(saved.getId(), userId);
        return saved;
    }

    public JobApplication retryFitAnalysis(UUID id, UUID userId) {
        JobApplication app = getById(id);
        if (!userId.equals(app.getUserId())) {
            throw new SecurityException("Application does not belong to the current user");
        }
        if (app.getCvId() == null || !isPresent(app.getJobDescriptionText())) {
            throw new IllegalArgumentException("A CV and job description are required before analysis can run");
        }
        app.setFitAnalysisStatus("PENDING");
        app.setFitAnalysisError(null);
        JobApplication saved = repository.save(app);
        manualApplicationFitAnalysisService.queue(saved.getId(), userId);
        return saved;
    }

    /**
     * Creates or links an application from a completed fit analysis. The fit analysis
     * is the source of truth for the CV and JD, so no new AI call is needed here.
     */
    public JobApplication markAsApplied(UUID fitAnalysisId, UUID userId) {
        return markAsApplied(fitAnalysisId, userId, null);
    }

    public JobApplication markAsApplied(UUID fitAnalysisId, UUID userId, String requestedRoleCategory) {
        FitAnalysis fitAnalysis = fitAnalysisRepository.findById(fitAnalysisId)
                .orElseThrow(() -> new IllegalArgumentException("Fit analysis not found: " + fitAnalysisId));

        if (fitAnalysis.getUserId() == null || !fitAnalysis.getUserId().equals(userId)) {
            throw new SecurityException("Fit analysis does not belong to the current user");
        }

        String roleCategory = isPresent(fitAnalysis.getRoleCategory())
                ? fitAnalysis.getRoleCategory()
                : requestedRoleCategory;

        Optional<JobApplication> alreadyLinked = repository.findByUserIdAndFitAnalysisId(userId, fitAnalysisId)
                .stream()
                .findFirst();
        if (alreadyLinked.isPresent()) {
            JobApplication app = alreadyLinked.get();
            if (!isPresent(app.getRoleCategory()) && isPresent(roleCategory)) {
                app.setRoleCategory(roleCategory.trim());
                return repository.save(app);
            }
            return app;
        }

        // Reuse an application already created from an email for the same role.
        Optional<JobApplication> existingForRole = repository
                .findByUserIdAndCompanyCanonicalIgnoreCaseAndJobTitleIgnoreCase(
                        userId, fitAnalysis.getCompanyNameCanonical(), fitAnalysis.getJobTitle());
        if (existingForRole.isPresent()) {
            JobApplication app = existingForRole.get();
            app.setFitAnalysisId(fitAnalysisId);
            if (app.getCvId() == null) app.setCvId(fitAnalysis.getCvId());
            if (!isPresent(app.getJobDescriptionText())) app.setJobDescriptionText(fitAnalysis.getJobDescriptionText());
            if (!isPresent(app.getApplicationStatus())) app.setApplicationStatus("Applied");
            if (!isPresent(app.getRoleCategory()) && isPresent(roleCategory)) app.setRoleCategory(roleCategory.trim());
            JobApplication saved = repository.save(app);
            studentInsightService.refreshForApplication(saved);
            return saved;
        }

        JobApplication app = new JobApplication();
        app.setUserId(userId);
        app.setCompany(fitAnalysis.getCompanyNameRaw());
        app.setCompanyNameCanonical(fitAnalysis.getCompanyNameCanonical());
        app.setJobTitle(fitAnalysis.getJobTitle());
        app.setRoleCategory(isPresent(roleCategory) ? roleCategory.trim() : null);
        app.setApplicationStatus("Applied");
        app.setFitAnalysisId(fitAnalysisId);
        app.setCvId(fitAnalysis.getCvId());
        app.setJobDescriptionText(fitAnalysis.getJobDescriptionText());
        app.getUpdates().add(new ApplicationUpdate(
                LocalDateTime.now(), "Application marked as applied from fit analysis."));

        JobApplication saved = repository.save(app);
        studentInsightService.refreshForApplication(saved);
        return saved;
    }

    public JobApplication update(UUID id, JobApplication updated, UUID userId) {
        JobApplication existing = getById(id);
        if (!userId.equals(existing.getUserId())) {
            throw new SecurityException("Application does not belong to the current user");
        }
        if (isPresent(updated.getCompany())) existing.setCompany(updated.getCompany());
        if (isPresent(updated.getJobTitle())) existing.setJobTitle(updated.getJobTitle());
        if (isPresent(updated.getRecruiterName())) existing.setRecruiterName(updated.getRecruiterName());
        if (isPresent(updated.getRecruiterEmail())) existing.setRecruiterEmail(updated.getRecruiterEmail());
        if (isPresent(updated.getApplicationStatus())) existing.setApplicationStatus(updated.getApplicationStatus());
        if (isPresent(updated.getReferral())) existing.setReferral(updated.getReferral());
        existing.setRoleCategory(nullable(updated.getRoleCategory()));
        existing.setInterviewDate(updated.getInterviewDate());
        if (updated.getCvId() != null) existing.setCvId(updated.getCvId());
        existing.setJobDescriptionText(nullable(updated.getJobDescriptionText()));
        existing.setJobDescriptionUrl(nullable(updated.getJobDescriptionUrl()));
        existing.setNotes(nullable(updated.getNotes()));
        JobApplication saved = repository.save(existing);
        studentInsightService.refreshForApplication(saved);
        return saved;
    }

    private String nullable(String value) {
        return isPresent(value) ? value : null;
    }

    public JobApplication snooze(UUID id, int days) {
        LocalDateTime snoozedUntil = days > 0
                ? LocalDateTime.now().plusDays(days)
                : LocalDateTime.now().plusMinutes(3);  // 3-min default for FOLLOW_UP_REFERRAL in dev
        repository.updateSnoozedUntil(id, snoozedUntil);
        JobApplication updated = getById(id);
        studentInsightService.refreshForApplication(updated);
        return updated;
    }

    public void delete(UUID id) {
        JobApplication existing = getById(id);
        UUID userId = existing.getUserId();
        repository.deleteById(id);
        if (userId != null) {
            studentInsightService.rebuildStoredInsightsAsync(userId);
        }
    }
}
