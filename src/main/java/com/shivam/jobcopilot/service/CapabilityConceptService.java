package com.shivam.jobcopilot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shivam.jobcopilot.entity.CapabilityConcept;
import com.shivam.jobcopilot.repository.CapabilityConceptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CapabilityConceptService {

    private static final Logger log = LoggerFactory.getLogger(CapabilityConceptService.class);

    private final ChatClient chatClient;
    private final CapabilityConceptRepository capabilityConceptRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CapabilityConceptService(ChatClient.Builder chatClientBuilder,
                                    CapabilityConceptRepository capabilityConceptRepository) {
        this.capabilityConceptRepository = capabilityConceptRepository;
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        You normalize career-readiness gaps into reusable capability concepts.
                        You MUST respond with ONLY a valid JSON object, no markdown, no extra text.

                        Your job is semantic normalization, not keyword matching.
                        Different wording can map to the same concept when the underlying capability is the same.
                        Similar-sounding wording must stay separate when the hiring signal is meaningfully different.

                        Do not use a fixed taxonomy. Infer the smallest useful concept set from the actual gaps.
                        Concept names should be concise, human-readable capabilities, not tools-only labels unless the gap is truly tool-specific.

                        Return this exact shape:
                        {
                          "mappings": [
                            {
                              "id": "<input id>",
                              "conceptName": "<normalized capability concept>",
                              "description": "<one sentence explaining what this concept means>",
                              "aliases": ["<raw/similar phrases this concept covers>"],
                              "confidence": <number between 0 and 1>
                            }
                          ]
                        }

                        Confidence rules:
                        - 0.85-1.0: strong semantic match to a clear capability.
                        - 0.70-0.84: reasonable match, but wording/context is somewhat broad.
                        - below 0.70: uncertain; concept should not drive recurring insight generation.
                        """)
                .build();
    }

    public Map<String, ConceptMapping> mapGaps(List<GapInput> gaps) {
        List<GapInput> usableGaps = gaps.stream()
                .filter(gap -> !safe(gap.rawGap()).isBlank())
                .toList();
        if (usableGaps.isEmpty()) return Map.of();

        try {
            String response = chatClient.prompt()
                    .user(buildPrompt(usableGaps))
                    .call()
                    .content();
            JsonNode root = objectMapper.readTree(response);

            Map<String, ConceptMapping> mappings = new LinkedHashMap<>();
            for (JsonNode node : root.path("mappings")) {
                String id = getText(node, "id");
                String conceptName = normalizeConceptName(getText(node, "conceptName"));
                if (id == null || conceptName == null) continue;

                CapabilityConcept concept = findOrCreateConcept(
                        conceptName,
                        getText(node, "description"),
                        aliases(node.path("aliases"))
                );
                mappings.put(id, new ConceptMapping(concept.getId(), concept.getName(), confidence(node)));
            }
            return withFallbacks(usableGaps, mappings);
        } catch (Exception e) {
            log.warn("Capability concept normalization failed; using low-confidence raw gap labels", e);
            return usableGaps.stream()
                    .collect(Collectors.toMap(
                            GapInput::id,
                            gap -> fallback(gap.rawGap()),
                            (existing, duplicate) -> existing,
                            LinkedHashMap::new
                    ));
        }
    }

    private String buildPrompt(List<GapInput> gaps) {
        List<CapabilityConcept> existingConcepts = capabilityConceptRepository.findAll();
        String existing = existingConcepts.isEmpty()
                ? "No existing concepts yet."
                : existingConcepts.stream()
                .map(concept -> "- " + concept.getName()
                        + (safe(concept.getDescription()).isBlank() ? "" : ": " + concept.getDescription())
                        + (concept.getAliases().isEmpty() ? "" : " Aliases: " + String.join(", ", concept.getAliases())))
                .collect(Collectors.joining("\n"));

        String inputs = gaps.stream()
                .map(gap -> """
                        - id: %s
                          role: %s
                          rawGap: %s
                        """.formatted(gap.id(), safe(gap.roleContext()), gap.rawGap()))
                .collect(Collectors.joining("\n"));

        return """
                Existing capability concepts:
                %s

                Normalize these raw gaps. Reuse an existing concept only when it is semantically appropriate.
                You may create new concepts when the existing registry does not fit.

                Raw gaps:
                %s
                """.formatted(existing, inputs);
    }

    private Map<String, ConceptMapping> withFallbacks(List<GapInput> gaps, Map<String, ConceptMapping> mappings) {
        Map<String, ConceptMapping> result = new LinkedHashMap<>(mappings);
        for (GapInput gap : gaps) {
            result.putIfAbsent(gap.id(), fallback(gap.rawGap()));
        }
        return result;
    }

    private CapabilityConcept findOrCreateConcept(String name, String description, Set<String> aliases) {
        Optional<CapabilityConcept> existing = capabilityConceptRepository.findByNameIgnoreCase(name);
        if (existing.isPresent()) {
            CapabilityConcept concept = existing.get();
            if (safe(concept.getDescription()).isBlank() && !safe(description).isBlank()) {
                concept.setDescription(description);
            }
            concept.getAliases().addAll(aliases);
            return capabilityConceptRepository.save(concept);
        }

        CapabilityConcept concept = new CapabilityConcept(name, description, aliases);
        concept.setStatus("CANDIDATE");
        return capabilityConceptRepository.save(concept);
    }

    private Set<String> aliases(JsonNode aliasesNode) {
        Set<String> aliases = new LinkedHashSet<>();
        if (aliasesNode.isArray()) {
            for (JsonNode alias : aliasesNode) {
                String text = alias.asText(null);
                if (text != null && !text.isBlank()) aliases.add(text.trim());
            }
        }
        return aliases;
    }

    private double confidence(JsonNode node) {
        double confidence = node.path("confidence").asDouble(0.6);
        if (confidence < 0) return 0;
        if (confidence > 1) return 1;
        return confidence;
    }

    private String getText(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) return null;
        String value = field.asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeConceptName(String value) {
        if (value == null) return null;
        String cleaned = value.replaceAll("\\s+", " ").trim();
        if (cleaned.isBlank()) return null;
        if (cleaned.length() > 80) cleaned = cleaned.substring(0, 80).trim();
        return Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
    }

    private ConceptMapping fallback(String rawGap) {
        return new ConceptMapping(null, toConceptName(rawGap), 0.55);
    }

    private String toConceptName(String rawGap) {
        String cleaned = safe(rawGap)
                .replaceAll("[^A-Za-z0-9 &/-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.length() > 54) cleaned = cleaned.substring(0, 54).trim();
        if (cleaned.isBlank()) return "General capability gap";
        return Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record GapInput(String id, String rawGap, String roleContext) {}
    public record ConceptMapping(UUID conceptId, String conceptName, double confidence) {}
}
