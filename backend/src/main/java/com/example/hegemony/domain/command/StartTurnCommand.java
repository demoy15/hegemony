package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;

public record StartTurnCommand() implements GameCommand {
    @Override
    public ActionType type() {
        return ActionType.START_TURN;
    }

    @Override
    public String moveId() {
        return "start-turn";
    }

    @Override
    public String summary() {
        return "Start current player's turn";
    }
}
