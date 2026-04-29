package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;

public record AdvanceToScoringCommand(String actorPlayerId) implements GameCommand {
    @Override
    public ActionType type() {
        return ActionType.ADVANCE_TO_SCORING;
    }

    @Override
    public String moveId() {
        return "advance-to-scoring:" + actorPlayerId;
    }

    @Override
    public String summary() {
        return "Advance from PRODUCTION to SCORING phase";
    }
}
