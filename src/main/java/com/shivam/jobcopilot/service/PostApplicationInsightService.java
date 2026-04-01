package com.shivam.jobcopilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shivam.jobcopilot.dto.PostApplicationInsightResponse;
import com.shivam.jobcopilot.entity.PostApplicationInsight;
import com.shivam.jobcopilot.entity.RedFlagItem;
import com.shivam.jobcopilot.entity.SkillGapItem;
import com.shivam.jobcopilot.repository.PostApplicationInsightRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PostApplicationInsightService {

    private static final Logger log = LoggerFactory.getLogger(PostApplicationInsightService.class);

    private final PostApplicationInsightRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PostApplicationInsightService(PostApplicationInsightRepository repository) {
        this.repository = repository;
    }

    public Optional<PostApplicationInsight> findByApplicationId(UUID jobApplicationId) {
        return repository.findByJobApplicationId(jobApplicationId);
    }

    public Map<UUID, PostApplicationInsight> findAllByApplicationIds(Collection<UUID> appIds) {
        return repository.findAllByJobApplicationIdIn(appIds).stream()
                .collect(Collectors.toMap(PostApplicationInsight::getJobApplicationId, i -> i));
    }

    public boolean isStale(PostApplicationInsight insight, UUID currentCvId, String currentJdText) {
        boolean cvChanged = !insight.getCvId().equals(currentCvId);
        boolean jdChanged = !insight.getJdHash().equals(hash(currentJdText));
        return cvChanged || jdChanged;
    }

    // Called by the controller's doOnComplete — parses the fully assembled JSON then persists.
    // Replaces any existing insight for this application (re-run overwrites).
    public Optional<PostApplicationInsight> persistFromJson(String json, UUID jobApplicationId,
                                                            UUID cvId, String jobDescriptionText, UUID userId) {
        try {
            PostApplicationInsightResponse parsed = objectMapper.readValue(json, PostApplicationInsightResponse.class);
            return Optional.of(save(parsed, jobApplicationId, cvId, jobDescriptionText, userId));
        } catch (Exception e) {
            log.error("Failed to persist post-application insight after stream completed", e);
            return Optional.empty();
        }
    }

    @Transactional
    public PostApplicationInsight save(PostApplicationInsightResponse response, UUID jobApplicationId,
                                       UUID cvId, String jobDescriptionText, UUID userId) {
        // Delete any previous insight for this application before saving the new one
        repository.findByJobApplicationId(jobApplicationId)
                .ifPresent(existing -> repository.deleteById(existing.getId()));

        PostApplicationInsight insight = new PostApplicationInsight();
        insight.setJobApplicationId(jobApplicationId);
        insight.setCvId(cvId);
        insight.setJdHash(hash(jobDescriptionText));
        insight.setUserId(userId);
        insight.setFitScore(response.getFitScore());
        insight.setInterviewAngle(response.getInterviewAngle());

        if (response.getSkillGaps() != null) {
            List<SkillGapItem> gaps = response.getSkillGaps().stream()
                    .map(g -> new SkillGapItem(g.skill(), g.priority(), g.action()))
                    .toList();
            insight.setSkillGaps(gaps);
        }

        if (response.getTalkingPoints() != null) {
            insight.setTalkingPoints(response.getTalkingPoints());
        }

        if (response.getRedFlags() != null) {
            List<RedFlagItem> flags = response.getRedFlags().stream()
                    .map(f -> new RedFlagItem(f.gap(), f.tip()))
                    .toList();
            insight.setRedFlags(flags);
        }

        return repository.save(insight);
    }

    static String hash(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(text.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
