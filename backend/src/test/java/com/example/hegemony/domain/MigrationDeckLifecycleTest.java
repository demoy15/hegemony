package com.example.hegemony.domain;

import com.example.hegemony.domain.command.ResolvePreparationPhaseCommand;
import com.example.hegemony.domain.engine.ApplyCommandResult;
import com.example.hegemony.domain.engine.GameRulesEngine;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.PopulationScale;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.RoundPhase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationDeckLifecycleTest {
    @Test
    void initialStateLoadsMigrationDeckCatalog() {
        GameState state = CoreTestSupport.state(4);

        assertThat(state.getMigrationCards()).hasSize(25);
        assertThat(state.getMigrationDeck().getOrderedCardIds()).hasSize(25);
    }

    @Test
    void policyBAddsTwoBaseWorkersAndOneMigrationWorkerForWorkerClass() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = stateInPreparationRound2();
        int expectedWorkerPopulation = PopulationScale.fromWorkerCount(workerCount(state, "worker") + 3);
        int expectedMiddlePopulation = PopulationScale.fromWorkerCount(workerCount(state, "middle_class") + 1);
        int beforeTotalWorkers = state.getWorkers().size();

        ApplyCommandResult result = engine.apply(state, new ResolvePreparationPhaseCommand("worker"));

        assertThat(result.validation().isValid()).isTrue();
        assertThat(result.resultingState().findPlayerById("worker").orElseThrow().getPopulation()).isEqualTo(expectedWorkerPopulation);
        assertThat(result.resultingState().findPlayerById("middle_class").orElseThrow().getPopulation()).isEqualTo(expectedMiddlePopulation);
        assertThat(result.resultingState().getWorkers()).hasSize(beforeTotalWorkers + 4);
        assertThat(result.resultingState().getMigrationDeck().getVisibleCardIds()).hasSize(2);
    }

    @Test
    void policyCAddsTwoBaseWorkersAndTwoMigrationWorkersForWorkerClass() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = stateInPreparationRound2();
        state.findPolicy(PolicyId.POLICY_7_IMMIGRATION).orElseThrow().setCurrentCourse(PolicyCourse.C);
        int expectedWorkerPopulation = PopulationScale.fromWorkerCount(workerCount(state, "worker") + 4);
        int expectedMiddlePopulation = PopulationScale.fromWorkerCount(workerCount(state, "middle_class") + 2);

        ApplyCommandResult result = engine.apply(state, new ResolvePreparationPhaseCommand("worker"));

        assertThat(result.validation().isValid()).isTrue();
        assertThat(result.resultingState().findPlayerById("worker").orElseThrow().getPopulation()).isEqualTo(expectedWorkerPopulation);
        assertThat(result.resultingState().findPlayerById("middle_class").orElseThrow().getPopulation()).isEqualTo(expectedMiddlePopulation);
        assertThat(result.resultingState().getMigrationDeck().getVisibleCardIds()).hasSize(4);
    }

    @Test
    void policyASkipsMigrationDraws() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = stateInPreparationRound2();
        state.findPolicy(PolicyId.POLICY_7_IMMIGRATION).orElseThrow().setCurrentCourse(PolicyCourse.A);
        int expectedWorkers = PopulationScale.fromWorkerCount(workerCount(state, "worker") + 2);
        int beforeMiddle = state.findPlayerById("middle_class").orElseThrow().getPopulation();

        ApplyCommandResult result = engine.apply(state, new ResolvePreparationPhaseCommand("worker"));

        assertThat(result.validation().isValid()).isTrue();
        assertThat(result.resultingState().findPlayerById("worker").orElseThrow().getPopulation()).isEqualTo(expectedWorkers);
        assertThat(result.resultingState().findPlayerById("middle_class").orElseThrow().getPopulation()).isEqualTo(beforeMiddle);
        assertThat(result.resultingState().getMigrationDeck().getVisibleCardIds()).isEmpty();
    }

    @Test
    void roundOneSkipsMigrationDrawsEvenOnOpenPolicy() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);
        state.setCurrentRound(1);
        state.getTurnOrder().setRound(1);
        state.setRoundMarker(1);
        state.setCurrentPhase(RoundPhase.PREPARATION);
        state.getTurnOrder().setPhase(RoundPhase.PREPARATION);
        int beforeWorkers = state.findPlayerById("worker").orElseThrow().getPopulation();
        int beforeMiddle = state.findPlayerById("middle_class").orElseThrow().getPopulation();

        ApplyCommandResult result = engine.apply(state, new ResolvePreparationPhaseCommand("worker"));

        assertThat(result.validation().isValid()).isTrue();
        assertThat(result.resultingState().findPlayerById("worker").orElseThrow().getPopulation()).isEqualTo(beforeWorkers);
        assertThat(result.resultingState().findPlayerById("middle_class").orElseThrow().getPopulation()).isEqualTo(beforeMiddle);
        assertThat(result.resultingState().getMigrationDeck().getVisibleCardIds()).isEmpty();
    }

    private GameState stateInPreparationRound2() {
        GameState state = CoreTestSupport.state(4);
        state.setCurrentRound(2);
        state.getTurnOrder().setRound(2);
        state.setRoundMarker(2);
        state.setCurrentPhase(RoundPhase.PREPARATION);
        state.getTurnOrder().setPhase(RoundPhase.PREPARATION);
        state.getTurnOrder().setCurrentPlayerIndex(0);
        return state;
    }

    private int workerCount(GameState state, String playerId) {
        return (int) state.getWorkers().stream()
                .filter(worker -> worker.getClassType().playerId().equals(playerId))
                .count();
    }
}
