package com.shivam.jobcopilot.dto;

public record FunnelConversionResponse(
        long resolvedApplications,    // Interview + Offer + Rejected + Closed (excludes pending "Applied")
        long interviews,              // Interview + Offer
        double appliedToInterviewRate,
        double appliedToInterviewDelta, // vs prev week
        long offers,
        double interviewToOfferRate,
        double interviewToOfferDelta    // vs prev week
) {}
