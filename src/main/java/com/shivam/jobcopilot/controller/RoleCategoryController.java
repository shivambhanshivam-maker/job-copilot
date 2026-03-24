package com.shivam.jobcopilot.controller;

import com.shivam.jobcopilot.entity.RoleCategory;
import com.shivam.jobcopilot.service.RoleCategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/role-categories")
public class RoleCategoryController {

    private final RoleCategoryService roleCategoryService;

    public RoleCategoryController(RoleCategoryService roleCategoryService) {
        this.roleCategoryService = roleCategoryService;
    }

    // Catalog endpoint — used by the UI to populate the role category picker in Preferences
    @GetMapping
    public List<RoleCategory> getAll() {
        return roleCategoryService.getAll();
    }

    // Only custom (non-default) categories can be deleted
    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        roleCategoryService.delete(id);
    }
}