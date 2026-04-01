package com.shivam.jobcopilot.dto;

public record FunnelConversionResponse(
        long resolvedApplications,
        long interviews,
        double appliedToInterviewRate,       // all-time
        double appliedToInterviewLast30Days, // last 30 days only
        long offers,
        double interviewToOfferRate,         // all-time
        double interviewToOfferLast30Days    // last 30 days only
) {}
