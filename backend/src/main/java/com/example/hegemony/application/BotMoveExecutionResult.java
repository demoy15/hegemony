package com.example.hegemony.application;

import com.example.hegemony.domain.event.DomainEvent;
import com.example.hegemony.domain.model.ActionType;
import com.example.hegemony.domain.model.GameState;

import java.util.List;

public record BotMoveExecutionResult(
        String selectedMoveId,
        ActionType actionType,
        String explanation,
        int legalActionCount,
        List<DomainEvent> events,
        GameState gameState
) {
}
