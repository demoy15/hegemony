package com.example.hegemony.domain.event;

import java.util.List;

public record MigrationResolvedEvent(
        int round,
        List<String> summaries
) implements DomainEvent {
    @Override
    public String type() {
        return "MIGRATION_RESOLVED";
    }

    @Override
    public String description() {
        return "Migration resolved for round " + round + ": " + String.join("; ", summaries) + ".";
    }
}
