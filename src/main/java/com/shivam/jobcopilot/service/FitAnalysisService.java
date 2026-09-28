package com.shivam.jobcopilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shivam.jobcopilot.dto.FitAnalysisResponse;
import com.shivam.jobcopilot.entity.CvAdjustmentItem;
import com.shivam.jobcopilot.entity.FitAnalysis;
import com.shivam.jobcopilot.entity.FitRequirement;
import com.shivam.jobcopilot.entity.GapItem;
import com.shivam.jobcopilot.entity.RequirementEvidence;
import com.shivam.jobcopilot.entity.StrengthItem;
import com.shivam.jobcopilot.repository.FitAnalysisRepository;
import com.shivam.jobcopilot.repository.JobApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;
import java.util.Objects;

@Service
public class FitAnalysisService {

    public record ScorePreview(List<FitRequirement> requirements,
                               List<RequirementEvidence> evidence,
                               FitScoringService.ScoringResult score) {}

    private static final Logger log = LoggerFactory.getLogger(FitAnalysisService.class);
    private static final Set<String> REQUIREMENT_IMPORTANCE = Set.of("CORE", "SUPPORTING", "PREFERRED");
    private static final Set<String> REQUIREMENT_RELEVANCE = Set.of("DIRECT", "INFERRED");
    private static final Set<String> EVIDENCE_STATUS = Set.of("STRONG", "GOOD", "WEAK", "PARTIAL", "MISSING");

    private final FitAnalysisRepository fitAnalysisRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final StudentInsightService studentInsightService;
    private final FitScoringService fitScoringService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Changes here invalidate only the repeat-analysis cache, while keeping the scoring formula versioned separately.
    private static final String ANALYSIS_CACHE_VERSION = FitScoringService.SCORING_VERSION + "_GPT56_LUNA";

    /** Constructor retained for the existing unit tests and small local callers. */
    public FitAnalysisService(FitAnalysisRepository fitAnalysisRepository,
                              JobApplicationRepository jobApplicationRepository,
                              StudentInsightService studentInsightService) {
        this(fitAnalysisRepository, jobApplicationRepository, studentInsightService, new FitScoringService());
    }

    @Autowired
    public FitAnalysisService(FitAnalysisRepository fitAnalysisRepository,
                              JobApplicationRepository jobApplicationRepository,
                              StudentInsightService studentInsightService,
                              FitScoringService fitScoringService) {
        this.fitAnalysisRepository = fitAnalysisRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.studentInsightService = studentInsightService;
        this.fitScoringService = fitScoringService;
    }

    // Called by the controller's doOnComplete — parses the fully assembled JSON then persists.
    // company and jobTitle are supplied by the caller directly — not extracted from the LLM response.
    // Returns empty if parsing fails so the scheduler can handle the failure gracefully.
    public Optional<FitAnalysis> persistFromJson(String json, String jobDescriptionText, UUID cvId,
                                                 String company, String jobTitle, UUID userId) {
        return persistFromJson(json, jobDescriptionText, cvId, company, jobTitle, null, userId);
    }

    public Optional<FitAnalysis> persistFromJson(String json, String jobDescriptionText, UUID cvId,
                                                 String company, String jobTitle, String roleCategory, UUID userId) {
        return persistFromJson(json, jobDescriptionText, cvId, company, jobTitle, roleCategory, userId, null);
    }

    public Optional<FitAnalysis> persistFromJson(String json, String jobDescriptionText, UUID cvId,
                                                 String company, String jobTitle, String roleCategory,
                                                 UUID userId, String cvText) {
        try {
            FitAnalysisResponse parsed = objectMapper.readValue(json, FitAnalysisResponse.class);
            return Optional.of(save(company, jobTitle, roleCategory, jobDescriptionText, cvId, parsed, userId, cvText));
        } catch (Exception e) {
            log.error("Failed to persist fit analysis after stream completed", e);
            return Optional.empty();
        }
    }

    // Overload without userId for scheduler (job listings) where there is no user context
    public Optional<FitAnalysis> persistFromJson(String json, String jobDescriptionText, UUID cvId,
                                                 String company, String jobTitle) {
        return persistFromJson(json, jobDescriptionText, cvId, company, jobTitle, null);
    }

    public FitAnalysis getById(UUID id) {
        return fitAnalysisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("FitAnalysis not found: " + id));
    }

    @Transactional
    public FitAnalysis save(String company, String jobTitle, String jobDescriptionText,
                            UUID cvId, FitAnalysisResponse response, UUID userId) {
        return save(company, jobTitle, null, jobDescriptionText, cvId, response, userId);
    }

    @Transactional
    public FitAnalysis save(String company, String jobTitle, String roleCategory, String jobDescriptionText,
                            UUID cvId, FitAnalysisResponse response, UUID userId) {
        return save(company, jobTitle, roleCategory, jobDescriptionText, cvId, response, userId, null);
    }

    @Transactional
    public FitAnalysis save(String company, String jobTitle, String roleCategory, String jobDescriptionText,
                            UUID cvId, FitAnalysisResponse response, UUID userId, String cvText) {
        FitAnalysis fa = new FitAnalysis();
        fa.setCompany(company);
        fa.setCompanyNameRaw(company);
        fa.setCompanyNameCanonical(present(response.getCompanyNameCanonical())
                ? response.getCompanyNameCanonical().trim()
                : company);
        fa.setJobTitle(jobTitle);
        fa.setRoleCategory(roleCategory);
        fa.setJobDescriptionText(jobDescriptionText);
        fa.setCvId(cvId);
        fa.setUserId(userId);
        fa.setRevisionNumber(1);
        fa.setScoringVersion(ANALYSIS_CACHE_VERSION);
        fa.setCvContentHash(hashText(cvText));
        fa.setCvTextSnapshot(cvText);
        fa.setJdContentHash(analysisContextHash(company, jobTitle, roleCategory, jobDescriptionText));
        populate(fa, response, cvText);

        FitAnalysis saved = fitAnalysisRepository.save(fa);

        // Auto-link: if a JobApplication already exists for this company + title, attach the fit analysis
        if (userId != null) {
            jobApplicationRepository
                    .findByUserIdAndCompanyCanonicalIgnoreCaseAndJobTitleIgnoreCase(userId, fa.getCompanyNameCanonical(), jobTitle)
                    .ifPresent(app -> {
                        app.setFitAnalysisId(saved.getId());
                        if (!present(app.getRoleCategory()) && present(saved.getRoleCategory())) {
                            app.setRoleCategory(saved.getRoleCategory());
                        }
                        jobApplicationRepository.save(app);
                    });
        } else {
            jobApplicationRepository
                    .findByCompanyCanonicalIgnoreCaseAndJobTitleIgnoreCase(fa.getCompanyNameCanonical(), jobTitle)
                    .ifPresent(app -> {
                        app.setFitAnalysisId(saved.getId());
                        if (!present(app.getRoleCategory()) && present(saved.getRoleCategory())) {
                            app.setRoleCategory(saved.getRoleCategory());
                        }
                        jobApplicationRepository.save(app);
                    });
        }

        if (userId != null) {
            queueInsightIngestion(saved, userId);
        }
        return saved;
    }

    // Replaces all analysis fields on an existing record in place (same ID, same job application link).
    public Optional<FitAnalysis> replaceFromJson(String json, UUID existingId, UUID cvId, UUID userId) {
        try {
            FitAnalysisResponse parsed = objectMapper.readValue(json, FitAnalysisResponse.class);
            FitAnalysis existing = getById(existingId);
            existing.setCvId(cvId);
            populate(existing, parsed);
            FitAnalysis saved = fitAnalysisRepository.save(existing);
            if (userId != null) {
                queueInsightIngestion(saved, userId);
            }
            return Optional.of(saved);
        } catch (Exception e) {
            log.error("Failed to replace fit analysis after re-analysis stream completed", e);
            return Optional.empty();
        }
    }

    /**
     * Persists a new revision. A revision with an unchanged JD reuses the previous
     * rubric; a JD/context change passes through the normal initial-analysis rubric.
     */
    @Transactional
    public Optional<FitAnalysis> createRevisionFromJson(String json,
                                                        FitAnalysis previous,
                                                        UUID cvId,
                                                        String cvText,
                                                        String company,
                                                        String jobTitle,
                                                        String roleCategory,
                                                        String jobDescriptionText,
                                                        boolean preserveRubric,
                                                        UUID userId) {
        try {
            FitAnalysisResponse parsed = objectMapper.readValue(json, FitAnalysisResponse.class);
            FitAnalysis revision = new FitAnalysis();
            revision.setCompany(company);
            revision.setCompanyNameRaw(company);
            revision.setCompanyNameCanonical(present(parsed.getCompanyNameCanonical())
                    ? parsed.getCompanyNameCanonical().trim()
                    : previous.getCompanyNameCanonical());
            revision.setJobTitle(jobTitle);
            revision.setRoleCategory(roleCategory);
            revision.setJobDescriptionText(jobDescriptionText);
            revision.setCvId(cvId);
            revision.setUserId(userId);
            revision.setPreviousAnalysisId(previous.getId());
            revision.setRevisionNumber((previous.getRevisionNumber() == null ? 1 : previous.getRevisionNumber()) + 1);
            revision.setScoringVersion(ANALYSIS_CACHE_VERSION);
            revision.setCvContentHash(hashText(cvText));
            revision.setCvTextSnapshot(cvText);
            revision.setJdContentHash(analysisContextHash(company, jobTitle, roleCategory, jobDescriptionText));

            if (preserveRubric && previous.getJdRequirements() != null && !previous.getJdRequirements().isEmpty()) {
                applyFixedRubricEvidence(revision, previous.getJdRequirements(), parsed.getJdRequirements(), cvText);
                populateNarrative(revision, parsed);
                applyDeterministicScore(revision);
            } else {
            populate(revision, parsed, cvText);
            }

            FitAnalysis saved = fitAnalysisRepository.save(revision);
            relinkApplications(previous, saved, userId);
            if (userId != null && !jobApplicationRepository.findByUserIdAndFitAnalysisId(userId, saved.getId()).isEmpty()) {
                queueInsightIngestion(saved, userId);
            }
            return Optional.of(saved);
        } catch (Exception e) {
            log.error("Failed to persist fit analysis revision after re-analysis stream completed", e);
            return Optional.empty();
        }
    }

    public boolean hasSameInputs(FitAnalysis analysis,
                                 UUID cvId,
                                 String cvText,
                                 String company,
                                 String jobTitle,
                                 String roleCategory,
                                 String jobDescriptionText) {
        return analysis != null
                && usesCurrentScoring(analysis)
                && java.util.Objects.equals(analysis.getCvId(), cvId)
                && java.util.Objects.equals(analysis.getCvContentHash(), hashText(cvText))
                && java.util.Objects.equals(analysis.getJdContentHash(),
                analysisContextHash(company, jobTitle, roleCategory, jobDescriptionText));
    }

    /**
     * Returns the latest complete result for the exact same CV and role context.
     * Repeating an unchanged fit check must not create a new model run with a
     * different evidence classification.
     */
    public Optional<FitAnalysis> findReusableAnalysis(UUID userId,
                                                       UUID cvId,
                                                       String cvText,
                                                       String company,
                                                       String jobTitle,
                                                       String roleCategory,
                                                       String jobDescriptionText) {
        if (userId == null || cvId == null || cvText == null) return Optional.empty();

        String cvHash = hashText(cvText);
        String jdHash = analysisContextHash(company, jobTitle, roleCategory, jobDescriptionText);
        return fitAnalysisRepository
                .findByUserIdAndCvContentHashAndJdContentHashAndScoringVersionOrderByAnalyzedAtDesc(
                        userId, cvHash, jdHash, ANALYSIS_CACHE_VERSION)
                .stream()
                .filter(analysis -> Objects.equals(analysis.getCvId(), cvId))
                .filter(analysis -> analysis.getJdRequirements() != null && !analysis.getJdRequirements().isEmpty())
                .findFirst();
    }

    public boolean usesCurrentScoring(FitAnalysis analysis) {
        return analysis != null && (FitScoringService.SCORING_VERSION.equals(analysis.getScoringVersion())
                || ANALYSIS_CACHE_VERSION.equals(analysis.getScoringVersion()));
    }

    /** Uses the same normalization and deterministic scoring as persistence, before the narrative is complete. */
    public ScorePreview previewScore(List<FitAnalysisResponse.JdRequirement> jdRequirements, String cvText) {
        FitAnalysis preview = new FitAnalysis();
        applyRequirementEvidence(preview, jdRequirements, cvText);
        return new ScorePreview(preview.getJdRequirements(), preview.getRequirementEvidence(),
                fitScoringService.score(preview.getJdRequirements(), preview.getRequirementEvidence()));
    }

    public String fixedRubricContext(FitAnalysis analysis) {
        StringBuilder context = new StringBuilder("Fixed JD rubric. Keep these criteria exactly as written; do not add, remove, rename, or reweight them:\n");
        if (analysis != null && analysis.getJdRequirements() != null) {
            for (FitRequirement requirement : analysis.getJdRequirements()) {
                context.append("- ").append(requirement.getRequirementKey())
                        .append(" | ").append(requirement.getRequirementText())
                        .append(" | importance=").append(requirement.getImportanceTier())
                        .append(" | weight=").append(requirement.getWeight())
                        .append(" | JD evidence=\"").append(requirement.getSourceExcerpt()).append("\"\n");
            }
        }
        return context.append("\n").toString();
    }

    private void populate(FitAnalysis fa, FitAnalysisResponse response) {
        populate(fa, response, null);
    }

    private void populate(FitAnalysis fa, FitAnalysisResponse response, String cvText) {
        fa.setFitScore(response.getFitScore());
        fa.setRecommendation(response.getRecommendation());
        fa.setConfidence(response.getConfidence());
        fa.setWeightageReasoning(response.getWeightageReasoning());

        FitAnalysisResponse.SubScores ss = response.getSubScores();
        if (ss != null) {
            if (ss.skillsMatch() != null)     { fa.setSkillsMatchScore(ss.skillsMatch().score());         fa.setSkillsMatchWeight(ss.skillsMatch().weight()); }
            if (ss.experienceMatch() != null) { fa.setExperienceMatchScore(ss.experienceMatch().score()); fa.setExperienceMatchWeight(ss.experienceMatch().weight()); }
            if (ss.domainMatch() != null)     { fa.setDomainMatchScore(ss.domainMatch().score());         fa.setDomainMatchWeight(ss.domainMatch().weight()); }
            if (ss.impactMatch() != null)     { fa.setImpactMatchScore(ss.impactMatch().score());         fa.setImpactMatchWeight(ss.impactMatch().weight()); }
            if (ss.cvPresentation() != null)  { fa.setCvPresentationScore(ss.cvPresentation().score());  fa.setCvPresentationWeight(ss.cvPresentation().weight()); }
        }

        if (response.getStrengthAlignment() != null) {
            fa.setStrengthAlignment(response.getStrengthAlignment().stream()
                    .map(s -> new StrengthItem(s.strength(), s.category()))
                    .toList());
        }

        fa.setDifferentiation(response.getDifferentiation());

        if (response.getGaps() != null) {
            fa.setGaps(response.getGaps().stream()
                    .map(g -> new GapItem(g.gap(), g.category(), g.severity()))
                    .toList());
        }

        applyRequirementEvidence(fa, response.getJdRequirements(), cvText);

        if (fa.getJdRequirements() != null && !fa.getJdRequirements().isEmpty()) {
            fa.setScoringVersion(ANALYSIS_CACHE_VERSION);
            applyDeterministicScore(fa);
        }

        fa.setPositioningAngle(response.getPositioningAngle());

        if (response.getCvAdjustments() != null) {
        fa.setCvAdjustments(response.getCvAdjustments().stream()
                    .map(a -> new CvAdjustmentItem(a.adjustment(), a.priority(), a.addressesGap(), a.action(), a.cvPoint(), a.suggestedText()))
                    .toList());
        }
    }

    private void populateNarrative(FitAnalysis fa, FitAnalysisResponse response) {
        fa.setConfidence(response.getConfidence());
        fa.setStrengthAlignment(response.getStrengthAlignment() == null ? new ArrayList<>() : response.getStrengthAlignment().stream()
                .map(s -> new StrengthItem(s.strength(), s.category())).toList());
        fa.setDifferentiation(response.getDifferentiation() == null ? new ArrayList<>() : response.getDifferentiation());
        fa.setGaps(response.getGaps() == null ? new ArrayList<>() : response.getGaps().stream()
                .map(g -> new GapItem(g.gap(), g.category(), g.severity())).toList());
        fa.setPositioningAngle(response.getPositioningAngle());
        fa.setCvAdjustments(response.getCvAdjustments() == null ? new ArrayList<>() : response.getCvAdjustments().stream()
                .map(a -> new CvAdjustmentItem(a.adjustment(), a.priority(), a.addressesGap(), a.action(), a.cvPoint(), a.suggestedText()))
                .toList());
    }

    private void applyDeterministicScore(FitAnalysis fa) {
        FitScoringService.ScoringResult score = fitScoringService.score(fa.getJdRequirements(), fa.getRequirementEvidence());
        fa.setFitScore(score.fitScore());
        fa.setRecommendation(score.recommendation());
        fa.setWeightageReasoning(score.weightageReasoning());
    }

    private void queueInsightIngestion(FitAnalysis saved, UUID userId) {
        if (saved == null || saved.getId() == null || userId == null) return;
        Runnable task = () -> studentInsightService.ingestFitAnalysisAsync(saved.getId(), userId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    /** Replaces only JD requirements and aligned evidence, preserving every other analysis field. */
    @Transactional
    public FitAnalysis replaceRequirementEvidence(UUID fitAnalysisId,
                                                  List<FitAnalysisResponse.JdRequirement> jdRequirements) {
        FitAnalysis existing = getById(fitAnalysisId);
        applyRequirementEvidence(existing, jdRequirements, null);
        return fitAnalysisRepository.save(existing);
    }

    private void applyRequirementEvidence(FitAnalysis fa,
                                          List<FitAnalysisResponse.JdRequirement> jdRequirements,
                                          String cvText) {
        List<FitRequirement> requirements = new ArrayList<>();
        List<RequirementEvidence> evidence = new ArrayList<>();
        Set<String> acceptedRequirementKeys = new HashSet<>();
        if (jdRequirements != null) {
            jdRequirements.stream()
                    .filter(this::validRequirement)
                    .forEach(requirement -> {
                        String requirementKey = requirement.id().trim();
                        if (!acceptedRequirementKeys.add(requirementKey)) return;
                        FitRequirement persistedRequirement = new FitRequirement(
                                requirementKey,
                                requirement.requirement().trim(),
                                requirement.capability().trim(),
                                normalizedImportance(requirement.importance(), requirement.relevance(), requirement.requirement()),
                                requirement.relevance().trim().toUpperCase(),
                                requirement.evidenceType(),
                                requirement.sourceExcerpt().trim()
                        );
                        requirements.add(persistedRequirement);

                        FitAnalysisResponse.RequirementEvidence alignedEvidence = requirement.evidence();
                        evidence.add(normalizeEvidence(requirementKey, alignedEvidence, cvText));
                    });
        }
        assignNormalizedWeights(requirements);
        fa.setJdRequirements(requirements);
        fa.setRequirementEvidence(evidence);
    }

    private void applyFixedRubricEvidence(FitAnalysis fa,
                                          List<FitRequirement> previousRequirements,
                                          List<FitAnalysisResponse.JdRequirement> responseRequirements,
                                          String cvText) {
        Map<String, FitAnalysisResponse.JdRequirement> responseByKey = new HashMap<>();
        if (responseRequirements != null) {
            responseRequirements.stream()
                    .filter(item -> item != null && present(item.id()))
                    .forEach(item -> responseByKey.putIfAbsent(item.id().trim(), item));
        }

        List<FitRequirement> requirements = previousRequirements.stream().map(this::copyRequirement).toList();
        assignNormalizedWeights(requirements);
        List<RequirementEvidence> evidence = new ArrayList<>();
        for (FitRequirement requirement : requirements) {
            FitAnalysisResponse.JdRequirement response = responseByKey.get(requirement.getRequirementKey());
            evidence.add(normalizeEvidence(requirement.getRequirementKey(), response == null ? null : response.evidence(), cvText));
        }
        fa.setJdRequirements(requirements);
        fa.setRequirementEvidence(evidence);
    }

    private RequirementEvidence normalizeEvidence(String requirementKey,
                                                   FitAnalysisResponse.RequirementEvidence source,
                                                   String cvText) {
        String status = source == null || !present(source.status()) ? "MISSING" : source.status().trim().toUpperCase();
        if (!EVIDENCE_STATUS.contains(status)) status = "MISSING";

        String evidenceText = source == null ? null : trimToNull(source.evidenceText());
        String artifact = source == null ? null : trimToNull(source.artifact());
        if ("MISSING".equals(status) || evidenceText == null
                || (cvText != null && !containsNormalized(cvText, evidenceText))) {
            return new RequirementEvidence(requirementKey, "Missing", null, null, null,
                    normalizedConfidence(source == null ? null : source.confidence()));
        }

        return new RequirementEvidence(requirementKey, titleCase(status),
                trimToNull(source.evidenceType()), evidenceText, artifact,
                normalizedConfidence(source.confidence()));
    }

    private String normalizedConfidence(String confidence) {
        if (!present(confidence)) return "Medium";
        String normalized = confidence.trim().toLowerCase();
        return switch (normalized) {
            case "high" -> "High";
            case "low" -> "Low";
            default -> "Medium";
        };
    }

    private String titleCase(String value) {
        return value.substring(0, 1) + value.substring(1).toLowerCase();
    }

    private FitRequirement copyRequirement(FitRequirement source) {
        FitRequirement copy = new FitRequirement(
                source.getRequirementKey(), source.getRequirementText(), source.getCapabilityPhrase(),
                normalizedImportance(source.getImportanceTier(), source.getRelevanceMode(), source.getRequirementText()),
                source.getRelevanceMode(), source.getEvidenceType(), source.getSourceExcerpt());
        copy.setWeight(source.getWeight());
        return copy;
    }

    private void assignNormalizedWeights(List<FitRequirement> requirements) {
        if (requirements == null || requirements.isEmpty()) return;
        int totalUnits = requirements.stream().mapToInt(this::importanceUnits).sum();
        if (totalUnits <= 0) return;

        int[] weights = new int[requirements.size()];
        double[] remainders = new double[requirements.size()];
        int assigned = 0;
        for (int i = 0; i < requirements.size(); i++) {
            double exact = importanceUnits(requirements.get(i)) * 100.0 / totalUnits;
            weights[i] = (int) Math.floor(exact);
            remainders[i] = exact - weights[i];
            assigned += weights[i];
        }

        int remaining = 100 - assigned;
        while (remaining-- > 0) {
            int bestIndex = 0;
            for (int i = 1; i < remainders.length; i++) {
                if (remainders[i] > remainders[bestIndex]) bestIndex = i;
            }
            weights[bestIndex]++;
            remainders[bestIndex] = -1;
        }
        for (int i = 0; i < requirements.size(); i++) {
            requirements.get(i).setWeight(weights[i]);
        }
    }

    private int importanceUnits(FitRequirement requirement) {
        if (requirement == null || requirement.getImportanceTier() == null) return 1;
        // Responsibility-derived criteria are useful, but stay below explicit qualifications.
        if ("INFERRED".equalsIgnoreCase(requirement.getRelevanceMode())) return 1;
        return switch (requirement.getImportanceTier().trim().toUpperCase()) {
            case "CORE" -> 3;
            case "PREFERRED" -> 2;
            case "SUPPORTING" -> 1;
            default -> 1;
        };
    }

    private String normalizedImportance(String importance, String relevance, String requirementText) {
        if (isExplicitQualification(requirementText)) return "CORE";
        if ("INFERRED".equalsIgnoreCase(relevance)) return "SUPPORTING";
        return present(importance) ? importance.trim().toUpperCase() : "SUPPORTING";
    }

    private boolean isExplicitQualification(String requirementText) {
        if (requirementText == null) return false;
        String text = requirementText.toLowerCase().replaceAll("[\\u2012\\u2013\\u2014\\u2212]", "-");
        return text.matches(".*\\b\\d+\\s*(?:-|to|\\+)\\s*\\d*\\s*years?\\b.*")
                || text.matches(".*\\b(minimum|required|must have)\\b.*\\b(years?|experience|degree|certification|qualification).*" )
                || text.matches(".*\\b(?:bachelor|master|mba|phd|certified|certification|qualification)\\b.*");
    }

    private void relinkApplications(FitAnalysis previous, FitAnalysis revision, UUID userId) {
        if (previous == null || previous.getId() == null || userId == null) return;
        jobApplicationRepository.findByUserIdAndFitAnalysisId(userId, previous.getId()).forEach(application -> {
            application.setFitAnalysisId(revision.getId());
            if (!present(application.getRoleCategory()) && present(revision.getRoleCategory())) {
                application.setRoleCategory(revision.getRoleCategory());
            }
            jobApplicationRepository.save(application);
        });
    }

    public String analysisContextHash(String company, String jobTitle, String roleCategory, String jobDescriptionText) {
        return hashText(normalize(company) + "\n" + normalize(jobTitle) + "\n"
                + normalize(roleCategory) + "\n" + normalize(jobDescriptionText));
    }

    private String hashText(String value) {
        if (value == null) return null;
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(normalize(value).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte item : digest) hex.append(String.format("%02x", item));
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.replace("\r\n", "\n").replaceAll("\\s+", " ").trim();
    }

    private String trimToNull(String value) {
        return present(value) ? value.trim() : null;
    }

    private boolean containsNormalized(String document, String phrase) {
        String comparableDocument = normalizeEvidenceText(document);
        String comparablePhrase = normalizeEvidenceText(phrase);
        return comparablePhrase.length() >= 12 && comparableDocument.contains(comparablePhrase);
    }

    private String normalizeEvidenceText(String value) {
        if (value == null) return "";
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFKC)
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private boolean validRequirement(FitAnalysisResponse.JdRequirement requirement) {
        if (requirement == null
                || !present(requirement.id())
                || !present(requirement.requirement())
                || !present(requirement.capability())
                || !present(requirement.importance())
                || !present(requirement.relevance())
                || !present(requirement.sourceExcerpt())) {
            return false;
        }
        return REQUIREMENT_IMPORTANCE.contains(requirement.importance().trim().toUpperCase())
                && REQUIREMENT_RELEVANCE.contains(requirement.relevance().trim().toUpperCase());
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }

    // Overload without userId — for backwards compat with scheduler
    @Transactional
    public FitAnalysis save(String company, String jobTitle, String jobDescriptionText,
                            UUID cvId, FitAnalysisResponse response) {
        return save(company, jobTitle, jobDescriptionText, cvId, response, null);
    }
}
