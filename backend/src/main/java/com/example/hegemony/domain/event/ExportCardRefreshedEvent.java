package com.example.hegemony.domain.event;

public record ExportCardRefreshedEvent(
        String cardId,
        int round,
        int availableOperations
) implements DomainEvent {
    @Override
    public String type() {
        return "EXPORT_CARD_REFRESHED";
    }

    @Override
    public String description() {
        return "Export card refreshed for round " + round + ": " + cardId + " with " + availableOperations + " operations.";
    }
}
