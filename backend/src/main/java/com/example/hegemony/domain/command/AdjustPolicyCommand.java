package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;
import com.example.hegemony.domain.model.PolicyTrack;

public record AdjustPolicyCommand(PolicyTrack track, int delta) implements GameCommand {
    @Override
    public ActionType type() {
        return ActionType.ADJUST_POLICY;
    }

    @Override
    public String moveId() {
        return "adjust-policy:" + track + ":" + delta;
    }

    @Override
    public String summary() {
        return "Adjust " + track + " by " + delta;
    }
}
