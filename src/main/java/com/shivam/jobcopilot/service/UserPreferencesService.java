package com.shivam.jobcopilot.service;

import com.shivam.jobcopilot.dto.PreferredLocationDto;
import com.shivam.jobcopilot.dto.UserPreferencesRequest;
import com.shivam.jobcopilot.entity.PreferredLocation;
import com.shivam.jobcopilot.entity.RoleCategory;
import com.shivam.jobcopilot.entity.UserPreferences;
import com.shivam.jobcopilot.repository.RoleCategoryRepository;
import com.shivam.jobcopilot.repository.UserPreferencesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class UserPreferencesService {

    private final UserPreferencesRepository preferencesRepository;
    private final RoleCategoryRepository roleCategoryRepository;

    public UserPreferencesService(UserPreferencesRepository preferencesRepository,
                                  RoleCategoryRepository roleCategoryRepository) {
        this.preferencesRepository = preferencesRepository;
        this.roleCategoryRepository = roleCategoryRepository;
    }

    public UserPreferences get(UUID userId) {
        return preferencesRepository.findByUserId(userId)
                .orElseGet(UserPreferences::new);
    }

    @Transactional
    public UserPreferences save(UserPreferencesRequest request, UUID userId) {
        // Upsert: update if exists, create if not
        UserPreferences prefs = preferencesRepository.findByUserId(userId)
                .orElseGet(UserPreferences::new);

        prefs.setUserId(userId);
        prefs.setExperienceLevel(request.getExperienceLevel());

        // Ensure any custom category names exist in the catalog before saving
        ensureCategoriesExist(request.getPreferredRoleCategories());
        prefs.setPreferredRoleCategories(request.getPreferredRoleCategories());

        // Full replace — orphanRemoval handles deleting old locations
        prefs.getPreferredLocations().clear();
        if (request.getPreferredLocations() != null) {
            List<PreferredLocation> locations = request.getPreferredLocations().stream()
                    .map(this::toEntity)
                    .toList();
            prefs.getPreferredLocations().addAll(locations);
        }

        return preferencesRepository.save(prefs);
    }

    // Used by the nightly scheduler — returns all users' prefs
    public List<UserPreferences> getAll() {
        return preferencesRepository.findAll();
    }

    // Creates catalog entries for any category names that don't already exist
    private void ensureCategoriesExist(Set<String> categoryNames) {
        if (categoryNames == null) return;
        for (String name : categoryNames) {
            if (name == null || name.isBlank()) continue;
            if (!roleCategoryRepository.existsByName(name.trim())) {
                RoleCategory custom = new RoleCategory();
                custom.setName(name.trim());
                custom.setDefault(false);
                custom.setActive(true);
                roleCategoryRepository.save(custom);
            }
        }
    }

    private PreferredLocation toEntity(PreferredLocationDto dto) {
        PreferredLocation loc = new PreferredLocation();
        loc.setCityName(dto.getCityName());
        loc.setCountryCode(dto.getCountryCode());
        loc.setDisplayName(dto.getDisplayName());
        return loc;
    }
}
