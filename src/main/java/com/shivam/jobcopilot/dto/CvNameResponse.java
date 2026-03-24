package com.shivam.jobcopilot.dto;

import java.util.UUID;

public record CvNameResponse(UUID id, String name, boolean isDefaultCv) {
}