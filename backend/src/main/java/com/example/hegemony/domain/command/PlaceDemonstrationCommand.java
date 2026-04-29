package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;

import java.util.Map;

public record PlaceDemonstrationCommand(String actorPlayerId, Map<String, Integer> penaltyAllocation) implements GameCommand {
    @Override
    public ActionType type() {
        return ActionType.PLACE_DEMONSTRATION;
    }

    @Override
    public String moveId() {
        return "place-demonstration:" + actorPlayerId;
    }

    @Override
    public String summary() {
        return "Place a demonstration token if worker unemployment exceeds available vacancies by at least 2.";
    }
}
