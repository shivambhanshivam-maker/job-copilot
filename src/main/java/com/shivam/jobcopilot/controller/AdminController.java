package com.shivam.jobcopilot.controller;

import com.shivam.jobcopilot.repository.FitAnalysisRepository;
import com.shivam.jobcopilot.repository.JobListingRepository;
import com.shivam.jobcopilot.scheduler.JobFetchScheduler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final JobFetchScheduler jobFetchScheduler;
    private final JobListingRepository jobListingRepository;
    private final FitAnalysisRepository fitAnalysisRepository;

    public AdminController(JobFetchScheduler jobFetchScheduler,
                           JobListingRepository jobListingRepository,
                           FitAnalysisRepository fitAnalysisRepository) {
        this.jobFetchScheduler = jobFetchScheduler;
        this.jobListingRepository = jobListingRepository;
        this.fitAnalysisRepository = fitAnalysisRepository;
    }

    // GET /admin/trigger-job-fetch
    @GetMapping("/trigger-job-fetch")
    public String triggerJobFetch() {
        jobFetchScheduler.fetchJobs();
        return "Job fetch triggered. Check logs and GET /job-listings for results.";
    }

    // GET /admin/clear-job-listings
    @GetMapping("/clear-job-listings")
    public String clearJobListings() {
        long count = jobListingRepository.count();
        fitAnalysisRepository.deleteAll();
        jobListingRepository.deleteAll();
        return "Deleted " + count + " job listings and their fit analyses.";
    }
}