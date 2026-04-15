package com.shivam.jobcopilot.repository;

import com.shivam.jobcopilot.entity.FitAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FitAnalysisRepository extends JpaRepository<FitAnalysis, UUID> {
    Optional<FitAnalysis> findByCompanyIgnoreCaseAndJobTitleIgnoreCase(String company, String jobTitle);

    List<FitAnalysis> findByCompanyIgnoreCase(String company);

    // Per-user queries
    List<FitAnalysis> findByUserId(UUID userId);

    List<FitAnalysis> findByUserIdAndCompanyIgnoreCaseAndJobTitleIgnoreCaseOrderByAnalyzedAtDesc(UUID userId, String company, String jobTitle);

    List<FitAnalysis> findByUserIdAndCompanyIgnoreCase(UUID userId, String company);
}
