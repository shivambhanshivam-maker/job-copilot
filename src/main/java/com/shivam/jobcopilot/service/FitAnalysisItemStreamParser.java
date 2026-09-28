package com.shivam.jobcopilot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shivam.jobcopilot.dto.FitAnalysisResponse;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Emits fit-analysis items as soon as their JSON values are complete.
 * The original model JSON remains buffered separately for persistence.
 */
public final class FitAnalysisItemStreamParser {

    public record StreamEvent(String type, Map<String, Object> data) {}

    private static final TypeReference<List<FitAnalysisResponse.JdRequirement>> REQUIREMENTS = new TypeReference<>() {};
    private static final TypeReference<List<FitAnalysisResponse.StrengthItem>> STRENGTHS = new TypeReference<>() {};
    private static final TypeReference<List<String>> DIFFERENTIATION = new TypeReference<>() {};
    private static final TypeReference<List<FitAnalysisResponse.GapItem>> GAPS = new TypeReference<>() {};
    private static final TypeReference<List<FitAnalysisResponse.CvAdjustmentItem>> ADJUSTMENTS = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final FitAnalysisService fitAnalysisService;
    private final String cvText;
    private final StringBuilder json = new StringBuilder();
    private final Map<String, Boolean> emittedFields = new HashMap<>();
    private int emittedStrengthCount;
    private int emittedDifferentiationCount;
    private int emittedGapCount;
    private int emittedAdjustmentCount;
    private FitAnalysisService.ScorePreview preview;
    private boolean scoreEmitted;
    private boolean breakdownComplete;

    public FitAnalysisItemStreamParser(ObjectMapper objectMapper,
                                       FitAnalysisService fitAnalysisService,
                                       String cvText) {
        this.objectMapper = objectMapper;
        this.fitAnalysisService = fitAnalysisService;
        this.cvText = cvText;
    }

    public List<StreamEvent> accept(String chunk) {
        if (chunk == null || chunk.isEmpty()) return List.of();
        json.append(chunk);
        return drainCompletedSections();
    }

    private List<StreamEvent> drainCompletedSections() {
        List<StreamEvent> events = new ArrayList<>();

        if (!scoreEmitted) {
            List<FitAnalysisResponse.JdRequirement> requirements = readArray("jdRequirements", REQUIREMENTS);
            if (requirements != null) {
                preview = fitAnalysisService.previewScore(requirements, cvText);
                FitScoringService.ScoringResult score = preview.score();
                events.add(event("score", Map.of(
                        "fitScore", score.fitScore(),
                        "recommendation", score.recommendation(),
                        "weightageReasoning", score.weightageReasoning()
                )));
                scoreEmitted = true;
            }
        }

        // Later sections are held until the authoritative score is ready.
        if (!scoreEmitted) return events;

        // Arrays are intentionally published as growing snapshots. Waiting for the closing ]
        // made the UI look like it was rendering one large section at a time.
        List<FitAnalysisResponse.StrengthItem> strengths = readCompletedArrayItems("strengthAlignment", STRENGTHS);
        List<String> differentiation = readCompletedArrayItems("differentiation", DIFFERENTIATION);
        boolean strengthsComplete = isArrayComplete("strengthAlignment");
        boolean differentiationComplete = isArrayComplete("differentiation");
        if (strengths != null && (strengths.size() > emittedStrengthCount
                || (differentiation != null && differentiation.size() > emittedDifferentiationCount)
                || (strengthsComplete && differentiationComplete && !hasEmitted("strengths")))) {
            events.add(event("strengths", Map.of(
                    "strengths", strengths,
                    "differentiation", differentiation == null ? List.of() : differentiation
            )));
            emittedStrengthCount = strengths.size();
            emittedDifferentiationCount = differentiation == null ? 0 : differentiation.size();
            if (strengthsComplete && differentiationComplete) markEmitted("strengths");
        }

        boolean gapsComplete = isArrayComplete("gaps");
        List<FitAnalysisResponse.GapItem> gaps = readCompletedArrayItems("gaps", GAPS);
        if (gaps != null && strengthsComplete && differentiationComplete
                && (gaps.size() > emittedGapCount || (gapsComplete && !hasEmitted("gaps")))) {
            events.add(event("gaps", Map.of("gaps", gaps)));
            emittedGapCount = gaps.size();
            if (gapsComplete) markEmitted("gaps");
        }

        if (!breakdownComplete && preview != null && hasEmitted("gaps")) {
            var requirements = preview.requirements();
            List<com.shivam.jobcopilot.entity.RequirementEvidence> evidence = preview.evidence();
            List<Map<String, Object>> items = new ArrayList<>();
            for (int index = 0; index < requirements.size(); index++) {
                items.add(Map.of(
                        "requirement", requirements.get(index),
                        "evidence", evidence.get(index)
                ));
            }
            events.add(event("breakdown", Map.of("items", items)));
            breakdownComplete = true;
        }

        if (breakdownComplete) {
            if (!hasEmitted("positioning")) {
                String positioning = readString("positioningAngle");
                if (positioning != null) {
                    events.add(event("positioning", Map.of("positioningAngle", positioning)));
                    markEmitted("positioning");
                }
            }

            // Adjustments are also published as a growing snapshot, after positioning.
            List<FitAnalysisResponse.CvAdjustmentItem> adjustments = readCompletedArrayItems("cvAdjustments", ADJUSTMENTS);
            boolean adjustmentsComplete = isArrayComplete("cvAdjustments");
            if (hasEmitted("positioning") && adjustments != null
                    && (adjustments.size() > emittedAdjustmentCount
                    || (adjustmentsComplete && !hasEmitted("adjustments")))) {
                events.add(event("adjustments", Map.of("adjustments", adjustments)));
                emittedAdjustmentCount = adjustments.size();
                if (adjustmentsComplete) markEmitted("adjustments");
            }
        }

        return events;
    }

    private <T> List<T> readArray(String fieldName, TypeReference<List<T>> type) {
        String value = findTopLevelFieldValue(fieldName);
        if (value == null || !value.startsWith("[")) return null;
        try {
            return objectMapper.readValue(value, type);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Reads only complete elements from an array that may still be receiving JSON.
     * The returned list is a valid JSON snapshot, so the client never sees malformed data.
     */
    private <T> List<T> readCompletedArrayItems(String fieldName, TypeReference<List<T>> type) {
        int arrayStart = findTopLevelFieldStart(fieldName);
        if (arrayStart < 0 || arrayStart >= json.length() || json.charAt(arrayStart) != '[') return null;

        String source = json.toString();
        List<String> completedItems = new ArrayList<>();
        int cursor = skipWhitespace(source, arrayStart + 1);
        while (cursor < source.length()) {
            if (source.charAt(cursor) == ']') break;
            if (source.charAt(cursor) == ',') {
                cursor = skipWhitespace(source, cursor + 1);
                continue;
            }
            int itemEnd = findJsonValueEnd(source, cursor);
            if (itemEnd < 0) break;
            completedItems.add(source.substring(cursor, itemEnd + 1));
            cursor = skipWhitespace(source, itemEnd + 1);
            if (cursor < source.length() && source.charAt(cursor) == ',') {
                cursor = skipWhitespace(source, cursor + 1);
            } else if (cursor >= source.length() || source.charAt(cursor) != ']') {
                break;
            }
        }

        if (completedItems.isEmpty()) {
            return cursor < source.length() && source.charAt(cursor) == ']' ? List.of() : null;
        }
        try {
            return objectMapper.readValue("[" + String.join(",", completedItems) + "]", type);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String readString(String fieldName) {
        String value = findTopLevelFieldValue(fieldName);
        if (value == null || !value.startsWith("\"")) return null;
        try {
            return objectMapper.readValue(value, String.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean hasStartedArray(String fieldName) {
        int start = findTopLevelFieldStart(fieldName);
        return start >= 0 && start < json.length() && json.charAt(start) == '[';
    }

    private boolean isArrayComplete(String fieldName) {
        String value = findTopLevelFieldValue(fieldName);
        return value != null && value.startsWith("[");
    }

    private boolean hasEmitted(String field) {
        return emittedFields.getOrDefault(field, false);
    }

    private void markEmitted(String field) {
        emittedFields.put(field, true);
    }

    private StreamEvent event(String type, Map<String, Object> data) {
        return new StreamEvent(type, new LinkedHashMap<>(data));
    }

    /** Finds only top-level properties, so a matching word inside an excerpt cannot trigger an event. */
    private int findTopLevelFieldStart(String fieldName) {
        String source = json.toString();
        int objectStart = source.indexOf('{');
        if (objectStart < 0) return -1;

        int cursor = objectStart + 1;
        while (cursor < source.length()) {
            cursor = skipWhitespace(source, cursor);
            if (cursor >= source.length() || source.charAt(cursor) == '}') return -1;
            if (source.charAt(cursor) != '"') return -1;

            int keyEnd = findStringEnd(source, cursor);
            if (keyEnd < 0) return -1;
            String key;
            try {
                key = objectMapper.readValue(source.substring(cursor, keyEnd + 1), String.class);
            } catch (Exception ignored) {
                return -1;
            }

            cursor = skipWhitespace(source, keyEnd + 1);
            if (cursor >= source.length() || source.charAt(cursor) != ':') return -1;
            int valueStart = skipWhitespace(source, cursor + 1);
            if (fieldName.equals(key)) return valueStart;

            int valueEnd = findJsonValueEnd(source, valueStart);
            if (valueEnd < 0) return -1;
            cursor = skipWhitespace(source, valueEnd + 1);
            if (cursor < source.length() && source.charAt(cursor) == ',') cursor++;
        }
        return -1;
    }

    private String findTopLevelFieldValue(String fieldName) {
        int valueStart = findTopLevelFieldStart(fieldName);
        if (valueStart < 0) return null;
        int valueEnd = findJsonValueEnd(json.toString(), valueStart);
        return valueEnd < 0 ? null : json.substring(valueStart, valueEnd + 1);
    }

    private int findJsonValueEnd(String source, int start) {
        if (start >= source.length()) return -1;
        char first = source.charAt(start);
        if (first == '"') return findStringEnd(source, start);

        if (first == '[' || first == '{') {
            ArrayDeque<Character> expectedClosers = new ArrayDeque<>();
            expectedClosers.push(first == '[' ? ']' : '}');
            boolean inString = false;
            boolean escaped = false;
            for (int i = start + 1; i < source.length(); i++) {
                char current = source.charAt(i);
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (inString) {
                    if (current == '\\') escaped = true;
                    else if (current == '"') inString = false;
                    continue;
                }
                if (current == '"') {
                    inString = true;
                } else if (current == '[') {
                    expectedClosers.push(']');
                } else if (current == '{') {
                    expectedClosers.push('}');
                } else if (current == ']' || current == '}') {
                    if (expectedClosers.isEmpty() || expectedClosers.pop() != current) return -1;
                    if (expectedClosers.isEmpty()) return i;
                }
            }
            return -1;
        }

        for (int i = start; i < source.length(); i++) {
            char current = source.charAt(i);
            if (current == ',' || current == '}') return skipWhitespaceBackwards(source, i - 1);
        }
        return -1;
    }

    private int findStringEnd(String source, int start) {
        boolean escaped = false;
        for (int i = start + 1; i < source.length(); i++) {
            char current = source.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else if (current == '"') {
                return i;
            }
        }
        return -1;
    }

    private int skipWhitespace(String source, int cursor) {
        while (cursor < source.length() && Character.isWhitespace(source.charAt(cursor))) cursor++;
        return cursor;
    }

    private int skipWhitespaceBackwards(String source, int cursor) {
        while (cursor >= 0 && Character.isWhitespace(source.charAt(cursor))) cursor--;
        return cursor;
    }
}
