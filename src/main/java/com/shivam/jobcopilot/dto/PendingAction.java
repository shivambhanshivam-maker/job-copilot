package com.shivam.jobcopilot.dto;

import java.util.UUID;

public record PendingAction(
        UUID jobApplicationId,
        String company,
        String jobTitle,
        String message,
        String suggestedStatus,
        String actionType,  // "FOLLOW_UP_REFERRAL" | "STALE_APPLICATION"
        int snoozeDays      // passed directly to ?days= on the snooze endpoint
) {
}
