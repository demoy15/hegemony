package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;

public record ResolveProductionPhaseCommand(String actorPlayerId) implements GameCommand {
    @Override
    public ActionType type() {
        return ActionType.RESOLVE_PRODUCTION_PHASE;
    }

    @Override
    public String moveId() {
        return "resolve-production-phase:" + actorPlayerId;
    }

    @Override
    public String summary() {
        return "Resolve production phase economic loop";
    }
}

