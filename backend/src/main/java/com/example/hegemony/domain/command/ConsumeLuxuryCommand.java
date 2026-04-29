package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;

public record ConsumeLuxuryCommand(String actorPlayerId) implements GameCommand {
    @Override
    public ActionType type() {
        return ActionType.CONSUME_LUXURY;
    }

    @Override
    public String moveId() {
        return "consume-luxury:" + actorPlayerId;
    }

    @Override
    public String summary() {
        return "Consume luxury for current population";
    }
}
