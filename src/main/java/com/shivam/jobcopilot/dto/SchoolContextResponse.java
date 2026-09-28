package com.shivam.jobcopilot.dto;

import com.shivam.jobcopilot.entity.AdvisorRole;

import java.util.UUID;

public record SchoolContextResponse(
        UUID advisorId,
        String advisorName,
        String advisorEmail,
        String advisorTitle,
        AdvisorRole advisorRole,
        UUID schoolId,
        String schoolName,
        String schoolSlug,
        String schoolDomain
) {}
