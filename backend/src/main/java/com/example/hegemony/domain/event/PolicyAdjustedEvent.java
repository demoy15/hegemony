package com.example.hegemony.domain.event;

import com.example.hegemony.domain.model.PolicyTrack;

public record PolicyAdjustedEvent(PolicyTrack track, int before, int after) implements DomainEvent {
    @Override
    public String type() {
        return "POLICY_ADJUSTED";
    }

    @Override
    public String description() {
        return "Policy " + track + " changed from " + before + " to " + after + ".";
    }
}
