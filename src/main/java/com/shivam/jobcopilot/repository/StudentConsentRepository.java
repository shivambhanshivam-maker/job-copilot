package com.shivam.jobcopilot.repository;

import com.shivam.jobcopilot.entity.StudentConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentConsentRepository extends JpaRepository<StudentConsent, UUID> {
    Optional<StudentConsent> findByAffiliationId(UUID affiliationId);
    Optional<StudentConsent> findByUserIdAndAffiliationSchoolId(UUID userId, UUID schoolId);
    List<StudentConsent> findByAffiliationSchoolId(UUID schoolId);
    List<StudentConsent> findByAffiliationSchoolIdAndAdvisorVisibilityEnabledTrue(UUID schoolId);
}
