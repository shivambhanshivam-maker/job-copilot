package com.shivam.jobcopilot.repository;

import com.shivam.jobcopilot.entity.AffiliationStatus;
import com.shivam.jobcopilot.entity.JobSearchStatus;
import com.shivam.jobcopilot.entity.StudentSchoolAffiliation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentSchoolAffiliationRepository extends JpaRepository<StudentSchoolAffiliation, UUID> {
    Optional<StudentSchoolAffiliation> findByUserIdAndSchoolId(UUID userId, UUID schoolId);
    List<StudentSchoolAffiliation> findByUserId(UUID userId);
    @EntityGraph(attributePaths = {"program", "cohort"})
    List<StudentSchoolAffiliation> findBySchoolIdAndStatus(UUID schoolId, AffiliationStatus status);

    @EntityGraph(attributePaths = {"program", "cohort"})
    List<StudentSchoolAffiliation> findBySchoolIdAndStatusAndJobSearchStatus(UUID schoolId, AffiliationStatus status, JobSearchStatus jobSearchStatus);
    List<StudentSchoolAffiliation> findByProgramIdAndStatus(UUID programId, AffiliationStatus status);
    List<StudentSchoolAffiliation> findByCohortIdAndStatus(UUID cohortId, AffiliationStatus status);

    @Query("SELECT a.userId FROM StudentSchoolAffiliation a WHERE a.school.id = :schoolId AND a.status = :status")
    List<UUID> findUserIdsBySchoolIdAndStatus(@Param("schoolId") UUID schoolId, @Param("status") AffiliationStatus status);

    @Query("SELECT a.userId FROM StudentSchoolAffiliation a WHERE a.program.id = :programId AND a.status = :status")
    List<UUID> findUserIdsByProgramIdAndStatus(@Param("programId") UUID programId, @Param("status") AffiliationStatus status);

    @Query("SELECT a.userId FROM StudentSchoolAffiliation a WHERE a.cohort.id = :cohortId AND a.status = :status")
    List<UUID> findUserIdsByCohortIdAndStatus(@Param("cohortId") UUID cohortId, @Param("status") AffiliationStatus status);
}
