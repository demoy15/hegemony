package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;

import java.util.List;

public record PlaceStrikesCommand(String actorPlayerId, List<String> enterpriseIds) implements GameCommand {
    @Override
    public ActionType type() {
        return ActionType.PLACE_STRIKES;
    }

    @Override
    public String moveId() {
        return "place-strikes:" + actorPlayerId + ":" + String.join(",", enterpriseIds == null ? List.of() : enterpriseIds);
    }

    @Override
    public String summary() {
        return "Place strike tokens on eligible enterprises employing worker-class workers.";
    }
}
