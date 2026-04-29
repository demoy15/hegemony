package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;

public record AdvanceToNextRoundCommand(String actorPlayerId) implements GameCommand {
    @Override
    public ActionType type() {
        return ActionType.ADVANCE_TO_NEXT_ROUND;
    }

    @Override
    public String moveId() {
        return "advance-to-next-round:" + actorPlayerId;
    }

    @Override
    public String summary() {
        return "Advance to next round after scoring";
    }
}
