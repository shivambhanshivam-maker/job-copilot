package com.shivam.jobcopilot.repository;

import com.shivam.jobcopilot.entity.AdvisorSupportAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdvisorSupportActionRepository extends JpaRepository<AdvisorSupportAction, UUID> {
    List<AdvisorSupportAction> findByAdvisorUserIdAndStudentUserIdIn(UUID advisorUserId, List<UUID> studentUserIds);

    Optional<AdvisorSupportAction> findByAdvisorUserIdAndStudentUserIdAndSignalTypeAndSignalFingerprint(
            UUID advisorUserId,
            UUID studentUserId,
            String signalType,
            String signalFingerprint
    );
}
