package com.shivam.jobcopilot.service;

import com.shivam.jobcopilot.entity.FitAnalysis;
import com.shivam.jobcopilot.entity.JobApplication;
import com.shivam.jobcopilot.repository.FitAnalysisRepository;
import com.shivam.jobcopilot.repository.JobApplicationRepository;
import com.shivam.jobcopilot.repository.JobListingRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ChatbotTools {

    // ThreadLocal so each request's userId is isolated
    private static final ThreadLocal<UUID> currentUserId = new ThreadLocal<>();

    private final JobApplicationRepository jobApplicationRepository;
    private final FitAnalysisRepository fitAnalysisRepository;
    private final JobListingRepository jobListingRepository;
    private final JobApplicationService jobApplicationService;

    public ChatbotTools(JobApplicationRepository jobApplicationRepository,
                        FitAnalysisRepository fitAnalysisRepository,
                        JobListingRepository jobListingRepository,
                        JobApplicationService jobApplicationService) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.fitAnalysisRepository = fitAnalysisRepository;
        this.jobListingRepository = jobListingRepository;
        this.jobApplicationService = jobApplicationService;
    }

    public static void setUserId(UUID userId) {
        currentUserId.set(userId);
    }

    public static void clearUserId() {
        currentUserId.remove();
    }

    private UUID getUserId() {
        return currentUserId.get();
    }

    @Tool(description = "List all job applications with company, job title, status, and applied date")
    public List<Map<String, Object>> listApplications() {
        UUID userId = getUserId();
        List<JobApplication> apps = userId != null
                ? jobApplicationRepository.findByUserId(userId)
                : jobApplicationRepository.findAll();
        return apps.stream()
                .map(app -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("company", nvl(app.getCompany()));
                    m.put("jobTitle", nvl(app.getJobTitle()));
                    m.put("status", nvl(app.getApplicationStatus()));
                    m.put("appliedAt", app.getCreatedAt() != null ? app.getCreatedAt().toLocalDate().toString() : "unknown");
                    m.put("hasFitAnalysis", app.getFitAnalysisId() != null);
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Tool(description = "Get job applications filtered by status. Valid values: Applied, Interview, Offer, Rejected, Closed, Referral Received")
    public List<Map<String, Object>> getApplicationsByStatus(String status) {
        UUID userId = getUserId();
        List<JobApplication> apps = userId != null
                ? jobApplicationRepository.findByUserId(userId)
                : jobApplicationRepository.findAll();
        return apps.stream()
                .filter(app -> status.equalsIgnoreCase(app.getApplicationStatus()))
                .map(app -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("company", nvl(app.getCompany()));
                    m.put("jobTitle", nvl(app.getJobTitle()));
                    m.put("status", nvl(app.getApplicationStatus()));
                    m.put("appliedAt", app.getCreatedAt() != null ? app.getCreatedAt().toLocalDate().toString() : "unknown");
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Tool(description = "Get all applications for a specific company (case-insensitive, partial match)")
    public List<Map<String, Object>> getApplicationsByCompany(String company) {
        UUID userId = getUserId();
        List<JobApplication> apps = userId != null
                ? jobApplicationRepository.findByUserId(userId)
                : jobApplicationRepository.findAll();
        return apps.stream()
                .filter(app -> app.getCompany() != null &&
                        app.getCompany().toLowerCase().contains(company.toLowerCase()))
                .map(app -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("company", nvl(app.getCompany()));
                    m.put("jobTitle", nvl(app.getJobTitle()));
                    m.put("status", nvl(app.getApplicationStatus()));
                    m.put("appliedAt", app.getCreatedAt() != null ? app.getCreatedAt().toLocalDate().toString() : "unknown");
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Tool(description = "Get application statistics: total count and breakdown by status, plus how many have been fit-analyzed")
    public Map<String, Object> getApplicationStats() {
        UUID userId = getUserId();
        List<JobApplication> all = userId != null
                ? jobApplicationRepository.findByUserId(userId)
                : jobApplicationRepository.findAll();
        Map<String, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(
                        app -> app.getApplicationStatus() != null ? app.getApplicationStatus() : "Unknown",
                        Collectors.counting()
                ));
        long analyzed = all.stream().filter(app -> app.getFitAnalysisId() != null).count();
        Map<String, Object> result = new HashMap<>();
        result.put("total", (long) all.size());
        result.put("byStatus", byStatus);
        result.put("analyzedCount", analyzed);
        return result;
    }

    @Tool(description = "Get the CV fit analysis for a specific company and job title, including fit score (0-100), strengths, gaps, and CV adjustment recommendations")
    public Map<String, Object> getFitAnalysis(String company, String jobTitle) {
        UUID userId = getUserId();
        Optional<FitAnalysis> fa = userId != null
                ? fitAnalysisRepository.findByUserIdAndCompanyCanonicalIgnoreCaseAndJobTitleIgnoreCaseOrderByAnalyzedAtDesc(userId, company, jobTitle).stream().findFirst()
                : fitAnalysisRepository.findByCompanyCanonicalIgnoreCaseAndJobTitleIgnoreCase(company, jobTitle);
        if (fa.isEmpty()) {
            Map<String, Object> notFound = new HashMap<>();
            notFound.put("found", false);
            notFound.put("message", "No fit analysis found for " + company + " - " + jobTitle);
            return notFound;
        }
        FitAnalysis a = fa.get();
        Map<String, Object> result = new HashMap<>();
        result.put("found", true);
        result.put("company", nvl(a.getCompany()));
        result.put("jobTitle", nvl(a.getJobTitle()));
        result.put("fitScore", a.getFitScore());
        result.put("strengths", a.getStrengthAlignment());
        result.put("gaps", a.getGaps());
        result.put("positioningAngle", nvl(a.getPositioningAngle()));
        result.put("cvAdjustments", a.getCvAdjustments());
        return result;
    }

    @Tool(description = "Get pending actions that need attention: referrals not yet applied, stale applications with no updates in 30+ days")
    public List<Map<String, Object>> getPendingActions() {
        UUID userId = getUserId();
        if (userId == null) return List.of();
        return jobApplicationService.getPendingActions(userId).stream()
                .map(action -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("company", nvl(action.company()));
                    m.put("jobTitle", nvl(action.jobTitle()));
                    m.put("message", nvl(action.message()));
                    m.put("type", nvl(action.actionType()));
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Tool(description = "Get job listings discovered by the nightly scheduler. Pass a role category to filter (e.g. 'Product Manager'), or empty string for all listings")
    public List<Map<String, Object>> getDiscoveredJobs(String roleCategory) {
        return jobListingRepository.findAll().stream()
                .filter(job -> roleCategory == null || roleCategory.isBlank() ||
                        (job.getRoleCategory() != null &&
                                job.getRoleCategory().toLowerCase().contains(roleCategory.toLowerCase())))
                .map(job -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("company", nvl(job.getEmployerName()));
                    m.put("jobTitle", nvl(job.getJobTitle()));
                    m.put("location", nvl(job.getJobLocation()));
                    m.put("country", nvl(job.getJobCountry()));
                    m.put("employmentType", nvl(job.getJobEmploymentType()));
                    m.put("applyLink", nvl(job.getJobApplyLink()));
                    m.put("hasFitAnalysis", job.getFitAnalysisId() != null);
                    return m;
                })
                .collect(Collectors.toList());
    }

    private String nvl(String value) {
        return value != null ? value : "";
    }
}
