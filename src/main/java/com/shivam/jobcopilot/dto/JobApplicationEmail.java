package com.shivam.jobcopilot.dto;

public record JobApplicationEmail(
        String company,
        String jobTitle,
        String recruiterName,
        String recruiterEmail,
        String applicationStatus,
        String referral,
        String roleCategory,
        String interviewDateAndTime,
        String gmailMessageId,
        String updateSummary
) {
}
