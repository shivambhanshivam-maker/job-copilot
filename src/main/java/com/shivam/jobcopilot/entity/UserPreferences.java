package com.shivam.jobcopilot.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "user_preferences")
public class UserPreferences {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(unique = true)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    private ExperienceLevel experienceLevel;

    // Stored as strings matching RoleCategory.name — avoids FK dependency on RANDOM_UUID() seeded IDs
    @ElementCollection
    @CollectionTable(name = "preferred_role_categories", joinColumns = @JoinColumn(name = "preferences_id"))
    @Column(name = "role_category_name")
    private Set<String> preferredRoleCategories = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "preferences_id")
    private List<PreferredLocation> preferredLocations = new ArrayList<>();

    public UserPreferences() {}

    public UUID getId() { return id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public ExperienceLevel getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(ExperienceLevel experienceLevel) { this.experienceLevel = experienceLevel; }

    public Set<String> getPreferredRoleCategories() { return preferredRoleCategories; }
    public void setPreferredRoleCategories(Set<String> preferredRoleCategories) { this.preferredRoleCategories = preferredRoleCategories; }

    public List<PreferredLocation> getPreferredLocations() { return preferredLocations; }
    public void setPreferredLocations(List<PreferredLocation> preferredLocations) { this.preferredLocations = preferredLocations; }
}