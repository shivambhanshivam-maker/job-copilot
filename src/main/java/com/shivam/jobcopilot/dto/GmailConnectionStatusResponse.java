package com.shivam.jobcopilot.dto;

import java.time.LocalDateTime;

public record GmailConnectionStatusResponse(
        boolean connected,
        String status,
        String message,
        LocalDateTime lastSuccessfulPollAt
) {}
