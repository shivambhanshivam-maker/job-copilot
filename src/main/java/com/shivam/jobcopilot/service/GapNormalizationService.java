package com.shivam.jobcopilot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shivam.jobcopilot.dto.GapNormalizationRunResponse;
import com.shivam.jobcopilot.entity.CapabilityConcept;
import com.shivam.jobcopilot.entity.GapMention;
import com.shivam.jobcopilot.repository.CapabilityConceptRepository;
import com.shivam.jobcopilot.repository.GapMentionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GapNormalizationService {

    private static final Logger log = LoggerFactory.getLogger(GapNormalizationService.class);
    private static final int MIN_GAPS_FOR_CONCEPT = 3;
    private static final int MIN_ROLES_FOR_CONCEPT = 2;
    private static final double MIN_CLUSTER_CONFIDENCE = 0.70;

    private final GapMentionRepository gapMentionRepository;
    private final CapabilityConceptRepository capabilityConceptRepository;
    private final StudentInsightService studentInsightService;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GapNormalizationService(GapMentionRepository gapMentionRepository,
                                   CapabilityConceptRepository capabilityConceptRepository,
                                   StudentInsightService studentInsightService,
                                   ChatClient.Builder chatClientBuilder) {
        this.gapMentionRepository = gapMentionRepository;
        this.capabilityConceptRepository = capabilityConceptRepository;
        this.studentInsightService = studentInsightService;
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        You normalize raw job-application fit-analysis gaps into reusable career capability concepts.
                        You MUST respond with ONLY a valid JSON object, no markdown, no extra text.

                        Do three things in one response:
                        1. Structure every raw gap into reusable capability + context fields.
                        2. Cluster semantically similar gaps into reusable capability concepts.
                        3. Decide whether each cluster should REUSE an existing concept or CREATE a new candidate concept.

                        Rules:
                        - capabilityPhrase must be broad and reusable.
                        - Keep geography, industry, company, and narrow role context out of capabilityPhrase unless essential.
                        - Use domainContext/artifact/evidenceType to preserve specificity.
                        - Prefer REUSE when an existing concept is a good semantic fit.
                        - CREATE only when no existing concept fits.
                        - Do not place the same gap id in more than one cluster.

                        Return this exact JSON shape:
                        {
                          "items": [
                            {
                              "id": "<input gap id>",
                              "capabilityPhrase": "<2-5 word reusable capability>",
                              "domainContext": "<industry/geography/business context, or null>",
                              "artifact": "<work product or proof expected, or null>",
                              "evidenceType": "<what evidence is missing from the CV, or null>",
                              "confidence": <number between 0 and 1>
                            }
                          ],
                          "clusters": [
                            {
                              "action": "<REUSE | CREATE>",
                              "existingConceptId": "<existing concept UUID, or null>",
                              "conceptName": "<2-5 word reusable capability concept>",
                              "gapIds": ["<gap id>", "<gap id>"],
                              "confidence": <number between 0 and 1>,
                              "reasoning": "<short reason>"
                            }
                          ]
                        }
                        """)
                .build();
    }

    @Transactional
    public GapNormalizationRunResponse runPendingNormalization() {
        List<GapMention> gaps = gapMentionRepository
                .findTop200ByNormalizationStatusInOrderByCreatedAtAsc(List.of("PENDING", "LOW_SUPPORT"));
        if (gaps.isEmpty()) {
            return new GapNormalizationRunResponse(0, 0, 0, 0, 0, 0, 0, List.of());
        }

        List<CapabilityConcept> existingConcepts = reusableConcepts();
        NormalizationPlan plan = normalize(gaps, existingConcepts);

        Map<UUID, GapMention> gapById = gaps.stream()
                .collect(Collectors.toMap(GapMention::getId, gap -> gap, (first, duplicate) -> first, LinkedHashMap::new));
        Map<UUID, CapabilityConcept> conceptById = existingConcepts.stream()
                .collect(Collectors.toMap(CapabilityConcept::getId, concept -> concept, (first, duplicate) -> first, LinkedHashMap::new));

        int structured = applyItemStructure(plan.items(), gapById);
        int mapped = 0;
        int created = 0;
        int reused = 0;
        Set<UUID> lowSupportGapIds = new LinkedHashSet<>();
        Set<UUID> assignedGapIds = new LinkedHashSet<>();
        Set<UUID> affectedUsers = new LinkedHashSet<>();
        List<GapNormalizationRunResponse.ConceptSummary> summaries = new ArrayList<>();

        for (ClusterDecision cluster : plan.clusters().stream()
                .sorted(Comparator.comparingInt((ClusterDecision cluster) -> cluster.gapIds().size()).reversed())
                .toList()) {
            List<GapMention> group = cluster.gapIds().stream()
                    .filter(id -> !assignedGapIds.contains(id))
                    .map(gapById::get)
                    .filter(Objects::nonNull)
                    .toList();
            if (group.isEmpty()) continue;
            assignedGapIds.addAll(group.stream().map(GapMention::getId).toList());

            if (!hasSupport(group) || cluster.confidence() < MIN_CLUSTER_CONFIDENCE) {
                markLowSupport(group, lowSupportGapIds);
                continue;
            }

            ConceptResolution resolution = resolveConcept(cluster, group, conceptById);
            if (resolution == null) {
                markLowSupport(group, lowSupportGapIds);
                continue;
            }
            if (resolution.created()) created++;
            if (resolution.reused()) reused++;

            CapabilityConcept concept = resolution.concept();
            for (GapMention gap : group) {
                gap.setConceptId(concept.getId());
                gap.setConceptName(concept.getName());
                gap.setMappingConfidence(cluster.confidence());
                gap.setNormalizationStatus("MAPPED");
                affectedUsers.add(gap.getUserId());
                mapped++;
            }

            summaries.add(new GapNormalizationRunResponse.ConceptSummary(
                    concept.getName(),
                    concept.getStatus(),
                    group.size(),
                    distinctUsers(group),
                    distinctRoles(group),
                    group.stream().map(GapMention::getRawGapText).filter(Objects::nonNull).distinct().limit(5).toList()
            ));
        }

        for (GapMention gap : gaps) {
            if (!assignedGapIds.contains(gap.getId()) && !"FAILED".equalsIgnoreCase(safe(gap.getNormalizationStatus()))) {
                gap.setNormalizationStatus("LOW_SUPPORT");
                lowSupportGapIds.add(gap.getId());
            }
        }

        int failed = (int) gaps.stream()
                .filter(gap -> "FAILED".equalsIgnoreCase(safe(gap.getNormalizationStatus())))
                .count();
        gapMentionRepository.saveAll(gaps);
        affectedUsers.forEach(studentInsightService::rebuildStoredInsights);

        return new GapNormalizationRunResponse(
                gaps.size(),
                structured,
                mapped,
                created,
                reused,
                lowSupportGapIds.size(),
                failed,
                summaries
        );
    }

    private NormalizationPlan normalize(List<GapMention> gaps, List<CapabilityConcept> existingConcepts) {
        try {
            String response = chatClient.prompt()
                    .user(buildPrompt(gaps, existingConcepts))
                    .call()
                    .content();
            JsonNode root = objectMapper.readTree(response);
            return new NormalizationPlan(parseItems(root.path("items")), parseClusters(root.path("clusters")));
        } catch (Exception e) {
            log.warn("Gap normalization failed for {} gap mentions", gaps.size(), e);
            gaps.forEach(gap -> gap.setNormalizationStatus("FAILED"));
            return new NormalizationPlan(List.of(), List.of());
        }
    }

    private String buildPrompt(List<GapMention> gaps, List<CapabilityConcept> existingConcepts) {
        String concepts = existingConcepts.isEmpty()
                ? "No existing reusable concepts yet."
                : existingConcepts.stream()
                .map(concept -> """
                        - id: %s
                          name: %s
                          status: %s
                          description: %s
                          aliases: %s
                        """.formatted(
                        concept.getId(),
                        safe(concept.getName()),
                        safe(concept.getStatus()),
                        safe(concept.getDescription()),
                        concept.getAliases().isEmpty() ? "" : String.join(" | ", concept.getAliases())
                ))
                .collect(Collectors.joining("\n"));

        String gapItems = gaps.stream()
                .map(gap -> """
                        - id: %s
                          role: %s
                          company: %s
                          roleCategory: %s
                          rawGap: %s
                        """.formatted(
                        gap.getId(),
                        safe(gap.getJobTitle()),
                        safe(gap.getCompany()),
                        safe(gap.getRoleCategory()),
                        safe(gap.getRawGapText())
                ))
                .collect(Collectors.joining("\n"));

        return """
                Existing capability concepts:
                %s

                Unmapped gap evidence:
                %s

                Example:
                Raw gap: No experience building pricing models for German renewable-energy procurement teams
                capabilityPhrase: Pricing modeling
                domainContext: German renewable-energy procurement
                artifact: Pricing model
                evidenceType: No demonstrated project
                """.formatted(concepts, gapItems);
    }

    private int applyItemStructure(List<StructuredItem> items, Map<UUID, GapMention> gapById) {
        int structured = 0;
        for (StructuredItem item : items) {
            GapMention gap = gapById.get(item.id());
            if (gap == null) continue;
            gap.setCapabilityPhrase(item.capabilityPhrase());
            gap.setDomainContext(item.domainContext());
            gap.setArtifact(item.artifact());
            gap.setEvidenceType(item.evidenceType());
            gap.setMappingConfidence(item.confidence());
            if (present(item.capabilityPhrase())) {
                structured++;
            } else {
                gap.setNormalizationStatus("FAILED");
            }
        }
        return structured;
    }

    private ConceptResolution resolveConcept(ClusterDecision cluster,
                                             List<GapMention> group,
                                             Map<UUID, CapabilityConcept> conceptById) {
        CapabilityConcept existing = cluster.existingConceptId() != null ? conceptById.get(cluster.existingConceptId()) : null;
        if ("REUSE".equalsIgnoreCase(cluster.action())) {
            if (existing == null) return null;
            existing.getAliases().addAll(aliases(group));
            return new ConceptResolution(capabilityConceptRepository.save(existing), false, true);
        }

        String conceptName = present(cluster.conceptName()) ? cluster.conceptName() : canonicalCapabilityName(group);
        Optional<CapabilityConcept> exactMatch = capabilityConceptRepository.findByNameIgnoreCase(conceptName);
        if (exactMatch.isPresent()) {
            CapabilityConcept concept = exactMatch.get();
            concept.getAliases().addAll(aliases(group));
            return new ConceptResolution(capabilityConceptRepository.save(concept), false, true);
        }

        CapabilityConcept concept = new CapabilityConcept(conceptName, description(conceptName, group), aliases(group));
        concept.setStatus("CANDIDATE");
        return new ConceptResolution(capabilityConceptRepository.save(concept), true, false);
    }

    private List<CapabilityConcept> reusableConcepts() {
        return capabilityConceptRepository.findAll().stream()
                .filter(concept -> {
                    String status = safe(concept.getStatus());
                    return status.equalsIgnoreCase("CANDIDATE") || status.equalsIgnoreCase("APPROVED");
                })
                .toList();
    }

    private List<StructuredItem> parseItems(JsonNode itemsNode) {
        List<StructuredItem> items = new ArrayList<>();
        if (!itemsNode.isArray()) return items;
        for (JsonNode node : itemsNode) {
            UUID id = parseUuid(getText(node, "id"));
            if (id == null) continue;
            items.add(new StructuredItem(
                    id,
                    getText(node, "capabilityPhrase"),
                    getText(node, "domainContext"),
                    getText(node, "artifact"),
                    getText(node, "evidenceType"),
                    confidence(node)
            ));
        }
        return items;
    }

    private List<ClusterDecision> parseClusters(JsonNode clustersNode) {
        List<ClusterDecision> clusters = new ArrayList<>();
        if (!clustersNode.isArray()) return clusters;
        for (JsonNode node : clustersNode) {
            List<UUID> gapIds = new ArrayList<>();
            for (JsonNode idNode : node.path("gapIds")) {
                UUID id = parseUuid(idNode.asText(null));
                if (id != null) gapIds.add(id);
            }
            if (gapIds.isEmpty()) continue;
            clusters.add(new ClusterDecision(
                    safe(getText(node, "action")).toUpperCase(Locale.ROOT),
                    parseUuid(getText(node, "existingConceptId")),
                    getText(node, "conceptName"),
                    gapIds,
                    confidence(node)
            ));
        }
        return clusters;
    }

    private void markLowSupport(List<GapMention> group, Set<UUID> lowSupportGapIds) {
        group.forEach(gap -> gap.setNormalizationStatus("LOW_SUPPORT"));
        lowSupportGapIds.addAll(group.stream().map(GapMention::getId).toList());
    }

    private boolean hasSupport(List<GapMention> group) {
        return group.size() >= MIN_GAPS_FOR_CONCEPT && distinctRoles(group) >= MIN_ROLES_FOR_CONCEPT;
    }

    private String canonicalCapabilityName(List<GapMention> group) {
        return group.stream()
                .map(GapMention::getCapabilityPhrase)
                .filter(this::present)
                .collect(Collectors.groupingBy(this::normalizeKey, LinkedHashMap::new, Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> titleCase(entry.getKey()))
                .orElse("Career capability");
    }

    private String description(String conceptName, List<GapMention> group) {
        List<String> artifacts = group.stream().map(GapMention::getArtifact).filter(this::present).distinct().limit(3).toList();
        if (artifacts.isEmpty()) return "Recurring applied-role gap related to " + conceptName + ".";
        return "Recurring applied-role gap related to " + conceptName + ", often evidenced through " + String.join(", ", artifacts) + ".";
    }

    private Set<String> aliases(List<GapMention> group) {
        return group.stream()
                .map(GapMention::getRawGapText)
                .filter(this::present)
                .distinct()
                .limit(10)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private int distinctUsers(List<GapMention> group) {
        return (int) group.stream().map(GapMention::getUserId).distinct().count();
    }

    private int distinctRoles(List<GapMention> group) {
        return (int) group.stream()
                .map(gap -> normalizeKey(safe(gap.getCompany()) + "|" + safe(gap.getJobTitle())))
                .distinct()
                .count();
    }

    private String getText(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull()) return null;
        String value = field.asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Double confidence(JsonNode node) {
        double confidence = node.path("confidence").asDouble(0.7);
        if (confidence < 0) return 0.0;
        if (confidence > 1) return 1.0;
        return confidence;
    }

    private UUID parseUuid(String value) {
        if (!present(value)) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String normalizeKey(String value) {
        return safe(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private String titleCase(String value) {
        String normalized = normalizeKey(value);
        if (normalized.isBlank()) return "Career capability";
        return List.of(normalized.split(" ")).stream()
                .filter(word -> !word.isBlank())
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record NormalizationPlan(List<StructuredItem> items, List<ClusterDecision> clusters) {}
    private record StructuredItem(UUID id, String capabilityPhrase, String domainContext, String artifact,
                                  String evidenceType, Double confidence) {}
    private record ClusterDecision(String action, UUID existingConceptId, String conceptName, List<UUID> gapIds,
                                   Double confidence) {}
    private record ConceptResolution(CapabilityConcept concept, boolean created, boolean reused) {}
}
