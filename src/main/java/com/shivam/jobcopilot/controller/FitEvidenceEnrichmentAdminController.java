package com.shivam.jobcopilot.controller;

import com.shivam.jobcopilot.dto.FitEvidenceEnrichmentResponse;
import com.shivam.jobcopilot.service.FitEvidenceEnrichmentService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/fit-evidence")
public class FitEvidenceEnrichmentAdminController {

    private final FitEvidenceEnrichmentService enrichmentService;

    public FitEvidenceEnrichmentAdminController(FitEvidenceEnrichmentService enrichmentService) {
        this.enrichmentService = enrichmentService;
    }

    @PostMapping("/enrich/{fitAnalysisId}")
    public FitEvidenceEnrichmentResponse enrichOne(@PathVariable UUID fitAnalysisId) {
        return enrichmentService.enrichOne(fitAnalysisId);
    }

    @PostMapping("/enrich-applied")
    public FitEvidenceEnrichmentResponse.Run enrichApplied() {
        return enrichmentService.enrichAppliedAnalyses();
    }
}
