package com.example.hegemony.application;

import com.example.hegemony.domain.model.BotTurnSummary;
import com.example.hegemony.domain.model.GameState;

import java.util.List;

public record BotUntilHumanExecutionResult(
        List<BotTurnSummary> turnSummaries,
        int executedSteps,
        boolean stoppedAtHumanDecisionPoint,
        boolean gameOver,
        GameState gameState
) {
}
