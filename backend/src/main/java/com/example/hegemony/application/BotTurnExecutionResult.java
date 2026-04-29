package com.example.hegemony.application;

import com.example.hegemony.domain.event.DomainEvent;
import com.example.hegemony.domain.model.BotTurnSummary;
import com.example.hegemony.domain.model.GameState;

import java.util.List;

public record BotTurnExecutionResult(
        BotTurnSummary summary,
        List<DomainEvent> events,
        GameState gameState
) {
}
