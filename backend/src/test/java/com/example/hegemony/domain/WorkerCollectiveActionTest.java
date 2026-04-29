package com.example.hegemony.domain;

import com.example.hegemony.domain.command.AssignWorkersCommand;
import com.example.hegemony.domain.command.AssignmentTargetType;
import com.example.hegemony.domain.command.PlaceDemonstrationCommand;
import com.example.hegemony.domain.command.PlaceStrikesCommand;
import com.example.hegemony.domain.command.WorkerAssignmentOperation;
import com.example.hegemony.domain.engine.GameRulesEngine;
import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.Enterprise;
import com.example.hegemony.domain.model.EnterpriseSlot;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.ResourceType;
import com.example.hegemony.domain.model.RoundPhase;
import com.example.hegemony.domain.model.Worker;
import com.example.hegemony.domain.model.WorkerLocation;
import com.example.hegemony.domain.model.WorkerQualification;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkerCollectiveActionTest {
    @Test
    void workerCanPlaceStrikeOnEligibleEnterpriseAndProductionSkipsIt() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);
        Enterprise target = nonWorkerEmptyEnterprise(state);

        var assigned = assignWorkersToEnterprise(engine, state, target);
        assertThat(assigned.validation().isValid()).isTrue();

        GameState assignedState = assigned.resultingState();
        Enterprise assignedEnterprise = assignedState.findEnterprise(target.getId()).orElseThrow();
        assignedEnterprise.setWageLevel(1);
        workersAt(assignedState, target.getId()).forEach(worker -> worker.setTiedContract(false));

        var strike = engine.apply(assignedState, new PlaceStrikesCommand("worker", List.of(target.getId())));

        assertThat(strike.validation().isValid()).isTrue();
        assertThat(strike.resultingState().findEnterprise(target.getId()).orElseThrow().isStrikeToken()).isTrue();

        GameState productionState = strike.resultingState();
        productionState.setCurrentPhase(RoundPhase.PRODUCTION);
        productionState.getTurnOrder().setCurrentPlayerIndex(0);
        int influenceBefore = productionState.findPlayerById("worker").orElseThrow().getInfluence();

        GameState resolved = CoreTestSupport.resolveProduction(productionState, engine);

        assertThat(resolved.findEnterprise(target.getId()).orElseThrow().isStrikeToken()).isFalse();
        assertThat(resolved.findPlayerById("worker").orElseThrow().getInfluence()).isGreaterThanOrEqualTo(influenceBefore + 1);
        assertThat(resolved.getLastProductionSummary().getEnterpriseResults()).anySatisfy(result -> {
            assertThat(result.getEnterpriseId()).isEqualTo(target.getId());
            assertThat(result.getProducedResources()).isEmpty();
            assertThat(result.getWagesPaidByRecipient()).isEmpty();
        });
    }

    @Test
    void strikeCannotBePlacedAtWageLevelThree() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);
        Enterprise target = nonWorkerEmptyEnterprise(state);

        var assigned = assignWorkersToEnterprise(engine, state, target);

        GameState assignedState = assigned.resultingState();
        assignedState.findEnterprise(target.getId()).orElseThrow().setWageLevel(3);
        workersAt(assignedState, target.getId()).forEach(worker -> worker.setTiedContract(false));

        var strike = engine.apply(assignedState, new PlaceStrikesCommand("worker", List.of(target.getId())));

        assertThat(strike.validation().isValid()).isFalse();
    }

    @Test
    void demonstrationResolvesInfluenceAndAllocatedVpPenaltyInProduction() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);
        for (int i = 0; i < 20; i++) {
            CoreTestSupport.addUnemployedWorker(state, "worker-extra-" + i, ClassType.WORKER, WorkerQualification.UNSKILLED);
        }
        state.findPlayerById("capitalist").orElseThrow().setVictoryPoints(10);
        state.findPlayerById("middle_class").orElseThrow().setVictoryPoints(10);

        var demonstration = engine.apply(state, new PlaceDemonstrationCommand("worker", Map.of(
                "capitalist", 1,
                "middle_class", 1
        )));

        assertThat(demonstration.validation().isValid()).isTrue();
        assertThat(demonstration.resultingState().isDemonstrationToken()).isTrue();

        GameState productionState = demonstration.resultingState();
        productionState.setCurrentPhase(RoundPhase.PRODUCTION);
        productionState.getTurnOrder().setCurrentPlayerIndex(0);
        int influenceBefore = productionState.findPlayerById("worker").orElseThrow().getInfluence();

        GameState resolved = CoreTestSupport.resolveProduction(productionState, engine);

        assertThat(resolved.isDemonstrationToken()).isFalse();
        assertThat(resolved.findPlayerById("worker").orElseThrow().getInfluence()).isGreaterThanOrEqualTo(influenceBefore + 1);
        assertThat(resolved.findPlayerById("capitalist").orElseThrow().getVictoryPoints()).isEqualTo(9);
        assertThat(resolved.findPlayerById("middle_class").orElseThrow().getVictoryPoints()).isEqualTo(9);
    }

    private Enterprise nonWorkerEmptyEnterprise(GameState state) {
        Enterprise enterprise = new Enterprise();
        enterprise.setId("test-capitalist-strike-target-" + state.getEnterprises().size());
        enterprise.setName("Test strike target");
        enterprise.setCategory("test");
        enterprise.setOwnerClass(ClassType.CAPITALIST);
        enterprise.setWageLevel(1);
        enterprise.setProducedResources(Map.of(ResourceType.FOOD.id(), 2));
        enterprise.setSlots(List.of(new EnterpriseSlot(
                enterprise.getId() + "-slot-1",
                WorkerQualification.UNSKILLED,
                null,
                null,
                null
        )));
        state.getEnterprises().add(enterprise);
        return enterprise;
    }

    private com.example.hegemony.domain.engine.ApplyCommandResult assignWorkersToEnterprise(
            GameRulesEngine engine,
            GameState state,
            Enterprise target
    ) {
        List<Worker> workers = CoreTestSupport.unemployedWorkers(state, ClassType.WORKER);
        List<WorkerAssignmentOperation> operations = new java.util.ArrayList<>();
        for (int i = 0; i < target.getSlots().size(); i++) {
            operations.add(new WorkerAssignmentOperation(
                    workers.get(i).getId(),
                    AssignmentTargetType.ENTERPRISE_SLOT,
                    target.getId() + ":" + target.getSlots().get(i).getId()
            ));
        }
        return engine.apply(state, new AssignWorkersCommand("worker", operations));
    }

    private List<Worker> workersAt(GameState state, String enterpriseId) {
        return state.getWorkers().stream()
                .filter(worker -> worker.getLocation() == WorkerLocation.ENTERPRISE_SLOT)
                .filter(worker -> enterpriseId.equals(worker.getEnterpriseId()))
                .toList();
    }
}
