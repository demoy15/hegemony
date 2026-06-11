package com.example.hegemony.domain;

import com.example.hegemony.domain.command.ResolveProductionPhaseCommand;
import com.example.hegemony.domain.engine.ApplyCommandResult;
import com.example.hegemony.domain.engine.GameRulesEngine;
import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.PlayerState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConsumerEconomyFoodIntegrationTest {
    @Test
    void productionPhaseConsumesMandatoryWorkerFood() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateInProductionPhase(4);
        PlayerState worker = state.findPlayerById("worker").orElseThrow();
        worker.setGoodsAmount("food", worker.getPopulation());

        ApplyCommandResult result = engine.apply(state, new ResolveProductionPhaseCommand("worker"));

        assertThat(result.validation().isValid()).isTrue();
        assertThat(result.resultingState().findPlayerById("worker").orElseThrow().getGoodsAmount("food")).isZero();
        assertThat(result.resultingState().getProductionPhaseState().getWorkerFoodRequired()).isEqualTo(worker.getPopulation());
        assertThat(result.resultingState().getProductionPhaseState().getWorkerFoodConsumed()).isEqualTo(worker.getPopulation());
        assertThat(result.resultingState().getProductionPhaseState().getWorkerFoodUnmet()).isEqualTo(0);
    }

    @Test
    void unifiedProductionPassBuysMissingFoodBeforeReportingNeedSatisfied() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateInProductionPhase(4);
        PlayerState worker = state.findPlayerById("worker").orElseThrow();
        worker.setGoodsAmount("food", Math.max(0, worker.getPopulation() - 1));

        ApplyCommandResult result = engine.apply(state, new ResolveProductionPhaseCommand("worker"));

        assertThat(result.validation().isValid()).isTrue();
        assertThat(result.resultingState().findPlayerById("worker").orElseThrow().getGoodsAmount("food")).isZero();
        assertThat(result.resultingState().getProductionPhaseState().getWorkerFoodConsumed()).isEqualTo(worker.getPopulation());
        assertThat(result.resultingState().getProductionPhaseState().getWorkerFoodUnmet()).isZero();
    }

    @Test
    void noShadowFoodCountersRemain() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateInProductionPhase(4);
        PlayerState worker = state.findPlayerById("worker").orElseThrow();
        worker.setGoodsAmount("food", worker.getPopulation());

        ApplyCommandResult result = engine.apply(state, new ResolveProductionPhaseCommand("worker"));

        assertThat(result.validation().isValid()).isTrue();
        PlayerState after = result.resultingState().findPlayerById("worker").orElseThrow();
        assertThat(after.getResources().getOrDefault("food", 0)).isEqualTo(after.getGoodsAndServicesArea().getOrDefault("food", 0));
    }

    @Test
    void productionRecordsWorkerAndCapitalistTaxes() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateInProductionPhase(2);
        PlayerState worker = state.findPlayerById("worker").orElseThrow();
        worker.setGoodsAmount("food", worker.getPopulation());

        ApplyCommandResult result = engine.apply(state, new ResolveProductionPhaseCommand("worker"));

        assertThat(result.validation().isValid()).isTrue();
        assertThat(result.resultingState().getProductionPhaseState().getWorkerTaxesPaid())
                .isEqualTo(worker.getPopulation() * 4);
        assertThat(result.resultingState().getProductionPhaseState().getCapitalistTaxesPaid()).isGreaterThan(0);
        assertThat(result.resultingState().getTaxMultiplier()).isEqualTo(5);
        assertThat(result.resultingState().getEventLog())
                .anySatisfy(entry -> assertThat(entry.getType()).isEqualTo("WORKER_TAX_PAID"));
        assertThat(result.resultingState().getEventLog())
                .anySatisfy(entry -> assertThat(entry.getType()).isEqualTo("CAPITALIST_TAX_PAID"));
    }

    @Test
    void mandatoryProductionPaymentsCreatePlayerLoansWhenCashIsShort() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateInProductionPhase(2);
        PlayerState worker = state.findPlayerById("worker").orElseThrow();
        worker.setMoney(0);
        worker.setPopulation(50);
        worker.setGoodsAmount("food", worker.getPopulation());

        ApplyCommandResult result = engine.apply(state, new ResolveProductionPhaseCommand("worker"));

        assertThat(result.validation().isValid()).isTrue();
        PlayerState afterWorker = result.resultingState().findPlayerById("worker").orElseThrow();
        assertThat(afterWorker.getResourceAmount("loan")).isGreaterThan(0);
        assertThat(result.resultingState().getEventLog())
                .anySatisfy(entry -> assertThat(entry.getType()).isEqualTo("PLAYER_LOAN_TAKEN"));
        assertThat(result.resultingState().getEconomyUnsupportedNotes().toString())
                .doesNotContain("UNSUPPORTED_LOANS");
    }

    @Test
    void productionLogsWagesAndProducedResourcesByEnterprise() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateInProductionPhase(2);
        state.findPlayerById("worker").orElseThrow()
                .setGoodsAmount("food", state.findPlayerById("worker").orElseThrow().getPopulation());

        ApplyCommandResult result = engine.apply(state, new ResolveProductionPhaseCommand("worker"));

        assertThat(result.validation().isValid()).isTrue();
        assertThat(result.resultingState().getEventLog())
                .anySatisfy(entry -> assertThat(entry.getType()).isEqualTo("WAGE_PAID"));
        assertThat(result.resultingState().getEventLog())
                .anySatisfy(entry -> assertThat(entry.getType()).isEqualTo("ENTERPRISE_PRODUCED"));
    }

    @Test
    void productionRecordsMiddleClassTaxesWhenActive() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateInProductionPhase(3);
        state.findPlayerById("worker").orElseThrow()
                .setGoodsAmount("food", state.findPlayerById("worker").orElseThrow().getPopulation());
        state.findPlayerById("middle_class").orElseThrow()
                .setGoodsAmount("food", state.findPlayerById("middle_class").orElseThrow().getPopulation());

        ApplyCommandResult result = engine.apply(state, new ResolveProductionPhaseCommand("worker"));

        assertThat(result.validation().isValid()).isTrue();
        assertThat(result.resultingState().getProductionPhaseState().getMiddleClassTaxesPaid()).isGreaterThan(0);
    }

    @Test
    void stateTreasuryTakesLoanWhenPayingStateEnterpriseWages() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateInProductionPhase(2);
        state.setTreasury(0);
        assertThat(state.getPlayers())
                .extracting(PlayerState::getClassType)
                .doesNotContain(ClassType.STATE);
        state.findPlayerById("worker").orElseThrow()
                .setGoodsAmount("food", state.findPlayerById("worker").orElseThrow().getPopulation());

        ApplyCommandResult result = engine.apply(state, new ResolveProductionPhaseCommand("worker"));

        assertThat(result.validation().isValid()).isTrue();
        assertThat(result.resultingState().getStateLoans()).isGreaterThan(0);
        assertThat(result.resultingState().getEventLog())
                .anySatisfy(entry -> assertThat(entry.getMessage()).contains("state paid"));
        assertThat(result.resultingState().getEventLog())
                .anySatisfy(entry -> assertThat(entry.getType()).isEqualTo("STATE_LOAN_TAKEN"));
        assertThat(result.resultingState().getEventLog())
                .anySatisfy(entry -> assertThat(entry.getMessage()).contains("state enterprise wages"));
    }
}
