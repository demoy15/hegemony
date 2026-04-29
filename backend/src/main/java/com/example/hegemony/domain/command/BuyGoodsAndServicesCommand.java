package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;

import java.util.List;

public record BuyGoodsAndServicesCommand(
        String actorPlayerId,
        String resourceType,
        List<PurchaseItem> purchases
) implements GameCommand {
    public BuyGoodsAndServicesCommand {
        purchases = purchases == null ? List.of() : List.copyOf(purchases);
    }

    @Override
    public ActionType type() {
        return ActionType.BUY_GOODS_AND_SERVICES;
    }

    @Override
    public String moveId() {
        return "buy-goods-and-services:" + actorPlayerId + ":" + resourceType + ":" + purchases.size();
    }

    @Override
    public String summary() {
        return "Buy " + resourceType + " from " + purchases.size() + " supplier(s)";
    }
}
