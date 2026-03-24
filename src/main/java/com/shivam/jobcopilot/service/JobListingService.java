package com.shivam.jobcopilot.service;

import com.shivam.jobcopilot.dto.JobListingResponse;
import com.shivam.jobcopilot.entity.FitAnalysis;
import com.shivam.jobcopilot.entity.JobListing;
import com.shivam.jobcopilot.repository.FitAnalysisRepository;
import com.shivam.jobcopilot.repository.JobListingRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobListingService {

    private final JobListingRepository repository;
    private final FitAnalysisRepository fitAnalysisRepository;

    public JobListingService(JobListingRepository repository,
                             FitAnalysisRepository fitAnalysisRepository) {
        this.repository = repository;
        this.fitAnalysisRepository = fitAnalysisRepository;
    }

    public List<JobListingResponse> listAll() {
        return enrich(repository.findAll());
    }

    public List<JobListingResponse> listByRoleCategory(String roleCategory) {
        return enrich(repository.findByRoleCategory(roleCategory));
    }

    public List<JobListingResponse> listAnalyzed() {
        return enrich(repository.findByFitAnalysisIdIsNotNull());
    }

    private List<JobListingResponse> enrich(List<JobListing> listings) {
        return listings.stream()
                .map(listing -> {
                    FitAnalysis analysis = listing.getFitAnalysisId() != null
                            ? fitAnalysisRepository.findById(listing.getFitAnalysisId()).orElse(null)
                            : null;
                    return JobListingResponse.from(listing, analysis);
                })
                .toList();
    }
}