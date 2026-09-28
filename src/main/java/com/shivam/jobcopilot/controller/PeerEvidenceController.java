package com.shivam.jobcopilot.controller;

import com.shivam.jobcopilot.dto.PeerEvidencePatternResponse;
import com.shivam.jobcopilot.service.PeerEvidenceAggregatorService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/student/peer-evidence")
public class PeerEvidenceController {

    private final PeerEvidenceAggregatorService aggregatorService;

    public PeerEvidenceController(PeerEvidenceAggregatorService aggregatorService) {
        this.aggregatorService = aggregatorService;
    }

    @GetMapping
    public PeerEvidencePatternResponse getPeerEvidence(Authentication auth) {
        return aggregatorService.getForStudent((UUID) auth.getPrincipal());
    }
}
