package com.shivam.jobcopilot.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SchoolOverviewResponse(
        UUID schoolId,
        String schoolName,
        long activeStudents,
        long applicationsTracked,
        long recruiterResponses,
        long interviews,
        long offers,
        double responseRate,
        double interviewRate,
        LocalDateTime latestActivityAt,
        List<BreakdownItem> topRoleCategories,
        List<BreakdownItem> topEmployers
) {
    public record BreakdownItem(String label, long count) {}
}
