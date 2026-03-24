package com.shivam.jobcopilot.entity;

public enum ExperienceLevel {
    EARLY_CAREER,   // maps to "less_than_3_years_experience" in JSearch
    EXPERIENCED;    // maps to "more_than_3_years_experience" in JSearch

    public String toApiValue() {
        return switch (this) {
            case EARLY_CAREER -> "less_than_3_years_experience";
            case EXPERIENCED  -> "more_than_3_years_experience";
        };
    }
}