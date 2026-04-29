package com.example.hegemony.domain;

import com.example.hegemony.domain.command.ProposeBillCommand;
import com.example.hegemony.domain.command.ResolveScoringPhaseCommand;
import com.example.hegemony.domain.engine.ApplyCommandResult;
import com.example.hegemony.domain.engine.GameRulesEngine;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.GameStatus;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.RoundPhase;
import com.example.hegemony.infrastructure.JsonGameStateStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GameOverLifecycleTest {
    @Test
    void afterRound5Scoring_gameEnds() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = stateInRound5Scoring();

        state = apply(engine, state, new ResolveScoringPhaseCommand(state.currentPlayer().getPlayerId()));

        assertThat(state.getGameStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(state.getCurrentPhase()).isEqualTo(RoundPhase.GAME_OVER);
        assertThat(state.isGameOver()).isTrue();
    }

    @Test
    void noNormalActionsLegalInGameOver() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState finished = apply(engine, stateInRound5Scoring(), new ResolveScoringPhaseCommand("worker"));

        assertThat(engine.generateLegalCommands(finished)).isEmpty();
        var validation = engine.validate(finished, new ProposeBillCommand(
                "worker",
                com.example.hegemony.domain.model.PolicyId.POLICY_3_TAXATION,
                com.example.hegemony.domain.model.PolicyCourse.B
        ));
        assertThat(validation.isValid()).isFalse();
        assertThat(validation.getReasonCodes()).contains(
                com.example.hegemony.domain.rules.ValidationReasonCode.GAME_ALREADY_FINISHED,
                com.example.hegemony.domain.rules.ValidationReasonCode.COMMAND_NOT_ALLOWED_IN_GAME_OVER
        );
    }

    @Test
    void finalResultIsPersistedInState() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = apply(engine, stateInRound5Scoring(), new ResolveScoringPhaseCommand("worker"));

        assertThat(state.getFinalResult()).isNotNull();
        assertThat(state.getFinalResult().getStandings()).isNotEmpty();
        assertThat(state.getFinalResult().getWinnerPlayerIds()).isNotEmpty();
    }

    @Test
    void finalWinnerComputedFromSupportedVp() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = stateInRound5Scoring();
        state.findPlayerById("worker").orElseThrow().setVictoryPoints(9);
        state.findPlayerById("worker").orElseThrow().setMoney(0);
        state.findPlayerById("capitalist").orElseThrow().setVictoryPoints(5);
        state.findPlayerById("capitalist").orElseThrow().setRevenue(0);
        state.findPlayerById("capitalist").orElseThrow().setCapital(0);
        state.findPlayerById("capitalist").orElseThrow().setProducedResourceStorage(Map.of());
        resetFinalPoliciesToB(state);

        state = apply(engine, state, new ResolveScoringPhaseCommand("worker"));

        assertThat(state.getFinalResult()).isNotNull();
        assertThat(state.getFinalResult().isTie()).isFalse();
        assertThat(state.getFinalResult().getWinnerPlayerIds()).containsExactly("worker");
    }

    @Test
    void tieHandledExplicitlyIfTiebreakStillUnresolved() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = stateInRound5Scoring();
        state.findPlayerById("worker").orElseThrow().setVictoryPoints(7);
        state.findPlayerById("worker").orElseThrow().setMoney(0);
        state.findPlayerById("capitalist").orElseThrow().setVictoryPoints(7);
        state.findPlayerById("capitalist").orElseThrow().setRevenue(0);
        state.findPlayerById("capitalist").orElseThrow().setCapital(0);
        state.findPlayerById("capitalist").orElseThrow().setProducedResourceStorage(Map.of());
        resetFinalPoliciesToB(state);

        state = apply(engine, state, new ResolveScoringPhaseCommand("worker"));

        assertThat(state.getFinalResult()).isNotNull();
        assertThat(state.getFinalResult().isTie()).isTrue();
        assertThat(state.getFinalResult().isUnresolvedTie()).isTrue();
        assertThat(state.getFinalResult().getUnsupportedNotes().toString()).contains("Final tiebreak remained unresolved");
    }

    @Test
    void finalTieCanBeResolvedByPolicyInterests() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = stateInRound5Scoring();
        CoreTestSupport.policy(state, PolicyId.POLICY_1_FISCAL).setCurrentCourse(PolicyCourse.A);
        CoreTestSupport.policy(state, PolicyId.POLICY_2_LABOR_MARKET).setCurrentCourse(PolicyCourse.A);
        CoreTestSupport.policy(state, PolicyId.POLICY_3_TAXATION).setCurrentCourse(PolicyCourse.C);
        CoreTestSupport.policy(state, PolicyId.POLICY_4_HEALTHCARE_AND_BENEFITS).setCurrentCourse(PolicyCourse.B);
        CoreTestSupport.policy(state, PolicyId.POLICY_5_EDUCATION).setCurrentCourse(PolicyCourse.B);
        state.findPlayerById("worker").orElseThrow().setVictoryPoints(6);
        state.findPlayerById("worker").orElseThrow().setMoney(0);
        state.findPlayerById("capitalist").orElseThrow().setVictoryPoints(10);
        state.findPlayerById("capitalist").orElseThrow().setRevenue(0);
        state.findPlayerById("capitalist").orElseThrow().setCapital(0);
        state.findPlayerById("capitalist").orElseThrow().setProducedResourceStorage(Map.of());

        state = apply(engine, state, new ResolveScoringPhaseCommand("worker"));

        assertThat(state.getFinalResult()).isNotNull();
        assertThat(state.getFinalResult().isTie()).isFalse();
        assertThat(state.getFinalResult().isTiebreakApplied()).isTrue();
        assertThat(state.getFinalResult().getWinnerPlayerIds()).containsExactly("worker");
    }

    @Test
    void saveLoadPreservesFinishedGameState() throws Exception {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState finished = apply(engine, stateInRound5Scoring(), new ResolveScoringPhaseCommand("worker"));

        Path tempDir = Files.createTempDirectory("hegemony-save-test");
        JsonGameStateStorage storage = new JsonGameStateStorage(new ObjectMapper(), tempDir.toString());
        storage.save(finished, "finished.json");
        GameState loaded = storage.load("finished.json");

        assertThat(loaded.getGameStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(loaded.getCurrentPhase()).isEqualTo(RoundPhase.GAME_OVER);
        assertThat(loaded.getFinalResult()).isNotNull();
    }

    private GameState stateInRound5Scoring() {
        GameState state = CoreTestSupport.state(2);
        state.setCurrentRound(5);
        state.getTurnOrder().setRound(5);
        state.setRoundMarker(5);
        state.setCurrentPhase(RoundPhase.SCORING);
        state.getTurnOrder().setPhase(RoundPhase.SCORING);
        state.setGameStatus(GameStatus.IN_PROGRESS);
        state.getTurnOrder().setCurrentPlayerIndex(0);
        return state;
    }

    private void resetFinalPoliciesToB(GameState state) {
        CoreTestSupport.policy(state, PolicyId.POLICY_1_FISCAL).setCurrentCourse(PolicyCourse.B);
        CoreTestSupport.policy(state, PolicyId.POLICY_2_LABOR_MARKET).setCurrentCourse(PolicyCourse.B);
        CoreTestSupport.policy(state, PolicyId.POLICY_3_TAXATION).setCurrentCourse(PolicyCourse.B);
        CoreTestSupport.policy(state, PolicyId.POLICY_4_HEALTHCARE_AND_BENEFITS).setCurrentCourse(PolicyCourse.B);
        CoreTestSupport.policy(state, PolicyId.POLICY_5_EDUCATION).setCurrentCourse(PolicyCourse.B);
    }

    private GameState apply(GameRulesEngine engine, GameState state, com.example.hegemony.domain.command.GameCommand command) {
        ApplyCommandResult result = engine.apply(state, command);
        assertThat(result.validation().isValid()).as(result.validation().getErrors().toString()).isTrue();
        return result.resultingState();
    }
}
