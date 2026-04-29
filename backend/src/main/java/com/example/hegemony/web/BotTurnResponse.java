package com.example.hegemony.web;

import com.example.hegemony.domain.model.BotTurnSummary;
import com.example.hegemony.domain.model.GameState;

import java.util.List;

public record BotTurnResponse(
        BotTurnSummary summary,
        List<EventView> events,
        GameState gameState
) {
}
