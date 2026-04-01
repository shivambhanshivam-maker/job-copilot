package com.shivam.jobcopilot.dto;

import com.shivam.jobcopilot.entity.PostApplicationInsight;

public record PostApplicationInsightResult(PostApplicationInsight insight, boolean isStale) {}
