package com.shivam.jobcopilot.controller;

import com.shivam.jobcopilot.dto.ApplicationVelocityResponse;
import com.shivam.jobcopilot.dto.ChannelEffectivenessResponse;
import com.shivam.jobcopilot.dto.FunnelConversionResponse;
import com.shivam.jobcopilot.dto.PerformanceMetricsResponse;
import com.shivam.jobcopilot.dto.StatusPipelineResponse;
import com.shivam.jobcopilot.service.AnalyticsService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    private UUID currentUserId(Authentication auth) {
        return (UUID) auth.getPrincipal();
    }

    @GetMapping("/channel-effectiveness")
    public ChannelEffectivenessResponse getChannelEffectiveness(Authentication auth) {
        return analyticsService.getChannelEffectiveness(currentUserId(auth));
    }

    @GetMapping("/funnel-conversion")
    public FunnelConversionResponse getFunnelConversion(Authentication auth) {
        return analyticsService.getFunnelConversion(currentUserId(auth));
    }

    @GetMapping("/application-velocity")
    public ApplicationVelocityResponse getApplicationVelocity(Authentication auth) {
        return analyticsService.getApplicationVelocity(currentUserId(auth));
    }

    @GetMapping("/status-pipeline")
    public StatusPipelineResponse getStatusPipeline(Authentication auth) {
        return analyticsService.getStatusPipeline(currentUserId(auth));
    }

    @GetMapping("/performance-metrics")
    public PerformanceMetricsResponse getPerformanceMetrics(Authentication auth) {
        return analyticsService.getPerformanceMetrics(currentUserId(auth));
    }
}
