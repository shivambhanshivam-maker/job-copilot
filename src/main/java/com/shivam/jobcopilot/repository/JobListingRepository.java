package com.shivam.jobcopilot.repository;

import com.shivam.jobcopilot.entity.JobListing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobListingRepository extends JpaRepository<JobListing, UUID> {
    boolean existsByExternalId(String externalId);
    List<JobListing> findByRoleCategory(String roleCategory);
    List<JobListing> findByFitAnalysisIdIsNotNull();
}