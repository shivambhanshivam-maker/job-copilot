package com.shivam.jobcopilot.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "job_listings")
public class JobListing {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    // JSearch job_id — used for deduplication across nightly runs
    @Column(unique = true, nullable = false)
    private String externalId;

    private String employerName;
    private String jobEmploymentType;
    private String jobTitle;
    private String jobLocation;
    private String jobCountry;

    @Column(length = 512)
    private String jobApplyLink;

    private String jobPostedAt;

    @Column(columnDefinition = "TEXT")
    private String jobDescription;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_listing_qualifications", joinColumns = @JoinColumn(name = "job_listing_id"))
    @Column(name = "qualification", columnDefinition = "TEXT")
    private List<String> qualifications = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_listing_responsibilities", joinColumns = @JoinColumn(name = "job_listing_id"))
    @Column(name = "responsibility", columnDefinition = "TEXT")
    private List<String> responsibilities = new ArrayList<>();

    // Which search category produced this listing — used for CV matching context
    private String roleCategory;

    // Set after nightly fit analysis is run against the default CV
    private UUID fitAnalysisId;

    private LocalDateTime fetchedAt;

    public JobListing() {}

    @PrePersist
    protected void onCreate() {
        this.fetchedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getEmployerName() { return employerName; }
    public void setEmployerName(String employerName) { this.employerName = employerName; }

    public String getJobEmploymentType() { return jobEmploymentType; }
    public void setJobEmploymentType(String jobEmploymentType) { this.jobEmploymentType = jobEmploymentType; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getJobLocation() { return jobLocation; }
    public void setJobLocation(String jobLocation) { this.jobLocation = jobLocation; }

    public String getJobCountry() { return jobCountry; }
    public void setJobCountry(String jobCountry) { this.jobCountry = jobCountry; }

    public String getJobApplyLink() { return jobApplyLink; }
    public void setJobApplyLink(String jobApplyLink) { this.jobApplyLink = jobApplyLink; }

    public String getJobPostedAt() { return jobPostedAt; }
    public void setJobPostedAt(String jobPostedAt) { this.jobPostedAt = jobPostedAt; }

    public String getJobDescription() { return jobDescription; }
    public void setJobDescription(String jobDescription) { this.jobDescription = jobDescription; }

    public List<String> getQualifications() { return qualifications; }
    public void setQualifications(List<String> qualifications) { this.qualifications = qualifications; }

    public List<String> getResponsibilities() { return responsibilities; }
    public void setResponsibilities(List<String> responsibilities) { this.responsibilities = responsibilities; }

    public String getRoleCategory() { return roleCategory; }
    public void setRoleCategory(String roleCategory) { this.roleCategory = roleCategory; }

    public UUID getFitAnalysisId() { return fitAnalysisId; }
    public void setFitAnalysisId(UUID fitAnalysisId) { this.fitAnalysisId = fitAnalysisId; }

    public LocalDateTime getFetchedAt() { return fetchedAt; }
}