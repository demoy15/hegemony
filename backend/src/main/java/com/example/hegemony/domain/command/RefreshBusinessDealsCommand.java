package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;

public record RefreshBusinessDealsCommand(String actorPlayerId) implements GameCommand {
    @Override
    public ActionType type() {
        return ActionType.REFRESH_BUSINESS_DEALS;
    }

    @Override
    public String moveId() {
        return "refresh-business-deals:" + actorPlayerId;
    }

    @Override
    public String summary() {
        return "Refresh visible business deals from ordered deck";
    }
}
