package com.example.hegemony.domain.event;

import com.example.hegemony.domain.model.PlayerRole;

public record CardPlayedEvent(
        PlayerRole role,
        String cardId,
        String cardName,
        int moneyDelta,
        int goodsDelta,
        int taxationDelta
) implements DomainEvent {
    @Override
    public String type() {
        return "CARD_PLAYED";
    }

    @Override
    public String description() {
        return role + " played " + cardName + " (" + cardId + ").";
    }
}
