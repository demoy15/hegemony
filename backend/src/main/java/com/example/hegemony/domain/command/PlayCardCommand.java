package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;

public record PlayCardCommand(String cardId) implements GameCommand {
    @Override
    public ActionType type() {
        return ActionType.PLAY_CARD;
    }

    @Override
    public String moveId() {
        return "play-card:" + cardId;
    }

    @Override
    public String summary() {
        return "Play card " + cardId;
    }
}
