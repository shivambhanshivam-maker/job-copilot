package com.shivam.jobcopilot.repository;

import com.shivam.jobcopilot.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {

    Optional<JobApplication> findByCompanyAndJobTitle(String company, String jobTitle);

    Optional<JobApplication> findByCompanyIgnoreCaseAndJobTitleIgnoreCase(String company, String jobTitle);

    Optional<JobApplication> findFirstByCompanyOrderByUpdatedAtDesc(String company);

    List<JobApplication> findByCompanyIgnoreCase(String company);

    List<JobApplication> findByApplicationStatusAndUpdatedAtBefore(String status, LocalDateTime cutoff);

    @Query("SELECT COUNT(j) FROM JobApplication j WHERE j.referral = :referral AND j.applicationStatus IN :statuses")
    long countByReferralAndStatusIn(@Param("referral") String referral, @Param("statuses") List<String> statuses);

    @Query("SELECT COUNT(j) FROM JobApplication j WHERE j.referral IN :referralValues AND j.applicationStatus IN :statuses")
    long countByReferralInAndStatusIn(@Param("referralValues") List<String> referralValues, @Param("statuses") List<String> statuses);

    long countByApplicationStatusIn(List<String> statuses);

    @Modifying
    @Transactional
    @Query("UPDATE JobApplication j SET j.snoozedUntil = :snoozedUntil WHERE j.id = :id")
    void updateSnoozedUntil(@Param("id") UUID id, @Param("snoozedUntil") LocalDateTime snoozedUntil);

    // Per-user queries
    List<JobApplication> findByUserId(UUID userId);

    List<JobApplication> findByUserIdAndCompanyIgnoreCase(UUID userId, String company);

    Optional<JobApplication> findByUserIdAndCompanyIgnoreCaseAndJobTitleIgnoreCase(UUID userId, String company, String jobTitle);

    List<JobApplication> findByUserIdAndApplicationStatusAndUpdatedAtBefore(UUID userId, String status, LocalDateTime cutoff);

    long countByUserIdAndApplicationStatusIn(UUID userId, List<String> statuses);

    @Query("SELECT COUNT(j) FROM JobApplication j WHERE j.userId = :userId AND j.referral = :referral AND j.applicationStatus IN :statuses")
    long countByUserIdAndReferralAndStatusIn(@Param("userId") UUID userId, @Param("referral") String referral, @Param("statuses") List<String> statuses);

    @Query("SELECT COUNT(j) FROM JobApplication j WHERE j.userId = :userId AND j.referral IN :referralValues AND j.applicationStatus IN :statuses")
    long countByUserIdAndReferralInAndStatusIn(@Param("userId") UUID userId, @Param("referralValues") List<String> referralValues, @Param("statuses") List<String> statuses);

    long countByUserId(UUID userId);

    long countByUserIdAndFirstRespondedAtIsNotNull(UUID userId);

    long countByUserIdAndApplicationStatusAndUpdatedAtBefore(UUID userId, String status, LocalDateTime before);

    long countByUserIdAndCreatedAtBetween(UUID userId, LocalDateTime start, LocalDateTime end);

    long countByUserIdAndApplicationStatusInAndCreatedAtBefore(UUID userId, List<String> statuses, LocalDateTime before);

    List<JobApplication> findByUserIdAndFirstRespondedAtIsNotNull(UUID userId);

    long countByUserIdAndCreatedAtBefore(UUID userId, LocalDateTime before);

    long countByUserIdAndFirstRespondedAtIsNotNullAndCreatedAtBefore(UUID userId, LocalDateTime before);
}
