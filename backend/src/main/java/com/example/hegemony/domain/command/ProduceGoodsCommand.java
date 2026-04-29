package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;

public record ProduceGoodsCommand(int amount) implements GameCommand {
    @Override
    public ActionType type() {
        return ActionType.PRODUCE_GOODS;
    }

    @Override
    public String moveId() {
        return "produce-goods:" + amount;
    }

    @Override
    public String summary() {
        return "Produce " + amount + " goods";
    }
}
