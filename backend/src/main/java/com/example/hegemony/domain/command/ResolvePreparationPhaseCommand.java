package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;

public record ResolvePreparationPhaseCommand(String actorPlayerId) implements GameCommand {
    @Override
    public ActionType type() {
        return ActionType.RESOLVE_PREPARATION_PHASE;
    }

    @Override
    public String moveId() {
        return "resolve-preparation-phase:" + actorPlayerId;
    }

    @Override
    public String summary() {
        return "Resolve supported preparation steps";
    }
}
