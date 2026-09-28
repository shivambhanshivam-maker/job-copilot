package com.shivam.jobcopilot.dto;

import java.util.List;

public record GapNormalizationRunResponse(
        int pendingSeen,
        int structured,
        int mapped,
        int candidateConceptsCreated,
        int existingConceptsReused,
        int lowSupport,
        int failed,
        List<ConceptSummary> concepts
) {
    public record ConceptSummary(
            String conceptName,
            String status,
            int mappedGapCount,
            int distinctStudents,
            int distinctRoles,
            List<String> examples
    ) {}
}
