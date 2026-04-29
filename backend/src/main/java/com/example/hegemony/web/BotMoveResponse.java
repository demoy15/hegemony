package com.example.hegemony.web;

import com.example.hegemony.domain.model.ActionType;
import com.example.hegemony.domain.model.GameState;

import java.util.List;

public record BotMoveResponse(
        String selectedMoveId,
        ActionType actionType,
        String explanation,
        int legalActionCount,
        List<EventView> events,
        GameState gameState
) {
}
