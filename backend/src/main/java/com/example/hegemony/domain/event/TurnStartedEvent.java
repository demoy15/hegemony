package com.example.hegemony.domain.event;

import com.example.hegemony.domain.model.PlayerRole;

public record TurnStartedEvent(PlayerRole role, int round) implements DomainEvent {
    @Override
    public String type() {
        return "TURN_STARTED";
    }

    @Override
    public String description() {
        return role + " starts round " + round + ".";
    }
}
