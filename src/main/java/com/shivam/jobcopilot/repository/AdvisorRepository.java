package com.shivam.jobcopilot.repository;

import com.shivam.jobcopilot.entity.Advisor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdvisorRepository extends JpaRepository<Advisor, UUID> {
    Optional<Advisor> findByUserIdAndIsActiveTrue(UUID userId);
    Optional<Advisor> findByUserIdAndSchoolIdAndIsActiveTrue(UUID userId, UUID schoolId);
    List<Advisor> findBySchoolIdAndIsActiveTrueOrderByNameAsc(UUID schoolId);
    boolean existsByUserIdAndIsActiveTrue(UUID userId);
    boolean existsByUserIdAndSchoolIdAndIsActiveTrue(UUID userId, UUID schoolId);
}
