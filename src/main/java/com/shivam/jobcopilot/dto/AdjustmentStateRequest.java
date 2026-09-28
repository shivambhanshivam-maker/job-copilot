package com.shivam.jobcopilot.dto;

import java.util.List;

public class AdjustmentStateRequest {

    private List<StateUpdate> states;
    private String cvId;
    private String jobDescription;
    private String jobTitle;
    private String companyName;
    private String roleCategory;

    public record StateUpdate(
            String state,         // "applied" | "dismissed"
            String cvPoint,       // original bullet text (null for "add" actions)
            String suggestedText, // what was recommended
            String adjustment     // the adjustment description
    ) {}

    public List<StateUpdate> getStates() { return states; }
    public void setStates(List<StateUpdate> states) { this.states = states; }

    public String getCvId() { return cvId; }
    public void setCvId(String cvId) { this.cvId = cvId; }

    public String getJobDescription() { return jobDescription; }
    public void setJobDescription(String jobDescription) { this.jobDescription = jobDescription; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getRoleCategory() { return roleCategory; }
    public void setRoleCategory(String roleCategory) { this.roleCategory = roleCategory; }
}
