package com.shivam.jobcopilot.repository;

import com.shivam.jobcopilot.entity.GapMention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface GapMentionRepository extends JpaRepository<GapMention, UUID> {
    List<GapMention> findByUserId(UUID userId);

    long countByNormalizationStatus(String normalizationStatus);

    List<GapMention> findTop200ByNormalizationStatusOrderByCreatedAtAsc(String normalizationStatus);

    List<GapMention> findTop200ByNormalizationStatusInOrderByCreatedAtAsc(List<String> normalizationStatuses);

    List<GapMention> findTop50ByConceptIdOrderByCreatedAtDesc(UUID conceptId);

    List<GapMention> findByUserIdAndFitAnalysisIdIn(UUID userId, List<UUID> fitAnalysisIds);

    @Transactional
    void deleteByUserId(UUID userId);

    @Transactional
    void deleteByFitAnalysisId(UUID fitAnalysisId);
}
