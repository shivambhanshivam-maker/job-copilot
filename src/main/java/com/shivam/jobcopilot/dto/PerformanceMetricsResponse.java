package com.shivam.jobcopilot.dto;

public record PerformanceMetricsResponse(
        double responseRate,
        double responseRateDelta,   // vs 30 days ago
        double avgResponseTimeDays,
        double ghostingRate
) {}
