package com.shivam.jobcopilot.dto;

public record ChannelEffectivenessResponse(
        long referralApplications,
        long referralInterviews,
        double referralYield,
        long directApplications,
        long directInterviews,
        double directYield,
        double multiplier,
        double benchmarkMultiplier
        // boolean hasEnoughData  -- to be implemented later
) {}