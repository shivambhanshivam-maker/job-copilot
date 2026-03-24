package com.shivam.jobcopilot.dto;

import java.util.List;

public record ApplicationVelocityResponse(
        List<WeeklyCount> weeks,
        double avgPerWeek
) {
    public record WeeklyCount(String label, long count) {}
}
