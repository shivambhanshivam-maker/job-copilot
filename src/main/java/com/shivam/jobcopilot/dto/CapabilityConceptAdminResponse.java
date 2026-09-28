package com.shivam.jobcopilot.dto;

import com.shivam.jobcopilot.entity.CapabilityConcept;
import com.shivam.jobcopilot.entity.GapMention;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CapabilityConceptAdminResponse(
        UUID id,
        String name,
        String description,
        String status,
        LocalDateTime createdAt,
        List<String> aliases,
        List<Example> examples
) {
    public static CapabilityConceptAdminResponse from(CapabilityConcept concept, List<GapMention> examples) {
        return new CapabilityConceptAdminResponse(
                concept.getId(),
                concept.getName(),
                concept.getDescription(),
                concept.getStatus(),
                concept.getCreatedAt(),
                concept.getAliases().stream().toList(),
                examples.stream().map(Example::from).toList()
        );
    }

    public record Example(
            UUID gapMentionId,
            UUID userId,
            String company,
            String jobTitle,
            String rawGapText,
            String capabilityPhrase,
            String domainContext,
            String artifact,
            String evidenceType,
            Double mappingConfidence
    ) {
        public static Example from(GapMention gap) {
            return new Example(
                    gap.getId(),
                    gap.getUserId(),
                    gap.getCompany(),
                    gap.getJobTitle(),
                    gap.getRawGapText(),
                    gap.getCapabilityPhrase(),
                    gap.getDomainContext(),
                    gap.getArtifact(),
                    gap.getEvidenceType(),
                    gap.getMappingConfidence()
            );
        }
    }
}
