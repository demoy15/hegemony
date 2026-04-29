package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;

public record EndTurnCommand() implements GameCommand {
    @Override
    public ActionType type() {
        return ActionType.END_TURN;
    }

    @Override
    public String moveId() {
        return "end-turn";
    }

    @Override
    public String summary() {
        return "End turn";
    }
}
