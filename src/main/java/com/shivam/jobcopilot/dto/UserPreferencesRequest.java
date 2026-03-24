package com.shivam.jobcopilot.dto;

import com.shivam.jobcopilot.entity.ExperienceLevel;

import java.util.List;
import java.util.Set;

public class UserPreferencesRequest {
    private ExperienceLevel experienceLevel;
    private Set<String> preferredRoleCategories;
    private List<PreferredLocationDto> preferredLocations;

    public ExperienceLevel getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(ExperienceLevel experienceLevel) { this.experienceLevel = experienceLevel; }

    public Set<String> getPreferredRoleCategories() { return preferredRoleCategories; }
    public void setPreferredRoleCategories(Set<String> preferredRoleCategories) { this.preferredRoleCategories = preferredRoleCategories; }

    public List<PreferredLocationDto> getPreferredLocations() { return preferredLocations; }
    public void setPreferredLocations(List<PreferredLocationDto> preferredLocations) { this.preferredLocations = preferredLocations; }
}