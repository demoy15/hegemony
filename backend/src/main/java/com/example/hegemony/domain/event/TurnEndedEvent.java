package com.example.hegemony.domain.event;

import com.example.hegemony.domain.model.PlayerRole;

public record TurnEndedEvent(
        PlayerRole endedBy,
        PlayerRole nextPlayer,
        int nextPlayerIndex,
        int nextRound
) implements DomainEvent {
    @Override
    public String type() {
        return "TURN_ENDED";
    }

    @Override
    public String description() {
        return endedBy + " ended turn. Next player: " + nextPlayer + " (round " + nextRound + ").";
    }
}
