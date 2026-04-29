package com.example.hegemony.domain.event;

import com.example.hegemony.domain.model.PlayerRole;

public record GoodsProducedEvent(PlayerRole role, int amount) implements DomainEvent {
    @Override
    public String type() {
        return "GOODS_PRODUCED";
    }

    @Override
    public String description() {
        return role + " produced " + amount + " goods.";
    }
}
