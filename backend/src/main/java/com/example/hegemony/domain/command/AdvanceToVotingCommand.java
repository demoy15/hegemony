package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;

public record AdvanceToVotingCommand(String actorPlayerId) implements GameCommand {
    @Override
    public ActionType type() {
        return ActionType.ADVANCE_TO_VOTING;
    }

    @Override
    public String moveId() {
        return "advance-to-voting:" + actorPlayerId;
    }

    @Override
    public String summary() {
        return "Advance from ACTIONS to VOTING phase";
    }
}
