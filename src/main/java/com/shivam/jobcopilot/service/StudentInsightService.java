package com.shivam.jobcopilot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shivam.jobcopilot.entity.FitAnalysis;
import com.shivam.jobcopilot.entity.GapItem;
import com.shivam.jobcopilot.entity.GapMention;
import com.shivam.jobcopilot.entity.JobApplication;
import com.shivam.jobcopilot.entity.StudentInsight;
import com.shivam.jobcopilot.repository.FitAnalysisRepository;
import com.shivam.jobcopilot.repository.GapMentionRepository;
import com.shivam.jobcopilot.repository.JobApplicationRepository;
import com.shivam.jobcopilot.repository.StudentInsightRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StudentInsightService {

    private static final Logger log = LoggerFactory.getLogger(StudentInsightService.class);

    private static final List<String> INTERVIEW_STATUSES = List.of("Interview");
    private static final List<String> OFFER_STATUSES = List.of("Offer");
    private static final int RECENT_APPLICATION_DAYS = 45;
    private static final int UPCOMING_INTERVIEW_DAYS = 7;
    private static final int MIN_CATEGORY_FIT_ANALYSES = 3;
    private static final int MIN_CATEGORY_FIT_GAP = 8;
    private static final int MIN_CATEGORY_RESPONSE_APPLICATIONS = 5;
    private static final double MIN_CATEGORY_RESPONSE_GAP = 0.20;

    private final FitAnalysisRepository fitAnalysisRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final GapMentionRepository gapMentionRepository;
    private final StudentInsightRepository studentInsightRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StudentInsightService(FitAnalysisRepository fitAnalysisRepository,
                                 JobApplicationRepository jobApplicationRepository,
                                 GapMentionRepository gapMentionRepository,
                                 StudentInsightRepository studentInsightRepository) {
        this.fitAnalysisRepository = fitAnalysisRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.gapMentionRepository = gapMentionRepository;
        this.studentInsightRepository = studentInsightRepository;
    }

    @Transactional
    public List<StudentInsight> refreshAndList(UUID userId) {
        regenerate(userId);
        return latestActiveByType(userId);
    }

    public List<StudentInsight> listActive(UUID userId) {
        return latestActiveByType(userId);
    }

    public Optional<PrimaryFocus> selectPrimaryFocus(List<StudentInsight> insights) {
        if (insights == null || insights.isEmpty()) return Optional.empty();

        Optional<StudentInsight> upcoming = insightOfType(insights, "UPCOMING_INTERVIEW");
        if (upcoming.isPresent()) {
            StudentInsight insight = upcoming.get();
            return Optional.of(new PrimaryFocus(
                    "UPCOMING_INTERVIEW",
                    insight.getTitle(),
                    insight.getRecommendation(),
                    "This is your most time-sensitive next step."
            ));
        }

        Optional<StudentInsight> recurring = insights.stream()
                .filter(insight -> "RECURRING_CAPABILITY_GAP".equalsIgnoreCase(safe(insight.getInsightType())))
                .max(Comparator.comparingInt(insight -> evidenceInt(evidence(insight), "applicationCount")));
        if (recurring.isPresent()) {
            StudentInsight insight = recurring.get();
            Map<String, Object> evidence = evidence(insight);
            String capability = evidenceString(evidence, "capability", "this capability");
            int applicationCount = evidenceInt(evidence, "applicationCount");
            String reason = applicationCount > 0
                    ? capability + " appears across " + applicationCount + " recent applied roles."
                    : capability + " appears repeatedly across your recent applied roles.";
            return Optional.of(new PrimaryFocus(
                    "RECURRING_CAPABILITY_GAP",
                    "Strengthen " + capability.toLowerCase(Locale.ROOT) + " evidence",
                    insight.getRecommendation(),
                    reason
            ));
        }

        Optional<StudentInsight> fit = insightOfType(insights, "ROLE_CATEGORY_FIT_COMPARISON");
        Optional<StudentInsight> traction = insightOfType(insights, "ROLE_CATEGORY_TRACTION");
        return categoryFocus(fit.orElse(null), traction.orElse(null));
    }

    private Optional<PrimaryFocus> categoryFocus(StudentInsight fitInsight, StudentInsight tractionInsight) {
        if (fitInsight == null && tractionInsight == null) return Optional.empty();

        Map<String, Object> fitEvidence = fitInsight == null ? Map.of() : evidence(fitInsight);
        Map<String, Object> tractionEvidence = tractionInsight == null ? Map.of() : evidence(tractionInsight);
        String strongestFit = evidenceString(fitEvidence, "bestCategory", "");
        String weakestFit = evidenceString(fitEvidence, "comparedCategory", "");
        String strongestTraction = evidenceString(tractionEvidence, "bestCategory", "");
        String weakestTraction = evidenceString(tractionEvidence, "worstCategory", "");

        if (fitInsight != null && tractionInsight != null) {
            boolean aligned = sameCategory(strongestFit, strongestTraction)
                    && sameCategory(weakestFit, weakestTraction);
            if (!aligned) return Optional.empty();

            int strongerApplications = evidenceInt(tractionEvidence, "bestApplications");
            int weakerApplications = evidenceInt(tractionEvidence, "worstApplications");
            if (weakerApplications >= strongerApplications) {
                return Optional.of(improveCategoryFocus(strongestFit, weakestFit, true));
            }
            return Optional.of(prioritizeCategoryFocus(strongestFit, true));
        }

        if (fitInsight != null && present(strongestFit) && present(weakestFit)) {
            return Optional.of(improveCategoryFocus(strongestFit, weakestFit, false));
        }

        if (tractionInsight != null && present(strongestTraction)) {
            return Optional.of(prioritizeCategoryFocus(strongestTraction, false));
        }

        return Optional.empty();
    }

    private PrimaryFocus improveCategoryFocus(String strongestCategory, String weakerCategory, boolean alignedTraction) {
        String reason = alignedTraction
                ? "Your recent fit and recruiter response are both stronger for " + strongestCategory + " than " + weakerCategory + "."
                : "Your recent applied-role fit analyses consistently favor " + strongestCategory + " over " + weakerCategory + ".";
        return new PrimaryFocus(
                "ROLE_CATEGORY_IMPROVEMENT",
                "Improve your fit for " + weakerCategory + " roles",
                "Your CV currently aligns more strongly with " + strongestCategory + " roles. Before your next "
                        + weakerCategory + " application, strengthen the CV evidence behind the requirements where your fit is weaker.",
                reason
        );
    }

    private PrimaryFocus prioritizeCategoryFocus(String category, boolean alignedFit) {
        String reason = alignedFit
                ? "This pattern is consistent across both CV fit and recruiter response."
                : "This is based on recent recruiter responses; keep tracking it as more fit evidence becomes available.";
        return new PrimaryFocus(
                "ROLE_CATEGORY_PRIORITY",
                "Prioritize " + category + " roles",
                "Your recent applications are showing stronger traction in " + category
                        + ". Use that signal when deciding where to focus your next applications.",
                reason
        );
    }

    private Optional<StudentInsight> insightOfType(List<StudentInsight> insights, String type) {
        return insights.stream()
                .filter(insight -> type.equalsIgnoreCase(safe(insight.getInsightType())))
                .findFirst();
    }

    private Map<String, Object> evidence(StudentInsight insight) {
        if (insight == null || !present(insight.getEvidenceJson())) return Map.of();
        try {
            return objectMapper.readValue(insight.getEvidenceJson(), new TypeReference<>() {});
        } catch (JsonProcessingException ignored) {
            return Map.of();
        }
    }

    private String evidenceString(Map<String, Object> evidence, String key, String fallback) {
        Object value = evidence.get(key);
        return value == null || value.toString().isBlank() ? fallback : value.toString();
    }

    private int evidenceInt(Map<String, Object> evidence, String key) {
        Object value = evidence.get(key);
        if (value instanceof Number number) return number.intValue();
        try {
            return value == null ? 0 : Integer.parseInt(value.toString());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private boolean sameCategory(String first, String second) {
        return present(first) && present(second) && first.trim().equalsIgnoreCase(second.trim());
    }

    private List<StudentInsight> latestActiveByType(UUID userId) {
        List<StudentInsight> stored = studentInsightRepository.findByUserIdAndStatusOrderByGeneratedAtDesc(userId, "ACTIVE");
        List<StudentInsight> recurringGaps = stored.stream()
                .filter(insight -> "RECURRING_CAPABILITY_GAP".equalsIgnoreCase(safe(insight.getInsightType())))
                .sorted(Comparator.comparingInt((StudentInsight insight) ->
                        evidenceInt(evidence(insight), "applicationCount")).reversed())
                .toList();
        List<StudentInsight> latestOtherInsights = stored.stream()
                .filter(insight -> !"RECURRING_CAPABILITY_GAP".equalsIgnoreCase(safe(insight.getInsightType())))
                .collect(Collectors.toMap(
                        StudentInsight::getInsightType,
                        insight -> insight,
                        (existing, duplicate) -> existing,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();
        List<StudentInsight> result = new ArrayList<>(recurringGaps);
        result.addAll(latestOtherInsights);
        return result;
    }

    @Transactional
    public void regenerate(UUID userId) {
        if (userId == null) return;

        log.info("Regenerating student insights for {}", userId);
        List<JobApplication> applications = jobApplicationRepository.findByUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        List<JobApplication> recentApplications = recentApplications(applications, now);
        List<FitAnalysis> analyses = appliedAnalyses(userId, recentApplications);
        syncMissingGapMentions(userId, analyses, recentApplications);
        rebuildStoredInsights(userId, analyses, applications, recentApplications, now);
    }

    @Transactional
    public void rebuildStoredInsights(UUID userId) {
        if (userId == null) return;
        List<JobApplication> applications = jobApplicationRepository.findByUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        List<JobApplication> recentApplications = recentApplications(applications, now);
        List<FitAnalysis> analyses = appliedAnalyses(userId, recentApplications);
        syncMissingGapMentions(userId, analyses, recentApplications);
        rebuildStoredInsights(userId, analyses, applications, recentApplications, now);
    }

    @Async("applicationAnalysisExecutor")
    @Transactional
    public void rebuildStoredInsightsAsync(UUID userId) {
        rebuildStoredInsights(userId);
    }

    private void rebuildStoredInsights(UUID userId,
                                       List<FitAnalysis> analyses,
                                       List<JobApplication> applications,
                                       List<JobApplication> recentApplications,
                                       LocalDateTime now) {
        List<GapMention> mentions = recentGapMentions(userId, analyses);

        studentInsightRepository.deleteByUserId(userId);
        log.info("Cleared previous student insights for {}", userId);

        List<StudentInsight> insights = new ArrayList<>();
        upcomingInterview(userId, applications, now).ifPresent(insights::add);
        insights.addAll(recurringCapabilityGaps(userId, mentions));
        roleCategoryFitComparison(userId, analyses, recentApplications).ifPresent(insights::add);
        roleCategoryTraction(userId, recentApplications).ifPresent(insights::add);
        staleApplicationFollowup(userId, applications).ifPresent(insights::add);

        studentInsightRepository.saveAll(insights);
        log.info("Saved {} student insights for {}", insights.size(), userId);
    }

    @Transactional
    public void ingestFitAnalysis(FitAnalysis analysis) {
        if (analysis == null || analysis.getUserId() == null) return;
        if (jobApplicationRepository.findByUserIdAndFitAnalysisId(analysis.getUserId(), analysis.getId()).isEmpty()) {
            log.info("Skipping gap evidence sync for exploratory fit analysis {}", analysis.getId());
            return;
        }
        syncFitAnalysisGapMentions(analysis);
        rebuildStoredInsights(analysis.getUserId());
    }

    /** Runs after the fit-analysis response has closed so insight work cannot hold the stream open. */
    @Async("applicationAnalysisExecutor")
    @Transactional
    public void ingestFitAnalysisAsync(UUID analysisId, UUID userId) {
        if (analysisId == null || userId == null) return;
        fitAnalysisRepository.findById(analysisId).ifPresent(analysis -> {
            if (!userId.equals(analysis.getUserId())) return;
            ingestFitAnalysis(analysis);
        });
    }

    @Async("applicationAnalysisExecutor")
    @Transactional
    public void refreshForApplication(JobApplication application) {
        if (application == null || application.getUserId() == null) return;
        rebuildStoredInsights(application.getUserId());
    }

    private List<FitAnalysis> appliedAnalyses(UUID userId, List<JobApplication> applications) {
        List<UUID> fitAnalysisIds = applications.stream()
                .map(JobApplication::getFitAnalysisId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (fitAnalysisIds.isEmpty()) {
            log.info("Loaded 0 applied fit analyses for {}", userId);
            return List.of();
        }

        List<FitAnalysis> analyses = fitAnalysisRepository.findAllById(fitAnalysisIds).stream()
                .filter(analysis -> analysis.getUserId() == null || userId.equals(analysis.getUserId()))
                .sorted(Comparator.comparing(FitAnalysis::getAnalyzedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        log.info("Loaded {} applied fit analyses for {}", analyses.size(), userId);
        return analyses;
    }

    private List<GapMention> recentGapMentions(UUID userId, List<FitAnalysis> analyses) {
        List<UUID> fitAnalysisIds = analyses.stream()
                .map(FitAnalysis::getId)
                .toList();
        if (fitAnalysisIds.isEmpty()) return List.of();
        List<GapMention> mentions = gapMentionRepository.findByUserIdAndFitAnalysisIdIn(userId, fitAnalysisIds);
        log.info("Loaded {} gap mentions for {}", mentions.size(), userId);
        return mentions;
    }

    private void syncMissingGapMentions(UUID userId, List<FitAnalysis> analyses, List<JobApplication> applications) {
        Set<UUID> alreadyStored = recentGapMentions(userId, analyses).stream()
                .map(GapMention::getFitAnalysisId)
                .collect(Collectors.toSet());
        List<FitAnalysis> missing = analyses.stream()
                .filter(analysis -> !alreadyStored.contains(analysis.getId()))
                .toList();
        if (missing.isEmpty()) return;

        List<GapMention> saved = ingestGapMentions(userId, missing, applications);
        log.info("Saved {} missing gap mentions for {}", saved.size(), userId);
    }

    private void syncFitAnalysisGapMentions(FitAnalysis analysis) {
        gapMentionRepository.deleteByFitAnalysisId(analysis.getId());
        List<JobApplication> applications = jobApplicationRepository.findByUserIdAndFitAnalysisId(analysis.getUserId(), analysis.getId());
        List<GapMention> saved = ingestGapMentions(analysis.getUserId(), List.of(analysis), applications);
        log.info("Saved {} gap mentions for fit analysis {}", saved.size(), analysis.getId());
    }

    private List<GapMention> ingestGapMentions(UUID userId, List<FitAnalysis> analyses, List<JobApplication> applications) {
        List<GapMention> mentions = new ArrayList<>();
        Map<UUID, JobApplication> applicationByFitAnalysis = applications.stream()
                .filter(application -> application.getFitAnalysisId() != null)
                .collect(Collectors.toMap(
                        JobApplication::getFitAnalysisId,
                        application -> application,
                        (first, duplicate) -> first,
                        LinkedHashMap::new
                ));
        for (FitAnalysis analysis : analyses) {
            if (analysis.getGaps() == null) continue;
            JobApplication application = applicationByFitAnalysis.get(analysis.getId());
            for (GapItem gap : analysis.getGaps()) {
                if (gap.getGap() == null || gap.getGap().isBlank()) continue;
                GapMention mention = new GapMention();
                mention.setUserId(userId);
                mention.setFitAnalysisId(analysis.getId());
                mention.setApplicationId(application != null ? application.getId() : null);
                mention.setCompany(application != null ? application.getCompany() : analysis.getCompany());
                mention.setJobTitle(application != null ? application.getJobTitle() : analysis.getJobTitle());
                mention.setRoleCategory(application != null ? application.getRoleCategory() : null);
                mention.setRawGapText(gap.getGap());
                mention.setRawCategory(gap.getCategory());
                mention.setRawSeverity(gap.getSeverity());
                mention.setNormalizationStatus("PENDING");
                mention.setMappingConfidence(0.0);
                mentions.add(mention);
            }
        }
        return gapMentionRepository.saveAll(mentions);
    }

    private List<JobApplication> recentApplications(List<JobApplication> applications, LocalDateTime now) {
        LocalDateTime cutoff = now.minusDays(RECENT_APPLICATION_DAYS);
        return applications.stream()
                .filter(application -> application.getCreatedAt() != null)
                .filter(application -> !application.getCreatedAt().isBefore(cutoff))
                .toList();
    }

    private Optional<StudentInsight> upcomingInterview(UUID userId,
                                                        List<JobApplication> applications,
                                                        LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        LocalDate lastEligibleDay = today.plusDays(UPCOMING_INTERVIEW_DAYS);

        return applications.stream()
                .filter(application -> application.getInterviewDate() != null)
                .filter(application -> {
                    LocalDate interviewDay = application.getInterviewDate().toLocalDate();
                    return !interviewDay.isBefore(today) && !interviewDay.isAfter(lastEligibleDay);
                })
                .filter(application -> !isTerminal(application))
                .min(Comparator.comparing(JobApplication::getInterviewDate))
                .map(application -> {
                    String role = present(application.getJobTitle()) ? application.getJobTitle() : "role";
                    String company = present(application.getCompany()) ? application.getCompany() : "the company";
                    String date = application.getInterviewDate().format(DateTimeFormatter.ofPattern("MMM d"));
                    return insight(userId,
                            "UPCOMING_INTERVIEW",
                            "HIGH",
                            "HIGH",
                            "Prepare for your upcoming " + role + " interview",
                            "Your interview with " + company + " is on " + date + ".",
                            "Focus your effort on preparation for this role before working on longer-term application patterns.",
                            Map.of(
                                    "applicationId", application.getId() == null ? "" : application.getId().toString(),
                                    "company", company,
                                    "jobTitle", role,
                                    "interviewDate", application.getInterviewDate().toString()
                            ));
                });
    }

    private List<StudentInsight> recurringCapabilityGaps(UUID userId, List<GapMention> mentions) {
        Map<UUID, List<GapMention>> byConcept = mentions.stream()
                .filter(m -> "MAPPED".equalsIgnoreCase(safe(m.getNormalizationStatus())))
                .filter(m -> m.getConceptId() != null && present(m.getConceptName()))
                .filter(m -> m.getMappingConfidence() != null && m.getMappingConfidence() >= 0.75)
                .collect(Collectors.groupingBy(GapMention::getConceptId));

        return byConcept.entrySet().stream()
                .filter(entry -> distinctAnalyses(entry.getValue()) >= 3)
                .filter(entry -> distinctRoleOrCompany(entry.getValue()) >= 2)
                .sorted(Map.Entry.<UUID, List<GapMention>>comparingByValue(
                        Comparator.comparingInt(List::size)).reversed())
                .limit(3)
                .map(entry -> {
                    List<GapMention> conceptMentions = entry.getValue();
                    long highCount = conceptMentions.stream()
                            .filter(m -> "HIGH".equalsIgnoreCase(m.getRawSeverity()))
                            .count();
                    String severity = highCount >= 2 ? "HIGH" : "MEDIUM";
                    String exampleRoles = conceptMentions.stream()
                            .map(GapMention::getJobTitle)
                            .filter(Objects::nonNull)
                            .distinct()
                            .limit(3)
                            .collect(Collectors.joining(", "));

                    return insight(userId,
                            "RECURRING_CAPABILITY_GAP",
                            severity,
                            "MEDIUM",
                            "Your recent applications keep asking for " + conceptName(conceptMentions),
                            "This capability has appeared as a gap across " + distinctAnalyses(conceptMentions)
                                    + " applications with saved fit analysis" + (exampleRoles.isBlank() ? "." : ", including " + exampleRoles + "."),
                            "Before applying to more similar roles, add one or two concrete CV bullets that show this capability in action.",
                            Map.of(
                                    "capability", conceptName(conceptMentions),
                                    "applicationCount", distinctAnalyses(conceptMentions),
                                    "roleCategory", primaryRoleCategory(conceptMentions),
                                    "examples", recurringExamples(conceptMentions)
                            ));
                })
                .toList();
    }

    private List<Map<String, Object>> recurringExamples(List<GapMention> mentions) {
        return mentions.stream()
                .filter(mention -> present(mention.getRawGapText()))
                .collect(Collectors.toMap(
                        mention -> safe(mention.getCompany()) + "|"
                                + safe(mention.getJobTitle()) + "|"
                                + mention.getRawGapText(),
                        mention -> mention,
                        (first, duplicate) -> first,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .limit(5)
                .map(mention -> {
                    Map<String, Object> example = new LinkedHashMap<>();
                    if (present(mention.getCompany())) example.put("company", mention.getCompany());
                    if (present(mention.getJobTitle())) example.put("jobTitle", mention.getJobTitle());
                    if (present(mention.getRoleCategory())) example.put("roleCategory", mention.getRoleCategory());
                    example.put("rawGapText", mention.getRawGapText());
                    if (mention.getApplicationId() != null) example.put("applicationId", mention.getApplicationId().toString());
                    if (mention.getFitAnalysisId() != null) example.put("fitAnalysisId", mention.getFitAnalysisId().toString());
                    return example;
                })
                .toList();
    }

    private Optional<StudentInsight> roleCategoryFitComparison(UUID userId,
                                                                List<FitAnalysis> analyses,
                                                                List<JobApplication> applications) {
        Map<UUID, FitAnalysis> analysisById = analyses.stream()
                .collect(Collectors.toMap(FitAnalysis::getId, analysis -> analysis, (first, duplicate) -> first));
        Map<String, List<Integer>> scoresByCategory = applications.stream()
                .filter(application -> present(application.getRoleCategory()))
                .filter(application -> application.getFitAnalysisId() != null)
                .filter(application -> analysisById.containsKey(application.getFitAnalysisId()))
                .collect(Collectors.groupingBy(
                        application -> application.getRoleCategory().trim(),
                        Collectors.mapping(application -> analysisById.get(application.getFitAnalysisId()).getFitScore(), Collectors.toList())
                ));

        List<CategoryFitStats> stats = scoresByCategory.entrySet().stream()
                .map(entry -> new CategoryFitStats(entry.getKey(), entry.getValue().size(), median(entry.getValue())))
                .filter(stat -> stat.applications() >= MIN_CATEGORY_FIT_ANALYSES)
                .toList();
        if (stats.size() < 2) return Optional.empty();

        CategoryFitStats strongest = stats.stream()
                .max(Comparator.comparingInt(CategoryFitStats::medianFitScore))
                .orElse(null);
        CategoryFitStats weakest = stats.stream()
                .min(Comparator.comparingInt(CategoryFitStats::medianFitScore))
                .orElse(null);
        if (strongest == null || weakest == null) return Optional.empty();

        int gap = strongest.medianFitScore() - weakest.medianFitScore();
        if (gap < MIN_CATEGORY_FIT_GAP) return Optional.empty();

        return Optional.of(insight(userId,
                "ROLE_CATEGORY_FIT_COMPARISON",
                gap >= 15 ? "HIGH" : "MEDIUM",
                strongest.applications() >= 5 && weakest.applications() >= 5 ? "MEDIUM" : "LOW",
                strongest.category() + " is currently your strongest-fit role category",
                "Your median fit score is " + strongest.medianFitScore() + " across " + strongest.applications()
                        + " applied roles, compared with " + weakest.medianFitScore() + " across " + weakest.applications()
                        + " " + weakest.category() + " applied roles.",
                "Use the stronger-fit category as a reference point while strengthening your CV evidence for " + weakest.category() + ".",
                Map.of(
                        "bestCategory", strongest.category(),
                        "bestApplications", strongest.applications(),
                        "bestMedianFitScore", strongest.medianFitScore(),
                        "comparedCategory", weakest.category(),
                        "comparedApplications", weakest.applications(),
                        "comparedMedianFitScore", weakest.medianFitScore(),
                        "fitScoreGap", gap
                )));
    }

    private String primaryRoleCategory(List<GapMention> mentions) {
        return mentions.stream()
                .map(GapMention::getRoleCategory)
                .filter(this::present)
                .collect(Collectors.groupingBy(category -> category, Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");
    }

    private Optional<StudentInsight> roleCategoryTraction(UUID userId, List<JobApplication> applications) {
        Map<String, List<JobApplication>> byCategory = applications.stream()
                .filter(a -> present(a.getRoleCategory()))
                .collect(Collectors.groupingBy(JobApplication::getRoleCategory));

        List<CategoryStats> stats = byCategory.entrySet().stream()
                .map(entry -> new CategoryStats(entry.getKey(), entry.getValue().size(),
                        entry.getValue().stream().filter(this::hasResponse).count()))
                .filter(stat -> stat.applications() >= MIN_CATEGORY_RESPONSE_APPLICATIONS)
                .toList();
        if (stats.size() < 2) return Optional.empty();

        CategoryStats best = stats.stream().max(Comparator.comparingDouble(CategoryStats::responseRate)).orElse(null);
        CategoryStats worst = stats.stream().min(Comparator.comparingDouble(CategoryStats::responseRate)).orElse(null);
        if (best == null || worst == null) return Optional.empty();
        double delta = best.responseRate() - worst.responseRate();
        if (delta < MIN_CATEGORY_RESPONSE_GAP) return Optional.empty();

        return Optional.of(insight(userId,
                "ROLE_CATEGORY_TRACTION",
                "MEDIUM",
                best.applications() >= 5 && worst.applications() >= 5 ? "MEDIUM" : "LOW",
                best.category() + " is showing stronger traction than " + worst.category(),
                best.category() + " has a " + percent(best.responseRate()) + " response rate across "
                        + best.applications() + " applications, compared with " + percent(worst.responseRate())
                        + " for " + worst.category() + ".",
                "Use this as an early signal. Consider prioritizing the role type that is already producing recruiter traction while you improve weaker segments.",
                Map.of(
                        "bestCategory", best.category(),
                        "bestApplications", best.applications(),
                        "bestResponseRate", best.responseRate(),
                        "worstCategory", worst.category(),
                        "worstApplications", worst.applications(),
                        "worstResponseRate", worst.responseRate()
                )));
    }

    private Optional<StudentInsight> staleApplicationFollowup(UUID userId, List<JobApplication> applications) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        List<JobApplication> stale = applications.stream()
                .filter(a -> "Applied".equalsIgnoreCase(a.getApplicationStatus()))
                .filter(a -> a.getUpdatedAt() != null && a.getUpdatedAt().isBefore(cutoff))
                .filter(a -> a.getSnoozedUntil() == null || a.getSnoozedUntil().isBefore(LocalDateTime.now()))
                .toList();
        if (stale.size() < 2) return Optional.empty();

        long oldest = stale.stream()
                .map(JobApplication::getUpdatedAt)
                .filter(Objects::nonNull)
                .mapToLong(updatedAt -> ChronoUnit.DAYS.between(updatedAt, LocalDateTime.now()))
                .max()
                .orElse(30);

        return Optional.of(insight(userId,
                "STALE_APPLICATION_FOLLOWUP",
                "LOW",
                "HIGH",
                "Several applications are stale",
                stale.size() + " applications have been in Applied status for more than 30 days. The oldest has had no update for " + oldest + " days.",
                "Decide whether to follow up, snooze, or close these applications so your dashboard reflects where attention is still useful.",
                Map.of("staleApplications", stale.size(), "oldestDays", oldest)));
    }

    private StudentInsight insight(UUID userId, String type, String severity, String confidence, String title,
                                   String summary, String recommendation, Map<String, Object> evidence) {
        StudentInsight insight = new StudentInsight();
        insight.setUserId(userId);
        insight.setInsightType(type);
        insight.setSeverity(severity);
        insight.setConfidence(confidence);
        insight.setTitle(title);
        insight.setSummary(summary);
        insight.setRecommendation(recommendation);
        insight.setEvidenceJson(toJson(evidence));
        return insight;
    }

    private String toJson(Map<String, Object> evidence) {
        try {
            return objectMapper.writeValueAsString(evidence);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private int distinctAnalyses(List<GapMention> mentions) {
        return (int) mentions.stream().map(GapMention::getFitAnalysisId).distinct().count();
    }

    private int distinctRoleOrCompany(List<GapMention> mentions) {
        Set<String> keys = new HashSet<>();
        for (GapMention mention : mentions) {
            keys.add(safe(mention.getCompany()).toLowerCase(Locale.ROOT) + "|" + safe(mention.getJobTitle()).toLowerCase(Locale.ROOT));
        }
        return keys.size();
    }

    private int median(List<Integer> scores) {
        return scores.stream().sorted().toList().get(scores.size() / 2);
    }

    private boolean hasResponse(JobApplication app) {
        return app.getFirstRespondedAt() != null || isInterview(app) || isOffer(app);
    }

    private boolean isInterview(JobApplication app) {
        String status = safe(app.getApplicationStatus());
        return INTERVIEW_STATUSES.stream().anyMatch(status::equalsIgnoreCase) || app.getInterviewDate() != null;
    }

    private boolean isOffer(JobApplication app) {
        String status = safe(app.getApplicationStatus());
        return OFFER_STATUSES.stream().anyMatch(status::equalsIgnoreCase);
    }

    private boolean isTerminal(JobApplication app) {
        String status = safe(app.getApplicationStatus());
        return isOffer(app)
                || "Rejected".equalsIgnoreCase(status)
                || "Withdrawn".equalsIgnoreCase(status)
                || "Closed".equalsIgnoreCase(status);
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String percent(double value) {
        return Math.round(value * 100) + "%";
    }

    private String conceptName(List<GapMention> mentions) {
        return mentions.stream()
                .map(GapMention::getConceptName)
                .filter(this::present)
                .findFirst()
                .orElse("Mapped capability");
    }

    private record CategoryStats(String category, int applications, long responses) {
        double responseRate() {
            return applications == 0 ? 0 : (double) responses / applications;
        }
    }

    private record CategoryFitStats(String category, int applications, int medianFitScore) {}

    public record PrimaryFocus(String type, String title, String action, String reason) {}

}
