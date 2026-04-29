package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;

public record ConsumeHealthcareCommand(String actorPlayerId) implements GameCommand {
    @Override
    public ActionType type() {
        return ActionType.CONSUME_HEALTHCARE;
    }

    @Override
    public String moveId() {
        return "consume-healthcare:" + actorPlayerId;
    }

    @Override
    public String summary() {
        return "Consume healthcare for current population";
    }
}
