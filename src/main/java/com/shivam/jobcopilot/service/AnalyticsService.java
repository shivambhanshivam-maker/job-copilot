package com.shivam.jobcopilot.service;

import com.shivam.jobcopilot.dto.ApplicationVelocityResponse;
import com.shivam.jobcopilot.dto.ChannelEffectivenessResponse;
import com.shivam.jobcopilot.dto.FunnelConversionResponse;
import com.shivam.jobcopilot.dto.PerformanceMetricsResponse;
import com.shivam.jobcopilot.dto.StatusPipelineResponse;
import com.shivam.jobcopilot.entity.JobApplication;
import com.shivam.jobcopilot.repository.JobApplicationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AnalyticsService {

    private final JobApplicationRepository repository;

    public AnalyticsService(JobApplicationRepository repository) {
        this.repository = repository;
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

    public FunnelConversionResponse getFunnelConversion(UUID userId) {
        List<String> resolvedStatuses = List.of("Interview", "Offer", "Rejected", "Closed");
        List<String> interviewStatuses = List.of("Interview", "Offer");

        // Current (all time)
        long resolved = repository.countByUserIdAndApplicationStatusIn(userId, resolvedStatuses);
        long interviews = repository.countByUserIdAndApplicationStatusIn(userId, interviewStatuses);
        long offers = repository.countByUserIdAndApplicationStatusIn(userId, List.of("Offer"));

        double currentA2I = resolved > 0 ? (double) interviews / resolved : 0;
        double currentI2O = interviews > 0 ? (double) offers / interviews : 0;

        // Previous (excluding last 30 days — what the rate looked like 30 days ago)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long prevResolved = repository.countByUserIdAndApplicationStatusInAndCreatedAtBefore(userId, resolvedStatuses, thirtyDaysAgo);
        long prevInterviews = repository.countByUserIdAndApplicationStatusInAndCreatedAtBefore(userId, interviewStatuses, thirtyDaysAgo);
        long prevOffers = repository.countByUserIdAndApplicationStatusInAndCreatedAtBefore(userId, List.of("Offer"), thirtyDaysAgo);

        double prevA2I = prevResolved > 0 ? (double) prevInterviews / prevResolved : 0;
        double prevI2O = prevInterviews > 0 ? (double) prevOffers / prevInterviews : 0;

        return new FunnelConversionResponse(
                resolved, interviews, currentA2I, currentA2I - prevA2I,
                offers, currentI2O, currentI2O - prevI2O
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

        // Previous snapshot (excluding last 30 days)
        long prevTotal = repository.countByUserIdAndCreatedAtBefore(userId, thirtyDaysAgo);
        long prevResponded = repository.countByUserIdAndFirstRespondedAtIsNotNullAndCreatedAtBefore(userId, thirtyDaysAgo);
        double prevResponseRate = prevTotal > 0 ? (double) prevResponded / prevTotal : 0;

        List<JobApplication> respondedApps = repository.findByUserIdAndFirstRespondedAtIsNotNull(userId);
        double avgResponseTimeDays = respondedApps.stream()
                .mapToLong(app -> ChronoUnit.DAYS.between(app.getCreatedAt(), app.getFirstRespondedAt()))
                .average()
                .orElse(0);

        return new PerformanceMetricsResponse(responseRate, responseRate - prevResponseRate, avgResponseTimeDays, ghostingRate);
    }
}
