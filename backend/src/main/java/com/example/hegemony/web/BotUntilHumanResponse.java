package com.example.hegemony.web;

import com.example.hegemony.domain.model.BotTurnSummary;
import com.example.hegemony.domain.model.GameState;

import java.util.List;

public record BotUntilHumanResponse(
        List<BotTurnSummary> turnSummaries,
        int executedSteps,
        boolean stoppedAtHumanDecisionPoint,
        boolean gameOver,
        GameState gameState
) {
}
