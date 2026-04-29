package com.example.hegemony.domain;

import com.example.hegemony.domain.command.ResolvePreparationPhaseCommand;
import com.example.hegemony.domain.command.ResolveScoringPhaseCommand;
import com.example.hegemony.domain.engine.ApplyCommandResult;
import com.example.hegemony.domain.engine.GameRulesEngine;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.RoundPhase;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PreparationAndScoringPhaseTest {
    @Test
    void supportedPreparationStepsRun() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = stateInPreparationRound2();

        state = apply(engine, state, new ResolvePreparationPhaseCommand("worker"));

        assertThat(state.getCurrentPhase()).isEqualTo(RoundPhase.ACTIONS);
        assertThat(state.getLastPreparationSummary()).isNotNull();
        assertThat(state.getLastPreparationSummary().isResolved()).isTrue();
        assertThat(state.getLastPreparationSummary().getExecutedSubsteps()).contains("round_marker_confirmed", "migration_resolved");
    }

    @Test
    void unsupportedPreparationStepsAreExplicitlyReported() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = apply(engine, stateInPreparationRound2(), new ResolvePreparationPhaseCommand("worker"));

        assertThat(state.getLastPreparationSummary()).isNotNull();
        assertThat(state.getLastPreparationSummary().getUnsupportedSubsteps().toString())
                .contains("UNSUPPORTED_PREPARATION_SUBSTEP");
        assertThat(state.getLastPreparationSummary().getUnsupportedSubsteps().toString())
                .doesNotContain("migration/worker growth");
    }

    @Test
    void preparationSummaryIsProduced() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = apply(engine, stateInPreparationRound2(), new ResolvePreparationPhaseCommand("worker"));

        assertThat(state.getLastRoundSummary()).isNotNull();
        assertThat(state.getLastRoundSummary().getPreparationSummary()).isNotNull();
    }

    @Test
    void preparationPaysStateLoanInterestFromTreasury() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = stateInPreparationRound2();
        state.setStateLoans(2);
        state.setTreasury(100);

        state = apply(engine, state, new ResolvePreparationPhaseCommand("worker"));

        assertThat(state.getTreasury()).isEqualTo(90);
        assertThat(state.getLastPreparationSummary().getExecutedSubsteps()).contains("state_loan_interest_paid");
        assertThat(state.getLastPreparationSummary().getNotes()).anySatisfy(note -> assertThat(note).contains("10"));
        assertThat(state.getEventLog()).anySatisfy(entry -> assertThat(entry.getType()).isEqualTo("STATE_LOAN_INTEREST_PAID"));
    }

    @Test
    void scoringPhaseProducesBreakdown() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = stateInScoringRound2();

        state = apply(engine, state, new ResolveScoringPhaseCommand("worker"));

        assertThat(state.getLastScoringSummary()).isNotNull();
        assertThat(state.getLastScoringSummary().getPlayers()).hasSize(state.getPlayers().size());
    }

    @Test
    void onlySupportedScoringSourcesAreApplied() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = stateInScoringRound2();
        int beforeWorkerVp = state.findPlayerById("worker").orElseThrow().getVictoryPoints();

        state = apply(engine, state, new ResolveScoringPhaseCommand("worker"));

        int afterWorkerVp = state.findPlayerById("worker").orElseThrow().getVictoryPoints();
        assertThat(afterWorkerVp).isEqualTo(beforeWorkerVp);
        assertThat(state.getLastScoringSummary().getPlayers())
                .filteredOn(row -> row.getPlayerId().equals("worker"))
                .singleElement()
                .satisfies(row -> assertThat(row.getGainedThisPhase()).isEqualTo(0));
    }

    @Test
    void capitalistWealthTrackMovesRevenueAndScoresGrowth() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = stateInScoringRound2();
        var capitalist = state.findPlayerById("capitalist").orElseThrow();
        capitalist.setCapital(0);
        capitalist.setRevenue(57);
        capitalist.setWealthTrackLevel(0);

        state = apply(engine, state, new ResolveScoringPhaseCommand("worker"));

        capitalist = state.findPlayerById("capitalist").orElseThrow();
        assertThat(capitalist.getCapital()).isEqualTo(57);
        assertThat(capitalist.getRevenue()).isZero();
        assertThat(capitalist.getWealthTrackLevel()).isEqualTo(3);
        assertThat(capitalist.getVictoryPoints()).isEqualTo(12);
    }

    @Test
    void capitalistWealthGrowthBonusOnlyUsesNewMaximum() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = stateInScoringRound2();
        var capitalist = state.findPlayerById("capitalist").orElseThrow();
        capitalist.setCapital(57);
        capitalist.setRevenue(0);
        capitalist.setWealthTrackLevel(3);

        state = apply(engine, state, new ResolveScoringPhaseCommand("worker"));

        capitalist = state.findPlayerById("capitalist").orElseThrow();
        assertThat(capitalist.getWealthTrackLevel()).isEqualTo(3);
        assertThat(capitalist.getVictoryPoints()).isEqualTo(3);
        assertThat(state.getLastScoringSummary().getPlayers())
                .filteredOn(row -> row.getPlayerId().equals("capitalist"))
                .singleElement()
                .satisfies(row -> assertThat(row.getSources().toString()).doesNotContain("capitalist_wealth_track_growth"));
    }

    @Test
    void finalScoringAddsWorkerAndCapitalistEndgameSources() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = stateInScoringRound5();
        CoreTestSupport.policy(state, PolicyId.POLICY_1_FISCAL).setCurrentCourse(PolicyCourse.A);
        CoreTestSupport.policy(state, PolicyId.POLICY_2_LABOR_MARKET).setCurrentCourse(PolicyCourse.A);
        CoreTestSupport.policy(state, PolicyId.POLICY_3_TAXATION).setCurrentCourse(PolicyCourse.C);
        CoreTestSupport.policy(state, PolicyId.POLICY_4_HEALTHCARE_AND_BENEFITS).setCurrentCourse(PolicyCourse.C);
        CoreTestSupport.policy(state, PolicyId.POLICY_5_EDUCATION).setCurrentCourse(PolicyCourse.C);
        state.findPlayerById("worker").orElseThrow().setMoney(25);
        var capitalist = state.findPlayerById("capitalist").orElseThrow();
        capitalist.setCapital(0);
        capitalist.setRevenue(0);
        capitalist.setProducedResourceStorage(Map.of(
                "food", 5,
                "ftz_food", 1,
                "luxury", 3,
                "education", 6
        ));

        state = apply(engine, state, new ResolveScoringPhaseCommand("worker"));

        assertThat(state.getFinalResult()).isNotNull();
        assertThat(state.findPlayerById("worker").orElseThrow().getVictoryPoints()).isEqualTo(10);
        assertThat(state.findPlayerById("capitalist").orElseThrow().getVictoryPoints()).isEqualTo(18);
        assertThat(state.getFinalResult().getScoringBreakdown().stream()
                .flatMap(row -> row.getSources().stream())
                .map(source -> source.getSourceId())
                .toList())
                .contains("worker_final_socialist_policies")
                .contains("capitalist_final_neoliberal_policies")
                .contains("capitalist_final_resources");
    }

    @Test
    void unsupportedScoringSourcesAreNotFaked() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = apply(engine, stateInScoringRound2(), new ResolveScoringPhaseCommand("worker"));

        assertThat(state.getLastScoringSummary()).isNotNull();
        assertThat(state.getLastScoringSummary().getUnsupportedSources().toString()).contains("UNSUPPORTED_SCORING_SOURCE");
    }

    @Test
    void accumulatedVpCarriesCorrectlyAcrossRounds() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = stateInScoringRound2();
        state.findPlayerById("worker").orElseThrow().setVictoryPoints(11);

        state = apply(engine, state, new ResolveScoringPhaseCommand("worker"));

        assertThat(state.getLastScoringSummary().getPlayers())
                .filteredOn(row -> row.getPlayerId().equals("worker"))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.getAccumulatedBeforePhase()).isEqualTo(11);
                    assertThat(row.getTotalAfterPhase()).isEqualTo(11);
                });
    }

    private GameState stateInPreparationRound2() {
        GameState state = CoreTestSupport.state(2);
        state.setCurrentRound(2);
        state.getTurnOrder().setRound(2);
        state.setRoundMarker(2);
        state.setCurrentPhase(RoundPhase.PREPARATION);
        state.getTurnOrder().setPhase(RoundPhase.PREPARATION);
        state.getTurnOrder().setCurrentPlayerIndex(0);
        return state;
    }

    private GameState stateInScoringRound2() {
        GameState state = CoreTestSupport.state(2);
        state.setCurrentRound(2);
        state.getTurnOrder().setRound(2);
        state.setRoundMarker(2);
        state.setCurrentPhase(RoundPhase.SCORING);
        state.getTurnOrder().setPhase(RoundPhase.SCORING);
        state.getTurnOrder().setCurrentPlayerIndex(0);
        return state;
    }

    private GameState stateInScoringRound5() {
        GameState state = stateInScoringRound2();
        state.setCurrentRound(5);
        state.getTurnOrder().setRound(5);
        state.setRoundMarker(5);
        return state;
    }

    private GameState apply(GameRulesEngine engine, GameState state, com.example.hegemony.domain.command.GameCommand command) {
        ApplyCommandResult result = engine.apply(state, command);
        assertThat(result.validation().isValid()).as(result.validation().getErrors().toString()).isTrue();
        return result.resultingState();
    }
}
