package com.shivam.jobcopilot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class RedFlagItem {

    @Column(name = "gap", columnDefinition = "TEXT")
    private String gap;

    @Column(name = "tip", columnDefinition = "TEXT")
    private String tip;

    public RedFlagItem() {}

    public RedFlagItem(String gap, String tip) {
        this.gap = gap;
        this.tip = tip;
    }

    public String getGap() { return gap; }
    public void setGap(String gap) { this.gap = gap; }

    public String getTip() { return tip; }
    public void setTip(String tip) { this.tip = tip; }
}
