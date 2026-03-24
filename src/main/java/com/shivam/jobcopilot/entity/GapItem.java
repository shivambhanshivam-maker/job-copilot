package com.shivam.jobcopilot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class GapItem {

    @Column(name = "gap", columnDefinition = "TEXT")
    private String gap;

    @Column(name = "category")
    private String category;

    @Column(name = "severity")
    private String severity;

    public GapItem() {}

    public GapItem(String gap, String category, String severity) {
        this.gap = gap;
        this.category = category;
        this.severity = severity;
    }

    public String getGap() { return gap; }
    public void setGap(String gap) { this.gap = gap; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
}
