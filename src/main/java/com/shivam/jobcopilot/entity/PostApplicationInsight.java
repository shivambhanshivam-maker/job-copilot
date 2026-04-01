package com.shivam.jobcopilot.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "post_application_insights")
public class PostApplicationInsight {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private UUID jobApplicationId;

    private UUID cvId;

    private UUID userId;

    private int fitScore;

    @Column(columnDefinition = "TEXT")
    private String interviewAngle;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pai_skill_gaps", joinColumns = @JoinColumn(name = "insight_id"))
    private List<SkillGapItem> skillGaps = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pai_talking_points", joinColumns = @JoinColumn(name = "insight_id"))
    @Column(name = "talking_point", columnDefinition = "TEXT")
    private List<String> talkingPoints = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pai_red_flags", joinColumns = @JoinColumn(name = "insight_id"))
    private List<RedFlagItem> redFlags = new ArrayList<>();

    private String jdHash;

    private LocalDateTime analyzedAt;

    public PostApplicationInsight() {}

    @PrePersist
    protected void onCreate() {
        this.analyzedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }

    public UUID getJobApplicationId() { return jobApplicationId; }
    public void setJobApplicationId(UUID jobApplicationId) { this.jobApplicationId = jobApplicationId; }

    public UUID getCvId() { return cvId; }
    public void setCvId(UUID cvId) { this.cvId = cvId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public int getFitScore() { return fitScore; }
    public void setFitScore(int fitScore) { this.fitScore = fitScore; }

    public String getInterviewAngle() { return interviewAngle; }
    public void setInterviewAngle(String interviewAngle) { this.interviewAngle = interviewAngle; }

    public List<SkillGapItem> getSkillGaps() { return skillGaps; }
    public void setSkillGaps(List<SkillGapItem> skillGaps) { this.skillGaps = skillGaps; }

    public List<String> getTalkingPoints() { return talkingPoints; }
    public void setTalkingPoints(List<String> talkingPoints) { this.talkingPoints = talkingPoints; }

    public List<RedFlagItem> getRedFlags() { return redFlags; }
    public void setRedFlags(List<RedFlagItem> redFlags) { this.redFlags = redFlags; }

    public String getJdHash() { return jdHash; }
    public void setJdHash(String jdHash) { this.jdHash = jdHash; }

    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
}
