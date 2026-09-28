package com.shivam.jobcopilot.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record EmailReviewResponse(
        UUID id,
        String companyNameRaw,
        String companyNameCanonical,
        String jobTitle,
        String recruiterName,
        String recruiterEmail,
        String applicationStatus,
        String referral,
        String roleCategory,
        String interviewDateAndTime,
        String updateSummary,
        String reviewReason,
        LocalDateTime createdAt,
        List<Candidate> candidates
) {
    public record Candidate(
            UUID id,
            String company,
            String jobTitle,
            String applicationStatus,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
