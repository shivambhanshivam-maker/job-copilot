package com.shivam.jobcopilot.dto;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public record SchoolSupportQueueResponse(
        UUID schoolId,
        String schoolName,
        long eligibleStudents,
        List<SupportQueueItem> items
) {
    public record SupportQueueItem(
            UUID studentUserId,
            String studentName,
            String studentEmail,
            String advisorVisibilityLevel,
            String severity,
            LocalDateTime lastActivityAt,
            SupportSummary summary,
            List<SupportSignal> signals
    ) {}

    public record SupportSummary(
            long totalApplications,
            long applicationsLast30Days,
            long responsesLast30Days,
            long interviews,
            long offers,
            LocalDateTime latestActivityAt,
            List<SummaryBucket> roleCategories,
            List<SummaryBucket> statuses
    ) {}

    public record SummaryBucket(String label, long count) {}

    public record SupportSignal(
            String type,
            String severity,
            String reason,
            List<String> evidence,
            String suggestedAction,
            String fingerprint
    ) {
        public SupportSignal(String type,
                             String severity,
                             String reason,
                             List<String> evidence,
                             String suggestedAction) {
            this(type, severity, reason, evidence, suggestedAction, fingerprintFor(type, evidence));
        }

        private static String fingerprintFor(String type, List<String> evidence) {
            String stableEvidence = evidence == null
                    ? ""
                    : evidence.stream()
                    // Counts may change without representing a new support situation.
                    // Keep zero/non-zero state while ignoring volatile magnitudes.
                    .map(value -> value.replaceAll("[1-9][0-9]*", "#"))
                    .reduce((left, right) -> left + "|" + right)
                    .orElse("");
            return UUID.nameUUIDFromBytes((type + "|" + stableEvidence).getBytes(StandardCharsets.UTF_8)).toString();
        }
    }
}
