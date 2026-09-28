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

    interface PeerEvidenceApplicationRow {
        UUID getUserId();
        String getRoleCategory();
        String getApplicationStatus();
        LocalDateTime getInterviewDate();
        LocalDateTime getCreatedAt();
        LocalDateTime getUpdatedAt();
        UUID getFitAnalysisId();
    }

    /**
     * Scalar application data used by school read models. Keeping this as a
     * projection avoids loading the eager email-update collection for every
     * student on the advisor pages.
     */
    interface SchoolApplicationRow {
        UUID getId();
        UUID getUserId();
        String getCompany();
        String getJobTitle();
        String getRoleCategory();
        String getApplicationStatus();
        LocalDateTime getInterviewDate();
        LocalDateTime getCreatedAt();
        LocalDateTime getUpdatedAt();
        LocalDateTime getFirstRespondedAt();
    }

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

    List<JobApplication> findByUserIdIn(List<UUID> userIds);

    @Query(value = """
            SELECT id,
                   user_id AS userId,
                   COALESCE(NULLIF(company_name_raw, ''), NULLIF(company, ''), company_name_canonical) AS company,
                   job_title AS jobTitle,
                   role_category AS roleCategory,
                   application_status AS applicationStatus,
                   interview_date AS interviewDate,
                   created_at AS createdAt,
                   updated_at AS updatedAt,
                   first_responded_at AS firstRespondedAt
            FROM job_applications
            WHERE user_id IN (:userIds)
            """, nativeQuery = true)
    List<SchoolApplicationRow> findSchoolApplicationsByUserIds(@Param("userIds") List<UUID> userIds);

    @Query(value = """
            SELECT user_id AS userId,
                   role_category AS roleCategory,
                   application_status AS applicationStatus,
                   interview_date AS interviewDate,
                   created_at AS createdAt,
                   updated_at AS updatedAt,
                   fit_analysis_id AS fitAnalysisId
            FROM job_applications
            WHERE user_id IN (:userIds)
            """, nativeQuery = true)
    List<PeerEvidenceApplicationRow> findPeerEvidenceApplicationsByUserIds(@Param("userIds") List<UUID> userIds);

    List<JobApplication> findByUserIdAndCompanyIgnoreCase(UUID userId, String company);

    @Query("SELECT j FROM JobApplication j WHERE j.userId = :userId AND LOWER(COALESCE(j.companyNameCanonical, j.company)) = LOWER(:company)")
    List<JobApplication> findByUserIdAndCompanyCanonicalIgnoreCase(@Param("userId") UUID userId, @Param("company") String company);

    Optional<JobApplication> findByUserIdAndCompanyIgnoreCaseAndJobTitleIgnoreCase(UUID userId, String company, String jobTitle);

    @Query("SELECT j FROM JobApplication j WHERE j.userId = :userId AND LOWER(COALESCE(j.companyNameCanonical, j.company)) = LOWER(:company) AND LOWER(j.jobTitle) = LOWER(:jobTitle)")
    Optional<JobApplication> findByUserIdAndCompanyCanonicalIgnoreCaseAndJobTitleIgnoreCase(
            @Param("userId") UUID userId, @Param("company") String company, @Param("jobTitle") String jobTitle);

    @Query("SELECT j FROM JobApplication j WHERE LOWER(COALESCE(j.companyNameCanonical, j.company)) = LOWER(:company) AND LOWER(j.jobTitle) = LOWER(:jobTitle)")
    Optional<JobApplication> findByCompanyCanonicalIgnoreCaseAndJobTitleIgnoreCase(
            @Param("company") String company, @Param("jobTitle") String jobTitle);

    List<JobApplication> findByUserIdAndFitAnalysisId(UUID userId, UUID fitAnalysisId);

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

    long countByUserIdAndApplicationStatusInAndCreatedAtBetween(UUID userId, List<String> statuses, LocalDateTime start, LocalDateTime end);

    long countByUserIdAndFirstRespondedAtIsNotNullAndCreatedAtBetween(UUID userId, LocalDateTime start, LocalDateTime end);

    // Category-filtered funnel queries (specific category)
    @Query("SELECT COUNT(j) FROM JobApplication j WHERE j.userId = :userId AND j.roleCategory = :category AND j.applicationStatus IN :statuses")
    long countByUserIdAndRoleCategoryAndApplicationStatusIn(@Param("userId") UUID userId, @Param("category") String category, @Param("statuses") List<String> statuses);

    @Query("SELECT COUNT(j) FROM JobApplication j WHERE j.userId = :userId AND j.roleCategory = :category AND j.applicationStatus IN :statuses AND j.createdAt BETWEEN :start AND :end")
    long countByUserIdAndRoleCategoryAndApplicationStatusInAndCreatedAtBetween(@Param("userId") UUID userId, @Param("category") String category, @Param("statuses") List<String> statuses, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Category-filtered funnel queries ("Others" = not in preferred categories)
    @Query("SELECT COUNT(j) FROM JobApplication j WHERE j.userId = :userId AND (j.roleCategory NOT IN :categories OR j.roleCategory IS NULL) AND j.applicationStatus IN :statuses")
    long countByUserIdAndRoleCategoryNotInAndApplicationStatusIn(@Param("userId") UUID userId, @Param("categories") List<String> categories, @Param("statuses") List<String> statuses);

    @Query("SELECT COUNT(j) FROM JobApplication j WHERE j.userId = :userId AND (j.roleCategory NOT IN :categories OR j.roleCategory IS NULL) AND j.applicationStatus IN :statuses AND j.createdAt BETWEEN :start AND :end")
    long countByUserIdAndRoleCategoryNotInAndApplicationStatusInAndCreatedAtBetween(@Param("userId") UUID userId, @Param("categories") List<String> categories, @Param("statuses") List<String> statuses, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
