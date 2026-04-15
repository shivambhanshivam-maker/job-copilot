package com.shivam.jobcopilot.dto;

import java.util.List;

public class AdjustmentStateRequest {

    private List<StateUpdate> states;

    public record StateUpdate(
            String state,         // "applied" | "dismissed"
            String cvPoint,       // original bullet text (null for "add" actions)
            String suggestedText, // what was recommended
            String adjustment     // the adjustment description
    ) {}

    public List<StateUpdate> getStates() { return states; }
    public void setStates(List<StateUpdate> states) { this.states = states; }
}