package com.shivam.jobcopilot.dto;

public record PerformanceMetricsResponse(
        double responseRate,             // all-time
        double responseRateLast30Days,   // last 30 days only
        double avgResponseTimeDays,
        double ghostingRate
) {}
