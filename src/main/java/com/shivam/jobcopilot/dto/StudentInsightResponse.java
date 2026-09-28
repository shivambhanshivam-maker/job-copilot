package com.shivam.jobcopilot.dto;

import com.shivam.jobcopilot.entity.StudentInsight;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record StudentInsightResponse(PrimaryFocus primaryFocus, List<Item> insights) {
    public record PrimaryFocus(String type, String title, String action, String reason) {
        public static PrimaryFocus from(com.shivam.jobcopilot.service.StudentInsightService.PrimaryFocus focus) {
            return new PrimaryFocus(focus.type(), focus.title(), focus.action(), focus.reason());
        }
    }

    public record Item(
            UUID id,
            String insightType,
            String severity,
            String confidence,
            String title,
            String summary,
            String recommendation,
            String evidenceJson,
            LocalDateTime generatedAt
    ) {
        public static Item from(StudentInsight insight) {
            return new Item(
                    insight.getId(),
                    insight.getInsightType(),
                    insight.getSeverity(),
                    insight.getConfidence(),
                    insight.getTitle(),
                    insight.getSummary(),
                    insight.getRecommendation(),
                    insight.getEvidenceJson(),
                    insight.getGeneratedAt()
            );
        }
    }
}
