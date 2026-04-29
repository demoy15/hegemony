package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;

public record AdvanceRoundCommand(String actorPlayerId) implements GameCommand {
    @Override
    public ActionType type() {
        return ActionType.ADVANCE_ROUND;
    }

    @Override
    public String moveId() {
        return "advance-round:" + actorPlayerId;
    }

    @Override
    public String summary() {
        return "Advance to the next round";
    }
}

