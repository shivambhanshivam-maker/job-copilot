package com.shivam.jobcopilot.dto;

import java.time.LocalDateTime;

public record OutlookConnectionStatusResponse(
        boolean connected,
        String status,
        String message,
        LocalDateTime lastSuccessfulPollAt
) {}
