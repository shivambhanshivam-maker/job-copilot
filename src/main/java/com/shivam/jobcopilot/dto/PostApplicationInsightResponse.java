package com.shivam.jobcopilot.dto;

import java.util.List;

public class PostApplicationInsightResponse {

    private int fitScore;
    private String interviewAngle;
    private List<SkillGapItem> skillGaps;
    private List<String> talkingPoints;
    private List<RedFlagItem> redFlags;

    public record SkillGapItem(String skill, String priority, String action) {}
    public record RedFlagItem(String gap, String tip) {}

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
}
