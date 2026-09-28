package com.shivam.jobcopilot.controller;

import com.shivam.jobcopilot.dto.CapabilityConceptAdminResponse;
import com.shivam.jobcopilot.dto.GapNormalizationRunResponse;
import com.shivam.jobcopilot.dto.GapNormalizationStatusResponse;
import com.shivam.jobcopilot.service.CapabilityConceptAdminService;
import com.shivam.jobcopilot.service.GapNormalizationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/gap-normalization")
public class GapNormalizationAdminController {

    private final GapNormalizationService gapNormalizationService;
    private final CapabilityConceptAdminService capabilityConceptAdminService;

    public GapNormalizationAdminController(GapNormalizationService gapNormalizationService,
                                           CapabilityConceptAdminService capabilityConceptAdminService) {
        this.gapNormalizationService = gapNormalizationService;
        this.capabilityConceptAdminService = capabilityConceptAdminService;
    }

    @PostMapping("/run")
    public GapNormalizationRunResponse run() {
        return gapNormalizationService.runPendingNormalization();
    }

    @GetMapping("/status")
    public GapNormalizationStatusResponse status() {
        return capabilityConceptAdminService.status();
    }

    @GetMapping("/concepts")
    public List<CapabilityConceptAdminResponse> listConcepts() {
        return capabilityConceptAdminService.listConcepts();
    }

    @PostMapping("/concepts/{id}/approve")
    public CapabilityConceptAdminResponse approveConcept(@PathVariable UUID id) {
        return capabilityConceptAdminService.approve(id);
    }

    @PostMapping("/concepts/{id}/archive")
    public CapabilityConceptAdminResponse archiveConcept(@PathVariable UUID id) {
        return capabilityConceptAdminService.archive(id);
    }
}
