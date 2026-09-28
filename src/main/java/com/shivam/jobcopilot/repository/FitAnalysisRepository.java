package com.shivam.jobcopilot.repository;

import com.shivam.jobcopilot.entity.FitAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FitAnalysisRepository extends JpaRepository<FitAnalysis, UUID> {
    interface PeerEvidenceRequirementRow {
        UUID getFitAnalysisId();
        UUID getUserId();
        String getRequirementKey();
        String getRequirementText();
        String getCapabilityPhrase();
        String getImportanceTier();
        String getRelevanceMode();
        String getEvidenceType();
        String getSourceExcerpt();
        String getEvidenceStatus();
    }

    @org.springframework.data.jpa.repository.Query(value = """
            SELECT r.fit_analysis_id AS fitAnalysisId,
                   f.user_id AS userId,
                   r.requirement_key AS requirementKey,
                   r.requirement_text AS requirementText,
                   r.capability_phrase AS capabilityPhrase,
                   r.importance_tier AS importanceTier,
                   r.relevance_mode AS relevanceMode,
                   r.evidence_type AS evidenceType,
                   r.source_excerpt AS sourceExcerpt,
                   e.evidence_status AS evidenceStatus
            FROM fit_analysis_requirements r
            JOIN fit_analyses f ON f.id = r.fit_analysis_id
            LEFT JOIN fit_analysis_requirement_evidence e
              ON e.fit_analysis_id = r.fit_analysis_id
             AND e.requirement_key = r.requirement_key
            WHERE r.fit_analysis_id IN (:analysisIds)
            """, nativeQuery = true)
    List<PeerEvidenceRequirementRow> findPeerEvidenceRequirementsByAnalysisIds(
            @org.springframework.data.repository.query.Param("analysisIds") List<UUID> analysisIds);

    Optional<FitAnalysis> findByCompanyIgnoreCaseAndJobTitleIgnoreCase(String company, String jobTitle);

    @org.springframework.data.jpa.repository.Query("SELECT f FROM FitAnalysis f WHERE LOWER(COALESCE(f.companyNameCanonical, f.company)) = LOWER(:company) AND LOWER(f.jobTitle) = LOWER(:jobTitle)")
    Optional<FitAnalysis> findByCompanyCanonicalIgnoreCaseAndJobTitleIgnoreCase(
            @org.springframework.data.repository.query.Param("company") String company,
            @org.springframework.data.repository.query.Param("jobTitle") String jobTitle);

    List<FitAnalysis> findByCompanyIgnoreCase(String company);

    // Per-user queries
    List<FitAnalysis> findByUserId(UUID userId);

    List<FitAnalysis> findTop12ByUserIdOrderByAnalyzedAtDesc(UUID userId);

    List<FitAnalysis> findByUserIdAndCompanyIgnoreCaseAndJobTitleIgnoreCaseOrderByAnalyzedAtDesc(UUID userId, String company, String jobTitle);

    List<FitAnalysis> findByUserIdAndCompanyIgnoreCase(UUID userId, String company);

    @org.springframework.data.jpa.repository.Query("SELECT f FROM FitAnalysis f WHERE f.userId = :userId AND LOWER(COALESCE(f.companyNameCanonical, f.company)) = LOWER(:company)")
    List<FitAnalysis> findByUserIdAndCompanyCanonicalIgnoreCase(
            @org.springframework.data.repository.query.Param("userId") UUID userId, @org.springframework.data.repository.query.Param("company") String company);

    List<FitAnalysis> findByUserIdAndCvContentHashAndJdContentHashAndScoringVersionOrderByAnalyzedAtDesc(
            UUID userId, String cvContentHash, String jdContentHash, String scoringVersion);

    @org.springframework.data.jpa.repository.Query("SELECT f FROM FitAnalysis f WHERE f.userId = :userId AND LOWER(COALESCE(f.companyNameCanonical, f.company)) = LOWER(:company) AND LOWER(f.jobTitle) = LOWER(:jobTitle) ORDER BY f.analyzedAt DESC")
    List<FitAnalysis> findByUserIdAndCompanyCanonicalIgnoreCaseAndJobTitleIgnoreCaseOrderByAnalyzedAtDesc(
            @org.springframework.data.repository.query.Param("userId") UUID userId,
            @org.springframework.data.repository.query.Param("company") String company,
            @org.springframework.data.repository.query.Param("jobTitle") String jobTitle);
}
