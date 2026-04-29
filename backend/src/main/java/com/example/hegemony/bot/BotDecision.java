package com.example.hegemony.bot;

import com.example.hegemony.domain.command.GameCommand;
import com.example.hegemony.domain.event.DomainEvent;

import java.util.List;

public record BotDecision(
        GameCommand selectedCommand,
        String explanation,
        int legalActionCount,
        List<DomainEvent> projectedEvents
) {
}
