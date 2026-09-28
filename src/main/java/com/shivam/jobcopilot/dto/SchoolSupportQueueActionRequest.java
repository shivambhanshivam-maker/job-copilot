package com.shivam.jobcopilot.dto;

import java.util.UUID;

public record SchoolSupportQueueActionRequest(
        UUID studentUserId,
        String signalType,
        String signalFingerprint,
        Integer snoozeDays
) {}
