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

    public CvAdjustmentItem() {}

    public CvAdjustmentItem(String adjustment, String priority, String addressesGap) {
        this.adjustment = adjustment;
        this.priority = priority;
        this.addressesGap = addressesGap;
    }

    public String getAdjustment() { return adjustment; }
    public void setAdjustment(String adjustment) { this.adjustment = adjustment; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getAddressesGap() { return addressesGap; }
    public void setAddressesGap(String addressesGap) { this.addressesGap = addressesGap; }
}
