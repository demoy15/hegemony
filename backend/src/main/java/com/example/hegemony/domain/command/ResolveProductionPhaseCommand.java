package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;

import java.util.List;

public record ResolveProductionPhaseCommand(String actorPlayerId, List<PurchaseItem> workerFoodPurchases) implements GameCommand {
    public ResolveProductionPhaseCommand(String actorPlayerId) {
        this(actorPlayerId, List.of());
    }

    public ResolveProductionPhaseCommand {
        workerFoodPurchases = workerFoodPurchases == null ? List.of() : List.copyOf(workerFoodPurchases);
    }

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
