package com.example.hegemony.domain;

import com.example.hegemony.domain.command.AdvanceToNextRoundCommand;
import com.example.hegemony.domain.command.AdvanceToProductionCommand;
import com.example.hegemony.domain.command.AdvanceToScoringCommand;
import com.example.hegemony.domain.command.AdvanceToVotingCommand;
import com.example.hegemony.domain.command.EndTurnCommand;
import com.example.hegemony.domain.command.ResolvePreparationPhaseCommand;
import com.example.hegemony.domain.command.ResolveProductionPhaseCommand;
import com.example.hegemony.domain.command.ResolveScoringPhaseCommand;
import com.example.hegemony.domain.engine.ApplyCommandResult;
import com.example.hegemony.domain.engine.GameRulesEngine;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.RoundPhase;
import com.example.hegemony.domain.rules.ValidationReasonCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoundLifecycleTest {
    @Test
    void round1_skipsPreparation() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(2);

        assertThat(state.getCurrentRound()).isEqualTo(1);
        assertThat(state.getCurrentPhase()).isEqualTo(RoundPhase.ACTIONS);
        assertThat(engine.generateLegalMoves(state))
                .extracting(move -> move.actionType().name())
                .doesNotContain("RESOLVE_PREPARATION_PHASE");
    }

    @Test
    void scoringPhaseBecomesAvailableAfterSupportedPriorPhases() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateInProductionPhase(2);

        state = apply(engine, state, new ResolveProductionPhaseCommand(state.currentPlayer().getPlayerId()));
        var validation = engine.validate(state, new AdvanceToVotingCommand(state.currentPlayer().getPlayerId()));

        assertThat(validation.isValid()).isTrue();
    }

    @Test
    void scoringThenAdvanceRoundWorks() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateInProductionPhase(2);

        state = apply(engine, state, new ResolveProductionPhaseCommand(state.currentPlayer().getPlayerId()));
        state = apply(engine, state, new AdvanceToVotingCommand(state.currentPlayer().getPlayerId()));
        state = apply(engine, state, new ResolveScoringPhaseCommand(state.currentPlayer().getPlayerId()));
        state = apply(engine, state, new AdvanceToNextRoundCommand(state.currentPlayer().getPlayerId()));

        assertThat(state.getCurrentRound()).isEqualTo(2);
        assertThat(state.getCurrentPhase()).isEqualTo(RoundPhase.PREPARATION);
    }

    @Test
    void cannotAdvanceRoundBeforeScoringResolution() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateInProductionPhase(2);
        state = apply(engine, state, new ResolveProductionPhaseCommand(state.currentPlayer().getPlayerId()));
        state = apply(engine, state, new AdvanceToVotingCommand(state.currentPlayer().getPlayerId()));

        var validation = engine.validate(state, new AdvanceToNextRoundCommand(state.currentPlayer().getPlayerId()));
        assertThat(validation.isValid()).isFalse();
        assertThat(validation.getReasonCodes()).contains(ValidationReasonCode.CANNOT_ADVANCE_ROUND_BEFORE_SCORING);
    }

    @Test
    void roundAdvancesCorrectlyUntilRound5() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(2);

        int guard = 0;
        while (state.getCurrentRound() < 5 && guard++ < 50) {
            state = driveToScoring(state, engine);
            state = apply(engine, state, new ResolveScoringPhaseCommand(state.currentPlayer().getPlayerId()));
            if (state.getCurrentRound() < 5) {
                state = apply(engine, state, new AdvanceToNextRoundCommand(state.currentPlayer().getPlayerId()));
                if (state.getCurrentPhase() == RoundPhase.PREPARATION) {
                    state = apply(engine, state, new ResolvePreparationPhaseCommand(state.currentPlayer().getPlayerId()));
                }
            }
        }

        assertThat(guard).isLessThan(50);
        assertThat(state.getCurrentRound()).isEqualTo(5);
    }

    @Test
    void endTurnPassesActionsPriorityToNextActor() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(2);

        state = apply(engine, state, new EndTurnCommand());

        assertThat(state.getCurrentPhase()).isEqualTo(RoundPhase.ACTIONS);
        assertThat(state.currentPlayer()).isNotNull();
        assertThat(state.currentPlayer().getPlayerId()).isEqualTo("capitalist");
    }

    @Test
    void lastActionTurnTransitionsIntoVotingAtFirstActor() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(2);

        for (int i = 0; i < 10; i++) {
            state = apply(engine, state, new EndTurnCommand());
        }

        assertThat(state.getCurrentPhase()).isEqualTo(RoundPhase.PRODUCTION);
        assertThat(state.getTurnOrder().getCurrentPlayerIndex()).isEqualTo(0);
        assertThat(state.currentPlayer()).isNotNull();
        assertThat(state.currentPlayer().getPlayerId()).isEqualTo("worker");
    }

    @Test
    void cannotAdvanceToVotingBeforeEveryPlayerUsesFiveTurns() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(2);

        var validation = engine.validate(state, new AdvanceToVotingCommand("worker"));

        assertThat(validation.isValid()).isFalse();
        assertThat(validation.getReasonCodes()).contains(ValidationReasonCode.NOT_IN_PRODUCTION_PHASE);
    }

    private GameState driveToScoring(GameState state, GameRulesEngine engine) {
        GameState current = state;
        if (current.getCurrentPhase() == RoundPhase.PREPARATION) {
            current = apply(engine, current, new ResolvePreparationPhaseCommand(current.currentPlayer().getPlayerId()));
        }
        if (current.getCurrentPhase() == RoundPhase.ACTIONS) {
            current = CoreTestSupport.exhaustActionPhase(current, engine);
        }
        if (current.getCurrentPhase() == RoundPhase.PRODUCTION) {
            current = apply(engine, current, new ResolveProductionPhaseCommand(current.currentPlayer().getPlayerId()));
            current = apply(engine, current, new AdvanceToVotingCommand(current.currentPlayer().getPlayerId()));
        }
        if (current.getCurrentPhase() == RoundPhase.VOTING && current.getCurrentVoteState() == null) {
            current = apply(engine, current, new AdvanceToScoringCommand(current.currentPlayer().getPlayerId()));
        }
        return current;
    }

    private GameState apply(GameRulesEngine engine, GameState state, com.example.hegemony.domain.command.GameCommand command) {
        ApplyCommandResult result = engine.apply(state, command);
        assertThat(result.validation().isValid()).as(result.validation().getErrors().toString()).isTrue();
        return result.resultingState();
    }
}
