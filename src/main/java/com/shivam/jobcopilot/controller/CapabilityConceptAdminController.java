package com.shivam.jobcopilot.controller;

import com.shivam.jobcopilot.dto.CapabilityConceptAdminResponse;
import com.shivam.jobcopilot.service.CapabilityConceptAdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/capability-concepts")
public class CapabilityConceptAdminController {

    private final CapabilityConceptAdminService capabilityConceptAdminService;

    public CapabilityConceptAdminController(CapabilityConceptAdminService capabilityConceptAdminService) {
        this.capabilityConceptAdminService = capabilityConceptAdminService;
    }

    @GetMapping
    public List<CapabilityConceptAdminResponse> listConcepts() {
        return capabilityConceptAdminService.listConcepts();
    }

    @PostMapping("/{id}/approve")
    public CapabilityConceptAdminResponse approveConcept(@PathVariable UUID id) {
        return capabilityConceptAdminService.approve(id);
    }

    @PostMapping("/{id}/archive")
    public CapabilityConceptAdminResponse archiveConcept(@PathVariable UUID id) {
        return capabilityConceptAdminService.archive(id);
    }
}
