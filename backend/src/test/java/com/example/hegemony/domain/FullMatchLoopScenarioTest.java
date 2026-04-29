package com.example.hegemony.domain;

import com.example.hegemony.bot.LegalMoveBot;
import com.example.hegemony.domain.engine.ApplyCommandResult;
import com.example.hegemony.domain.engine.GameRulesEngine;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.GameStatus;
import com.example.hegemony.domain.model.RoundPhase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FullMatchLoopScenarioTest {
    @Test
    void fullFiveRoundFlow_reachesGameOver() {
        RunResult run = runBotMatch();

        assertThat(run.steps).isLessThan(500);
        assertThat(run.state.getCurrentRound()).isEqualTo(5);
        assertThat(run.state.getCurrentPhase()).isEqualTo(RoundPhase.GAME_OVER);
        assertThat(run.state.getGameStatus()).isEqualTo(GameStatus.FINISHED);
    }

    @Test
    void botCanProgressWholeMatchLoopLegally() {
        RunResult run = runBotMatch();
        assertThat(run.illegalSelections).isZero();
        assertThat(run.invalidApplies).isZero();
    }

    @Test
    void stateRemainsConsistentAcrossAllRounds() {
        RunResult run = runBotMatch();
        assertThat(run.invalidRoundStates).isZero();
        assertThat(run.invalidPhaseStates).isZero();
        assertThat(run.state.getRoundMarker()).isEqualTo(run.state.getCurrentRound());
    }

    private RunResult runBotMatch() {
        GameRulesEngine engine = CoreTestSupport.engine();
        LegalMoveBot bot = new LegalMoveBot();
        GameState state = CoreTestSupport.state(2);

        int steps = 0;
        int illegalSelections = 0;
        int invalidApplies = 0;
        int invalidRoundStates = 0;
        int invalidPhaseStates = 0;

        while (!state.isGameOver() && steps++ < 500) {
            var decision = bot.chooseMove(state, engine);
            var validation = engine.validate(state, decision.selectedCommand());
            if (!validation.isValid()) {
                illegalSelections++;
                break;
            }

            ApplyCommandResult applied = engine.apply(state, decision.selectedCommand());
            if (!applied.validation().isValid()) {
                invalidApplies++;
                break;
            }

            state = applied.resultingState();

            if (state.getCurrentRound() < 1 || state.getCurrentRound() > 5) {
                invalidRoundStates++;
            }
            if (state.getCurrentPhase() == null) {
                invalidPhaseStates++;
            }
        }

        return new RunResult(state, steps, illegalSelections, invalidApplies, invalidRoundStates, invalidPhaseStates);
    }

    private record RunResult(
            GameState state,
            int steps,
            int illegalSelections,
            int invalidApplies,
            int invalidRoundStates,
            int invalidPhaseStates
    ) {
    }
}
