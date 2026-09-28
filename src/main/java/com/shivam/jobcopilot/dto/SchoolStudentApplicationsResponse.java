package com.shivam.jobcopilot.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SchoolStudentApplicationsResponse(
        UUID studentUserId,
        String studentName,
        String studentEmail,
        List<ApplicationItem> applications
) {
    public record ApplicationItem(
            UUID id,
            String company,
            String jobTitle,
            String roleCategory,
            String applicationStatus,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime firstRespondedAt,
            LocalDateTime interviewDate
    ) {}
}
