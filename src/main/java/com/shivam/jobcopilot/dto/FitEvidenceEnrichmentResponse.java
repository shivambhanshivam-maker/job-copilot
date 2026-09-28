package com.shivam.jobcopilot.dto;

import java.util.List;
import java.util.UUID;

public record FitEvidenceEnrichmentResponse(
        UUID fitAnalysisId,
        String status,
        String message
) {
    public record Run(
            int requested,
            int enriched,
            int skippedNoCv,
            int skippedNoText,
            int failed,
            List<String> errors
    ) {}
}
