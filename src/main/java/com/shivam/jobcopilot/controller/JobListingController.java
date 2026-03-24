package com.shivam.jobcopilot.controller;

import com.shivam.jobcopilot.dto.JobListingResponse;
import com.shivam.jobcopilot.service.JobListingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/job-listings")
public class JobListingController {

    private final JobListingService jobListingService;

    public JobListingController(JobListingService jobListingService) {
        this.jobListingService = jobListingService;
    }

    // GET /job-listings                                   — all listings
    // GET /job-listings?roleCategory=Product+Management   — filter by category
    // GET /job-listings?analyzed=true                     — only listings with a fit analysis
    @GetMapping
    public List<JobListingResponse> list(@RequestParam(required = false) String roleCategory,
                                         @RequestParam(required = false) Boolean analyzed) {
        if (Boolean.TRUE.equals(analyzed)) {
            return jobListingService.listAnalyzed();
        }
        if (roleCategory != null && !roleCategory.isBlank()) {
            return jobListingService.listByRoleCategory(roleCategory);
        }
        return jobListingService.listAll();
    }
}