package com.shivam.jobcopilot.dto;

public record GapNormalizationStatusResponse(
        long pending,
        long lowSupport,
        long mapped,
        long failed,
        long candidateConcepts,
        long approvedConcepts,
        long archivedConcepts
) {}
