package com.shivam.jobcopilot.scheduler;

import com.shivam.jobcopilot.entity.CV;
import com.shivam.jobcopilot.entity.FitAnalysis;
import com.shivam.jobcopilot.entity.JobListing;
import com.shivam.jobcopilot.entity.PreferredLocation;
import com.shivam.jobcopilot.entity.UserPreferences;
import com.shivam.jobcopilot.repository.JobListingRepository;
import com.shivam.jobcopilot.service.AIService;
import com.shivam.jobcopilot.service.CVService;
import com.shivam.jobcopilot.service.FitAnalysisService;
import com.shivam.jobcopilot.service.JSearchService;
import com.shivam.jobcopilot.service.UserPreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JobFetchScheduler {

    private static final Logger log = LoggerFactory.getLogger(JobFetchScheduler.class);

    private final UserPreferencesService userPreferencesService;
    private final JSearchService jSearchService;
    private final JobListingRepository jobListingRepository;
    private final CVService cvService;
    private final AIService aiService;
    private final FitAnalysisService fitAnalysisService;

    public JobFetchScheduler(UserPreferencesService userPreferencesService,
                             JSearchService jSearchService,
                             JobListingRepository jobListingRepository,
                             CVService cvService,
                             AIService aiService,
                             FitAnalysisService fitAnalysisService) {
        this.userPreferencesService = userPreferencesService;
        this.jSearchService = jSearchService;
        this.jobListingRepository = jobListingRepository;
        this.cvService = cvService;
        this.aiService = aiService;
        this.fitAnalysisService = fitAnalysisService;
    }

    //@Scheduled(cron = "0 0 23 * * *") // runs every night at 11pm
    public void fetchJobs() {
        List<UserPreferences> allPrefs = userPreferencesService.getAll();

        if (allPrefs.isEmpty()) {
            log.info("No preferences configured — skipping nightly job fetch");
            return;
        }

        int totalSaved = 0;
        int totalSkipped = 0;
        int[] totalAnalyzed = {0}; // array to allow mutation inside lambda

        for (UserPreferences prefs : allPrefs) {
            if (prefs.getPreferredRoleCategories().isEmpty() || prefs.getPreferredLocations().isEmpty()) {
                log.info("Preferences incomplete (no categories or locations) — skipping");
                continue;
            }

            // Fetch default CV per user
            UUID userId = prefs.getUserId();
            CV defaultCv = getDefaultCvForUser(userId);
            if (defaultCv == null) continue;

            // Build every (category × location) combination and fire one API call each
            for (String roleCategory : prefs.getPreferredRoleCategories()) {
                for (PreferredLocation location : prefs.getPreferredLocations()) {

                    List<JobListing> fetched = jSearchService.search(
                            roleCategory,
                            location.getCityName(),
                            location.getCountryCode(),
                            prefs.getExperienceLevel()
                    );

                    for (JobListing listing : fetched) {
                        if (listing.getExternalId() == null) continue;

                        // Skip duplicates — job may appear across multiple nightly runs
                        if (jobListingRepository.existsByExternalId(listing.getExternalId())) {
                            totalSkipped++;
                            continue;
                        }

                        JobListing saved = jobListingRepository.save(listing);
                        totalSaved++;

                        // Run fit analysis and link result to the listing
                        Optional<FitAnalysis> analysis = analyze(saved, defaultCv);
                        analysis.ifPresent(fa -> {
                            saved.setFitAnalysisId(fa.getId());
                            saved.setEmployerName(fa.getCompany());
                            saved.setJobTitle(fa.getJobTitle());
                            jobListingRepository.save(saved);
                            totalAnalyzed[0]++;
                        });
                    }
                }
            }
        }

        log.info("Nightly job fetch complete — saved: {}, skipped (duplicates): {}, analyzed: {}",
                totalSaved, totalSkipped, totalAnalyzed[0]);
    }

    private Optional<FitAnalysis> analyze(JobListing listing, CV cv) {
        try {
            String jobContent = buildJobContent(listing);
            String rawJson = aiService.analyze(cv.getContentText(), jobContent,
                    listing.getEmployerName(), listing.getJobTitle());
            return fitAnalysisService.persistFromJson(rawJson, jobContent, cv.getId(),
                    listing.getEmployerName(), listing.getJobTitle());
        } catch (Exception e) {
            log.error("Fit analysis failed for listing {} ({})", listing.getJobTitle(), listing.getExternalId(), e);
            return Optional.empty();
        }
    }

    // Assembles qualifications + responsibilities into a text block for the LLM.
    // Falls back to job_description if both highlight lists are empty.
    private String buildJobContent(JobListing listing) {
        StringBuilder sb = new StringBuilder();
        if (listing.getQualifications() != null && !listing.getQualifications().isEmpty()) {
            sb.append("Qualifications:\n");
            listing.getQualifications().forEach(q -> sb.append("- ").append(q).append("\n"));
        }
        if (listing.getResponsibilities() != null && !listing.getResponsibilities().isEmpty()) {
            sb.append("\nResponsibilities:\n");
            listing.getResponsibilities().forEach(r -> sb.append("- ").append(r).append("\n"));
        }
        if (sb.isEmpty() && listing.getJobDescription() != null && !listing.getJobDescription().isBlank()) {
            sb.append(listing.getJobDescription());
        }
        return sb.toString();
    }

    private CV getDefaultCvForUser(UUID userId) {
        try {
            if (userId == null) {
                log.warn("No userId on preferences — skipping fit analysis for this user");
                return null;
            }
            return cvService.getDefault(userId);
        } catch (Exception e) {
            log.warn("No default CV set for user {} — skipping fit analysis for tonight's job fetch. Set a default CV to enable this.", userId);
            return null;
        }
    }
}
