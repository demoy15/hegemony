package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;

public record AdvanceToProductionCommand(String actorPlayerId) implements GameCommand {
    @Override
    public ActionType type() {
        return ActionType.ADVANCE_TO_PRODUCTION;
    }

    @Override
    public String moveId() {
        return "advance-to-production:" + actorPlayerId;
    }

    @Override
    public String summary() {
        return "Advance to production phase";
    }
}

