package com.shivam.jobcopilot.repository;

import com.shivam.jobcopilot.entity.CV;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CVRepository extends JpaRepository<CV, UUID> {
    Optional<CV> findByIsDefaultCvTrue();

    // Per-user queries
    List<CV> findByUserId(UUID userId);

    Optional<CV> findByUserIdAndIsDefaultCvTrue(UUID userId);
}
