package com.shivam.jobcopilot.dto;

public record StatusPipelineResponse(
        long applied,
        long interview,
        long offer,
        long rejected
) {}
