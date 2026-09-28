package com.shivam.jobcopilot.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MergeDecision(
        String action,              // UPDATE, CREATE, IGNORE, UNRESOLVED
        UUID applicationId,         // null for CREATE / IGNORE
        UUID fitAnalysisId,         // null if no matching fit analysis found
        Map<String, String> fieldsToSet,
        List<String> fieldsToClear,
        String reasoning
) {
    public MergeDecision {
        // CREATE, IGNORE, and some model responses do not have field mutations.
        // Keep the API collection-shaped so clients never need to handle null lists/maps.
        fieldsToSet = fieldsToSet == null ? Map.of() : fieldsToSet;
        fieldsToClear = fieldsToClear == null ? List.of() : fieldsToClear;
    }
}
