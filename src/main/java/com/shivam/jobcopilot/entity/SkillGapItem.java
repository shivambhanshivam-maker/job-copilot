package com.shivam.jobcopilot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class SkillGapItem {

    @Column(name = "skill", columnDefinition = "TEXT")
    private String skill;

    @Column(name = "priority")
    private String priority;

    @Column(name = "action", columnDefinition = "TEXT")
    private String action;

    public SkillGapItem() {}

    public SkillGapItem(String skill, String priority, String action) {
        this.skill = skill;
        this.priority = priority;
        this.action = action;
    }

    public String getSkill() { return skill; }
    public void setSkill(String skill) { this.skill = skill; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
