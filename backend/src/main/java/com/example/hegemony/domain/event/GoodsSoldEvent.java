package com.example.hegemony.domain.event;

import com.example.hegemony.domain.model.PlayerRole;

public record GoodsSoldEvent(PlayerRole role, int amount, int revenue) implements DomainEvent {
    @Override
    public String type() {
        return "GOODS_SOLD";
    }

    @Override
    public String description() {
        return role + " sold " + amount + " goods for $" + revenue + ".";
    }
}
