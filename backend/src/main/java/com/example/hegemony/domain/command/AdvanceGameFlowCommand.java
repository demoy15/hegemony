package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;

public record AdvanceGameFlowCommand(String actorPlayerId) implements GameCommand {
    @Override
    public ActionType type() {
        return ActionType.ADVANCE_GAME_FLOW;
    }

    @Override
    public String moveId() {
        return "advance-game-flow:" + actorPlayerId;
    }

    @Override
    public String summary() {
        return "Advance one safe lifecycle step";
    }
}
