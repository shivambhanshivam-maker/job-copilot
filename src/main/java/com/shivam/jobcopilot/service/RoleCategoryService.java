package com.shivam.jobcopilot.service;

import com.shivam.jobcopilot.entity.RoleCategory;
import com.shivam.jobcopilot.repository.RoleCategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RoleCategoryService {

    private final RoleCategoryRepository repository;

    public RoleCategoryService(RoleCategoryRepository repository) {
        this.repository = repository;
    }

    public List<RoleCategory> getAll() {
        return repository.findAll();
    }

    // Used by AI classification prompt — all categories so user can be classified into any role, not just selected ones
    public List<String> getAllNames() {
        return repository.findAll().stream().map(RoleCategory::getName).toList();
    }

    // Only custom categories (isDefault = false) can be deleted
    public void delete(UUID id) {
        RoleCategory category = getById(id);
        if (category.isDefault()) {
            throw new IllegalArgumentException("Default categories cannot be deleted.");
        }
        repository.deleteById(id);
    }

    private RoleCategory getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role category not found: " + id));
    }
}