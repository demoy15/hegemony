package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;

public record SellGoodsCommand(int amount) implements GameCommand {
    @Override
    public ActionType type() {
        return ActionType.SELL_GOODS;
    }

    @Override
    public String moveId() {
        return "sell-goods:" + amount;
    }

    @Override
    public String summary() {
        return "Sell " + amount + " goods";
    }
}
