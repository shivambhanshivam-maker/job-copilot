package com.shivam.jobcopilot.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MergeDecision(
        String action,              // UPDATE, CREATE, IGNORE
        UUID applicationId,         // null for CREATE / IGNORE
        UUID fitAnalysisId,         // null if no matching fit analysis found
        Map<String, String> fieldsToSet,
        List<String> fieldsToClear,
        String reasoning
) {}
