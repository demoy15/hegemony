package com.example.hegemony.domain;

import com.example.hegemony.domain.command.ConsumeEducationCommand;
import com.example.hegemony.domain.command.ConsumeHealthcareCommand;
import com.example.hegemony.domain.command.ConsumeLuxuryCommand;
import com.example.hegemony.domain.engine.ApplyCommandResult;
import com.example.hegemony.domain.engine.GameRulesEngine;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.PlayerState;
import com.example.hegemony.domain.model.WorkerQualification;
import com.example.hegemony.domain.model.WorkerSector;
import com.example.hegemony.domain.model.WorkerSlotColor;
import com.example.hegemony.domain.rules.ValidationReasonCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConsumerEconomyConsumptionTest {
    @Test
    void workerConsumesHealthcareWhenEnoughForPopulation() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);
        PlayerState worker = state.findPlayerById("worker").orElseThrow();
        worker.setGoodsAmount("healthcare", worker.getPopulation());
        int beforeWelfare = worker.getWelfare();
        int beforeVp = worker.getVictoryPoints();

        ApplyCommandResult result = engine.apply(state, new ConsumeHealthcareCommand("worker"));

        assertThat(result.validation().isValid()).isTrue();
        PlayerState after = result.resultingState().findPlayerById("worker").orElseThrow();
        assertThat(after.getGoodsAmount("healthcare")).isEqualTo(0);
        assertThat(after.getWelfare()).isEqualTo(beforeWelfare + 1);
        assertThat(after.getVictoryPoints()).isEqualTo(beforeVp + after.getWelfare() + 2);
        assertThat(result.resultingState().getWorkers().stream().filter(workerUnit -> workerUnit.getClassType() == worker.getClassType()).count())
                .isGreaterThan(state.getWorkers().stream().filter(workerUnit -> workerUnit.getClassType() == worker.getClassType()).count());
    }

    @Test
    void workerConsumesEducationWhenEnoughForPopulation() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);
        PlayerState worker = state.findPlayerById("worker").orElseThrow();
        worker.setGoodsAmount("education", worker.getPopulation());
        String workerId = state.getWorkers().stream()
                .filter(workerUnit -> workerUnit.getClassType() == worker.getClassType())
                .filter(workerUnit -> workerUnit.getQualificationType() == WorkerQualification.UNSKILLED)
                .findFirst()
                .orElseThrow()
                .getId();

        ApplyCommandResult result = engine.apply(state, new ConsumeEducationCommand("worker", workerId, WorkerSlotColor.BLUE));

        assertThat(result.validation().isValid()).isTrue();
        assertThat(result.resultingState().findPlayerById("worker").orElseThrow().getGoodsAmount("education")).isEqualTo(0);
        assertThat(result.resultingState().findWorker(workerId).orElseThrow().getQualificationType()).isEqualTo(WorkerQualification.SKILLED);
        assertThat(result.resultingState().findWorker(workerId).orElseThrow().getSector()).isEqualTo(WorkerSector.BLUE);
    }

    @Test
    void workerConsumesLuxuryWhenEnoughForPopulation() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);
        PlayerState worker = state.findPlayerById("worker").orElseThrow();
        worker.setGoodsAmount("luxury", worker.getPopulation());

        ApplyCommandResult result = engine.apply(state, new ConsumeLuxuryCommand("worker"));

        assertThat(result.validation().isValid()).isTrue();
        assertThat(result.resultingState().findPlayerById("worker").orElseThrow().getGoodsAmount("luxury")).isEqualTo(0);
    }

    @Test
    void middleClassConsumesSupportedResources() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);
        state.getTurnOrder().setCurrentPlayerIndex(1);
        state.setCurrentPhase(com.example.hegemony.domain.model.RoundPhase.ACTIONS);
        PlayerState middle = state.findPlayerById("middle_class").orElseThrow();
        middle.setGoodsAmount("healthcare", middle.getPopulation());

        ApplyCommandResult result = engine.apply(state, new ConsumeHealthcareCommand("middle_class"));

        assertThat(result.validation().isValid()).isTrue();
        assertThat(result.resultingState().findPlayerById("middle_class").orElseThrow().getWelfare()).isEqualTo(middle.getWelfare() + 1);
    }

    @Test
    void consumptionFailsIfBelowPopulationRequirement() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);
        PlayerState worker = state.findPlayerById("worker").orElseThrow();
        worker.setGoodsAmount("healthcare", Math.max(0, worker.getPopulation() - 1));

        ApplyCommandResult result = engine.apply(state, new ConsumeHealthcareCommand("worker"));

        assertThat(result.validation().isValid()).isFalse();
        assertThat(result.validation().getReasonCodes()).contains(ValidationReasonCode.INSUFFICIENT_RESOURCE_FOR_CONSUMPTION);
    }

    @Test
    void welfareIncreasesOnSuccessfulConsumption() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);
        PlayerState worker = state.findPlayerById("worker").orElseThrow();
        worker.setGoodsAmount("luxury", worker.getPopulation());
        int beforeWelfare = worker.getWelfare();
        int beforeVp = worker.getVictoryPoints();

        ApplyCommandResult result = engine.apply(state, new ConsumeLuxuryCommand("worker"));

        assertThat(result.validation().isValid()).isTrue();
        PlayerState after = result.resultingState().findPlayerById("worker").orElseThrow();
        assertThat(after.getWelfare()).isEqualTo(beforeWelfare + 1);
        assertThat(after.getLastWelfareDelta()).isEqualTo(1);
        assertThat(after.getVictoryPoints()).isEqualTo(beforeVp + after.getWelfare());
    }
}
