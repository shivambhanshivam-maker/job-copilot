package com.shivam.jobcopilot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class CvAdjustmentItem {

    @Column(name = "adjustment", columnDefinition = "TEXT")
    private String adjustment;

    @Column(name = "priority")
    private String priority;

    @Column(name = "addresses_gap", columnDefinition = "TEXT")
    private String addressesGap;

    @Column(name = "action")
    private String action;

    @Column(name = "cv_point", columnDefinition = "TEXT")
    private String cvPoint;

    @Column(name = "suggested_text", columnDefinition = "TEXT")
    private String suggestedText;

    public CvAdjustmentItem() {}

    public CvAdjustmentItem(String adjustment, String priority, String addressesGap, String action, String cvPoint, String suggestedText) {
        this.adjustment = adjustment;
        this.priority = priority;
        this.addressesGap = addressesGap;
        this.action = action;
        this.cvPoint = cvPoint;
        this.suggestedText = suggestedText;
    }

    public String getAdjustment() { return adjustment; }
    public void setAdjustment(String adjustment) { this.adjustment = adjustment; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getAddressesGap() { return addressesGap; }
    public void setAddressesGap(String addressesGap) { this.addressesGap = addressesGap; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getCvPoint() { return cvPoint; }
    public void setCvPoint(String cvPoint) { this.cvPoint = cvPoint; }

    public String getSuggestedText() { return suggestedText; }
    public void setSuggestedText(String suggestedText) { this.suggestedText = suggestedText; }
}