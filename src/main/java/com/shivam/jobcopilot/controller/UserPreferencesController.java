package com.shivam.jobcopilot.controller;

import com.shivam.jobcopilot.dto.UserPreferencesRequest;
import com.shivam.jobcopilot.entity.UserPreferences;
import com.shivam.jobcopilot.service.UserPreferencesService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/preferences")
public class UserPreferencesController {

    private final UserPreferencesService service;

    public UserPreferencesController(UserPreferencesService service) {
        this.service = service;
    }

    private UUID currentUserId(Authentication auth) {
        return (UUID) auth.getPrincipal();
    }

    @GetMapping
    public UserPreferences get(Authentication auth) {
        return service.get(currentUserId(auth));
    }

    @PutMapping
    public UserPreferences save(@RequestBody UserPreferencesRequest request, Authentication auth) {
        return service.save(request, currentUserId(auth));
    }
}
