package com.shivam.jobcopilot.service;

import com.shivam.jobcopilot.dto.CapabilityConceptAdminResponse;
import com.shivam.jobcopilot.dto.GapNormalizationStatusResponse;
import com.shivam.jobcopilot.entity.CapabilityConcept;
import com.shivam.jobcopilot.repository.CapabilityConceptRepository;
import com.shivam.jobcopilot.repository.GapMentionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class CapabilityConceptAdminService {

    private final CapabilityConceptRepository capabilityConceptRepository;
    private final GapMentionRepository gapMentionRepository;

    public CapabilityConceptAdminService(CapabilityConceptRepository capabilityConceptRepository,
                                         GapMentionRepository gapMentionRepository) {
        this.capabilityConceptRepository = capabilityConceptRepository;
        this.gapMentionRepository = gapMentionRepository;
    }

    public GapNormalizationStatusResponse status() {
        List<CapabilityConcept> concepts = capabilityConceptRepository.findAll();
        return new GapNormalizationStatusResponse(
                gapMentionRepository.countByNormalizationStatus("PENDING"),
                gapMentionRepository.countByNormalizationStatus("LOW_SUPPORT"),
                gapMentionRepository.countByNormalizationStatus("MAPPED"),
                gapMentionRepository.countByNormalizationStatus("FAILED"),
                countStatus(concepts, "CANDIDATE"),
                countStatus(concepts, "APPROVED"),
                countStatus(concepts, "ARCHIVED")
        );
    }

    public List<CapabilityConceptAdminResponse> listConcepts() {
        return capabilityConceptRepository.findAll().stream()
                .sorted(Comparator
                        .comparing(CapabilityConcept::getStatus, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(CapabilityConcept::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(concept -> CapabilityConceptAdminResponse.from(
                        concept,
                        gapMentionRepository.findTop50ByConceptIdOrderByCreatedAtDesc(concept.getId())
                ))
                .toList();
    }

    public CapabilityConceptAdminResponse approve(UUID id) {
        return updateStatus(id, "APPROVED");
    }

    public CapabilityConceptAdminResponse archive(UUID id) {
        return updateStatus(id, "ARCHIVED");
    }

    private CapabilityConceptAdminResponse updateStatus(UUID id, String status) {
        CapabilityConcept concept = capabilityConceptRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Capability concept not found"));
        concept.setStatus(status);
        CapabilityConcept saved = capabilityConceptRepository.save(concept);
        return CapabilityConceptAdminResponse.from(
                saved,
                gapMentionRepository.findTop50ByConceptIdOrderByCreatedAtDesc(saved.getId())
        );
    }

    private long countStatus(List<CapabilityConcept> concepts, String status) {
        return concepts.stream()
                .filter(concept -> status.equalsIgnoreCase(safe(concept.getStatus())))
                .count();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
