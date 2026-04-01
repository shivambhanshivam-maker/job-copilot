package com.shivam.jobcopilot.service;

import com.shivam.jobcopilot.dto.ApplicationVelocityResponse;
import com.shivam.jobcopilot.dto.ChannelEffectivenessResponse;
import com.shivam.jobcopilot.dto.FunnelConversionResponse;
import com.shivam.jobcopilot.dto.PerformanceMetricsResponse;
import com.shivam.jobcopilot.dto.StatusPipelineResponse;
import com.shivam.jobcopilot.entity.JobApplication;
import com.shivam.jobcopilot.entity.UserPreferences;
import com.shivam.jobcopilot.repository.JobApplicationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AnalyticsService {

    private final JobApplicationRepository repository;
    private final UserPreferencesService preferencesService;

    public AnalyticsService(JobApplicationRepository repository, UserPreferencesService preferencesService) {
        this.repository = repository;
        this.preferencesService = preferencesService;
    }

    public List<String> getFunnelFilters(UUID userId) {
        UserPreferences prefs = preferencesService.get(userId);
        Set<String> preferred = prefs.getPreferredRoleCategories();
        List<String> filters = new ArrayList<>();
        filters.add("All");
        if (preferred != null) {
            preferred.stream().sorted().forEach(filters::add);
            if (!preferred.isEmpty()) filters.add("Others");
        }
        return filters;
    }

    public ChannelEffectivenessResponse getChannelEffectiveness(UUID userId) {
        List<String> appliedStatuses = List.of("Applied", "Interview", "Offer", "Closed", "Rejected");
        List<String> interviewStatuses = List.of("Interview", "Offer");
        List<String> directReferralValues = List.of("No", "N/A");

        long refApps = repository.countByUserIdAndReferralAndStatusIn(userId, "Yes", appliedStatuses);
        long refInterviews = repository.countByUserIdAndReferralAndStatusIn(userId, "Yes", interviewStatuses);
        long dirApps = repository.countByUserIdAndReferralInAndStatusIn(userId, directReferralValues, appliedStatuses);
        long dirInterviews = repository.countByUserIdAndReferralInAndStatusIn(userId, directReferralValues, interviewStatuses);

        double refYield = refApps > 0 ? (double) refInterviews / refApps : 0;
        double dirYield = dirApps > 0 ? (double) dirInterviews / dirApps : 0;
        double multiplier = dirYield > 0 ? refYield / dirYield : 0;

        return new ChannelEffectivenessResponse(
                refApps, refInterviews, refYield,
                dirApps, dirInterviews, dirYield,
                multiplier, 10.0
        );
    }

    public FunnelConversionResponse getFunnelConversion(UUID userId, String category) {
        List<String> resolvedStatuses = List.of("Interview", "Offer", "Rejected", "Closed");
        List<String> interviewStatuses = List.of("Interview", "Offer");
        List<String> offerStatuses = List.of("Offer");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);

        long resolved, interviews, offers, last30Resolved, last30Interviews, last30Offers;

        if (category == null || category.isBlank() || category.equalsIgnoreCase("All")) {
            resolved = repository.countByUserIdAndApplicationStatusIn(userId, resolvedStatuses);
            interviews = repository.countByUserIdAndApplicationStatusIn(userId, interviewStatuses);
            offers = repository.countByUserIdAndApplicationStatusIn(userId, offerStatuses);
            last30Resolved = repository.countByUserIdAndApplicationStatusInAndCreatedAtBetween(userId, resolvedStatuses, thirtyDaysAgo, now);
            last30Interviews = repository.countByUserIdAndApplicationStatusInAndCreatedAtBetween(userId, interviewStatuses, thirtyDaysAgo, now);
            last30Offers = repository.countByUserIdAndApplicationStatusInAndCreatedAtBetween(userId, offerStatuses, thirtyDaysAgo, now);
        } else if (category.equalsIgnoreCase("Others")) {
            UserPreferences prefs = preferencesService.get(userId);
            Set<String> preferred = prefs.getPreferredRoleCategories();
            List<String> preferredList = (preferred == null || preferred.isEmpty()) ? List.of("__none__") : preferred.stream().toList();
            resolved = repository.countByUserIdAndRoleCategoryNotInAndApplicationStatusIn(userId, preferredList, resolvedStatuses);
            interviews = repository.countByUserIdAndRoleCategoryNotInAndApplicationStatusIn(userId, preferredList, interviewStatuses);
            offers = repository.countByUserIdAndRoleCategoryNotInAndApplicationStatusIn(userId, preferredList, offerStatuses);
            last30Resolved = repository.countByUserIdAndRoleCategoryNotInAndApplicationStatusInAndCreatedAtBetween(userId, preferredList, resolvedStatuses, thirtyDaysAgo, now);
            last30Interviews = repository.countByUserIdAndRoleCategoryNotInAndApplicationStatusInAndCreatedAtBetween(userId, preferredList, interviewStatuses, thirtyDaysAgo, now);
            last30Offers = repository.countByUserIdAndRoleCategoryNotInAndApplicationStatusInAndCreatedAtBetween(userId, preferredList, offerStatuses, thirtyDaysAgo, now);
        } else {
            resolved = repository.countByUserIdAndRoleCategoryAndApplicationStatusIn(userId, category, resolvedStatuses);
            interviews = repository.countByUserIdAndRoleCategoryAndApplicationStatusIn(userId, category, interviewStatuses);
            offers = repository.countByUserIdAndRoleCategoryAndApplicationStatusIn(userId, category, offerStatuses);
            last30Resolved = repository.countByUserIdAndRoleCategoryAndApplicationStatusInAndCreatedAtBetween(userId, category, resolvedStatuses, thirtyDaysAgo, now);
            last30Interviews = repository.countByUserIdAndRoleCategoryAndApplicationStatusInAndCreatedAtBetween(userId, category, interviewStatuses, thirtyDaysAgo, now);
            last30Offers = repository.countByUserIdAndRoleCategoryAndApplicationStatusInAndCreatedAtBetween(userId, category, offerStatuses, thirtyDaysAgo, now);
        }

        double allTimeA2I = resolved > 0 ? (double) interviews / resolved : 0;
        double allTimeI2O = interviews > 0 ? (double) offers / interviews : 0;
        double last30A2I = last30Resolved > 0 ? (double) last30Interviews / last30Resolved : 0;
        double last30I2O = last30Interviews > 0 ? (double) last30Offers / last30Interviews : 0;

        return new FunnelConversionResponse(
                resolved, interviews, allTimeA2I, last30A2I,
                offers, allTimeI2O, last30I2O
        );
    }

    public ApplicationVelocityResponse getApplicationVelocity(UUID userId) {
        LocalDateTime now = LocalDateTime.now();
        List<ApplicationVelocityResponse.WeeklyCount> weeks = new ArrayList<>();
        long total = 0;

        for (int i = 3; i >= 0; i--) {
            LocalDateTime weekEnd = now.minusWeeks(i);
            LocalDateTime weekStart = weekEnd.minusWeeks(1);
            long count = repository.countByUserIdAndCreatedAtBetween(userId, weekStart, weekEnd);
            weeks.add(new ApplicationVelocityResponse.WeeklyCount("Week " + (4 - i), count));
            total += count;
        }

        double avg = Math.round((total / 4.0) * 10.0) / 10.0;
        return new ApplicationVelocityResponse(weeks, avg);
    }

    public StatusPipelineResponse getStatusPipeline(UUID userId) {
        long applied = repository.countByUserIdAndApplicationStatusIn(userId, List.of("Applied"));
        long interview = repository.countByUserIdAndApplicationStatusIn(userId, List.of("Interview"));
        long offer = repository.countByUserIdAndApplicationStatusIn(userId, List.of("Offer"));
        long rejected = repository.countByUserIdAndApplicationStatusIn(userId, List.of("Rejected"));
        return new StatusPipelineResponse(applied, interview, offer, rejected);
    }

    public PerformanceMetricsResponse getPerformanceMetrics(UUID userId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        // All-time
        long total = repository.countByUserId(userId);
        long responded = repository.countByUserIdAndFirstRespondedAtIsNotNull(userId);
        long ghosted = repository.countByUserIdAndApplicationStatusAndUpdatedAtBefore(
                userId, "Applied", thirtyDaysAgo);

        double responseRate = total > 0 ? (double) responded / total : 0;
        double ghostingRate = total > 0 ? (double) ghosted / total : 0;

        // Last 30 days response rate
        LocalDateTime now = LocalDateTime.now();
        long last30Total = repository.countByUserIdAndCreatedAtBetween(userId, thirtyDaysAgo, now);
        long last30Responded = repository.countByUserIdAndFirstRespondedAtIsNotNullAndCreatedAtBetween(userId, thirtyDaysAgo, now);
        double last30ResponseRate = last30Total > 0 ? (double) last30Responded / last30Total : 0;

        List<JobApplication> respondedApps = repository.findByUserIdAndFirstRespondedAtIsNotNull(userId);
        double avgResponseTimeDays = respondedApps.stream()
                .mapToLong(app -> ChronoUnit.DAYS.between(app.getCreatedAt(), app.getFirstRespondedAt()))
                .average()
                .orElse(0);

        return new PerformanceMetricsResponse(responseRate, last30ResponseRate, avgResponseTimeDays, ghostingRate);
    }
}
