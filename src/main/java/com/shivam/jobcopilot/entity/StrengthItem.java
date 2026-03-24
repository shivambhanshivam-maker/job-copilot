package com.shivam.jobcopilot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class StrengthItem {

    @Column(name = "strength", columnDefinition = "TEXT")
    private String strength;

    @Column(name = "category")
    private String category;

    public StrengthItem() {}

    public StrengthItem(String strength, String category) {
        this.strength = strength;
        this.category = category;
    }

    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
