package com.shivam.jobcopilot.service;

import com.shivam.jobcopilot.dto.JobApplicationEmail;
import com.shivam.jobcopilot.dto.MergeDecision;
import com.shivam.jobcopilot.dto.PendingAction;
import com.shivam.jobcopilot.entity.ApplicationUpdate;
import com.shivam.jobcopilot.entity.JobApplication;
import com.shivam.jobcopilot.repository.FitAnalysisRepository;
import com.shivam.jobcopilot.repository.JobApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
    private final EmailMergeDecisionService mergeDecisionService;

    public JobApplicationService(JobApplicationRepository repository,
                                 FitAnalysisRepository fitAnalysisRepository,
                                 EmailMergeDecisionService mergeDecisionService) {
        this.repository = repository;
        this.fitAnalysisRepository = fitAnalysisRepository;
        this.mergeDecisionService = mergeDecisionService;
    }

    public void upsert(JobApplicationEmail email, UUID userId) {
        try {
            if (email.company() == null || email.company().isBlank()) {
                log.warn("Email has no company, skipping upsert");
                return;
            }

            List<JobApplication> candidates = repository.findByUserIdAndCompanyIgnoreCase(userId, email.company());
            List<com.shivam.jobcopilot.entity.FitAnalysis> fitAnalyses =
                    fitAnalysisRepository.findByUserIdAndCompanyIgnoreCase(userId, email.company());

            MergeDecision decision;
            if (candidates.isEmpty()) {
                // No candidates — create directly, no LLM call needed
                decision = new MergeDecision("CREATE", null, null, null, null, "No existing applications at this company");
            } else {
                decision = mergeDecisionService.decide(email, candidates, fitAnalyses);
            }

            applyDecision(decision, email, userId);

        } catch (Exception e) {
            log.error("Upsert failed for {} at {}: {}", email.jobTitle(), email.company(), e.getMessage(), e);
        }
    }

    private void applyDecision(MergeDecision decision, JobApplicationEmail email, UUID userId) {
        switch (decision.action()) {
            case "UPDATE" -> {
                if (decision.applicationId() == null) {
                    log.warn("UPDATE decision has no applicationId, falling back to CREATE");
                    createFromEmail(email, decision.fitAnalysisId(), userId);
                    return;
                }
                Optional<JobApplication> found = repository.findById(decision.applicationId());
                if (found.isEmpty()) {
                    log.warn("UPDATE decision references unknown id {}, falling back to CREATE", decision.applicationId());
                    createFromEmail(email, decision.fitAnalysisId(), userId);
                    return;
                }
                JobApplication app = found.get();
                applyFieldsToSet(app, decision.fieldsToSet());
                applyFieldsToClear(app, decision.fieldsToClear());
                if (decision.fitAnalysisId() != null) app.setFitAnalysisId(decision.fitAnalysisId());
                repository.save(app);
                appendUpdate(app, email.updateSummary());
                log.info("Updated job application for {} at {}", app.getJobTitle(), app.getCompany());
            }
            case "CREATE" -> createFromEmail(email, decision.fitAnalysisId(), userId);
            case "IGNORE" -> log.info("Merge decision: IGNORE — {}", decision.reasoning());
            default -> {
                log.warn("Unknown merge decision action '{}', falling back to CREATE", decision.action());
                createFromEmail(email, null, userId);
            }
        }
    }

    private void createFromEmail(JobApplicationEmail email, UUID fitAnalysisId, UUID userId) {
        JobApplication app = new JobApplication();
        app.setCompany(email.company());
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
        repository.save(app);
        appendUpdate(app, email.updateSummary());
        log.info("Created job application for {} at {}", email.jobTitle(), email.company());
    }

    private void applyFieldsToSet(JobApplication app, Map<String, String> fieldsToSet) {
        if (fieldsToSet == null) return;
        fieldsToSet.forEach((field, value) -> {
            if (value == null || value.isBlank()) return;
            switch (field) {
                case "company"           -> app.setCompany(value);
                case "jobTitle"          -> app.setJobTitle(value);
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

    // Auto-link: if a FitAnalysis already exists for this company + title, attach it
    // Only used for manual application creation (email path uses merge decision's fitAnalysisId)
    private void tryLinkFitAnalysis(JobApplication app) {
        if (!isPresent(app.getCompany()) || !isPresent(app.getJobTitle())) return;
        if (app.getFitAnalysisId() != null) return; // already linked
        if (app.getUserId() == null) return;

        fitAnalysisRepository
                .findByUserIdAndCompanyIgnoreCaseAndJobTitleIgnoreCase(app.getUserId(), app.getCompany(), app.getJobTitle())
                .ifPresent(fa -> {
                    app.setFitAnalysisId(fa.getId());
                    repository.save(app);
                });
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
        return repository.findByUserId(userId);
    }

    public JobApplication getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job application not found: " + id));
    }

    public JobApplication create(JobApplication app, UUID userId) {
        app.setUserId(userId);
        JobApplication saved = repository.save(app);
        tryLinkFitAnalysis(saved);
        return saved;
    }

    public JobApplication update(UUID id, JobApplication updated, UUID userId) {
        JobApplication existing = getById(id);
        if (isPresent(updated.getCompany())) existing.setCompany(updated.getCompany());
        if (isPresent(updated.getJobTitle())) existing.setJobTitle(updated.getJobTitle());
        if (isPresent(updated.getRecruiterName())) existing.setRecruiterName(updated.getRecruiterName());
        if (isPresent(updated.getRecruiterEmail())) existing.setRecruiterEmail(updated.getRecruiterEmail());
        if (isPresent(updated.getApplicationStatus())) existing.setApplicationStatus(updated.getApplicationStatus());
        if (isPresent(updated.getReferral())) existing.setReferral(updated.getReferral());
        if (updated.getInterviewDate() != null) existing.setInterviewDate(updated.getInterviewDate());
        if (updated.getCvId() != null) existing.setCvId(updated.getCvId());
        return repository.save(existing);
    }

    public JobApplication snooze(UUID id, int days) {
        LocalDateTime snoozedUntil = days > 0
                ? LocalDateTime.now().plusDays(days)
                : LocalDateTime.now().plusMinutes(3);  // 3-min default for FOLLOW_UP_REFERRAL in dev
        repository.updateSnoozedUntil(id, snoozedUntil);
        return getById(id);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
