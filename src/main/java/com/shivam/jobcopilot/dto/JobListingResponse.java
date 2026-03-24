package com.shivam.jobcopilot.dto;

import com.shivam.jobcopilot.entity.CvAdjustmentItem;
import com.shivam.jobcopilot.entity.FitAnalysis;
import com.shivam.jobcopilot.entity.GapItem;
import com.shivam.jobcopilot.entity.JobListing;
import com.shivam.jobcopilot.entity.StrengthItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class JobListingResponse {

    private UUID id;
    private String externalId;
    private String employerName;
    private String jobTitle;
    private String jobEmploymentType;
    private String jobLocation;
    private String jobCountry;
    private String jobApplyLink;
    private String jobPostedAt;
    private String roleCategory;
    private String jobDescription;
    private List<String> qualifications;
    private List<String> responsibilities;
    private LocalDateTime fetchedAt;

    // null when the listing has not been analysed yet
    private FitAnalysisSummary fitAnalysis;

    public record FitAnalysisSummary(
            UUID id,
            int fitScore,
            String recommendation,
            String confidence,
            List<StrengthItem> strengths,
            List<GapItem> gaps,
            String positioningAngle,
            List<CvAdjustmentItem> cvAdjustments
    ) {}

    public static JobListingResponse from(JobListing listing, FitAnalysis analysis) {
        JobListingResponse r = new JobListingResponse();
        r.id = listing.getId();
        r.externalId = listing.getExternalId();
        r.employerName = listing.getEmployerName();
        r.jobTitle = listing.getJobTitle();
        r.jobEmploymentType = listing.getJobEmploymentType();
        r.jobLocation = listing.getJobLocation();
        r.jobCountry = listing.getJobCountry();
        r.jobApplyLink = listing.getJobApplyLink();
        r.jobPostedAt = listing.getJobPostedAt();
        r.roleCategory = listing.getRoleCategory();
        r.jobDescription = listing.getJobDescription();
        r.qualifications = listing.getQualifications();
        r.responsibilities = listing.getResponsibilities();
        r.fetchedAt = listing.getFetchedAt();

        if (analysis != null) {
            r.fitAnalysis = new FitAnalysisSummary(
                    analysis.getId(),
                    analysis.getFitScore(),
                    analysis.getRecommendation(),
                    analysis.getConfidence(),
                    analysis.getStrengthAlignment(),
                    analysis.getGaps(),
                    analysis.getPositioningAngle(),
                    analysis.getCvAdjustments()
            );
        }
        return r;
    }

    public UUID getId() { return id; }
    public String getExternalId() { return externalId; }
    public String getEmployerName() { return employerName; }
    public String getJobTitle() { return jobTitle; }
    public String getJobEmploymentType() { return jobEmploymentType; }
    public String getJobLocation() { return jobLocation; }
    public String getJobCountry() { return jobCountry; }
    public String getJobApplyLink() { return jobApplyLink; }
    public String getJobPostedAt() { return jobPostedAt; }
    public String getRoleCategory() { return roleCategory; }
    public String getJobDescription() { return jobDescription; }
    public List<String> getQualifications() { return qualifications; }
    public List<String> getResponsibilities() { return responsibilities; }
    public LocalDateTime getFetchedAt() { return fetchedAt; }
    public FitAnalysisSummary getFitAnalysis() { return fitAnalysis; }
}