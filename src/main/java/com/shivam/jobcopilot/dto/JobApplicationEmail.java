package com.shivam.jobcopilot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JobApplicationEmail(
        String companyNameRaw,
        String companyNameCanonical,
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
    @JsonProperty("company")
    public String company() {
        return companyNameCanonical != null && !companyNameCanonical.isBlank()
                ? companyNameCanonical : companyNameRaw;
    }
}
