package com.example.hegemony.domain;

import com.example.hegemony.domain.command.AssignWorkersCommand;
import com.example.hegemony.domain.command.AssignmentTargetType;
import com.example.hegemony.domain.command.WorkerAssignmentOperation;
import com.example.hegemony.domain.engine.GameRulesEngine;
import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.Enterprise;
import com.example.hegemony.domain.model.EnterpriseSlot;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.WorkerLocation;
import com.example.hegemony.domain.model.Worker;
import com.example.hegemony.domain.model.WorkerQualification;
import com.example.hegemony.domain.rules.ValidationReasonCode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AssignWorkersLegalityTest {
    @Test
    void canAssignUpToThreeWorkers() {
        GameState state = CoreTestSupport.state(2);
        GameRulesEngine engine = CoreTestSupport.engine();
        Enterprise target = CoreTestSupport.firstEmptyEnterpriseWithAtLeastSlots(state, 2);
        List<Worker> workers = CoreTestSupport.unemployedWorkers(state, ClassType.WORKER);

        var command = new AssignWorkersCommand("worker", List.of(
                new WorkerAssignmentOperation(workers.get(0).getId(), AssignmentTargetType.ENTERPRISE_SLOT, target.getId() + ":" + target.getSlots().get(0).getId()),
                new WorkerAssignmentOperation(workers.get(1).getId(), AssignmentTargetType.ENTERPRISE_SLOT, target.getId() + ":" + target.getSlots().get(1).getId())
        ));

        var result = engine.validate(state, command);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void assigningWorkersTiesThemByContractAndLogsIt() {
        GameState state = CoreTestSupport.state(2);
        GameRulesEngine engine = CoreTestSupport.engine();
        Enterprise target = CoreTestSupport.firstEmptyEnterpriseWithAtLeastSlots(state, 2);
        List<Worker> workers = CoreTestSupport.unemployedWorkers(state, ClassType.WORKER);

        var command = new AssignWorkersCommand("worker", List.of(
                new WorkerAssignmentOperation(workers.get(0).getId(), AssignmentTargetType.ENTERPRISE_SLOT, target.getId() + ":" + target.getSlots().get(0).getId()),
                new WorkerAssignmentOperation(workers.get(1).getId(), AssignmentTargetType.ENTERPRISE_SLOT, target.getId() + ":" + target.getSlots().get(1).getId())
        ));

        var result = engine.apply(state, command);

        assertThat(result.validation().isValid()).isTrue();
        assertThat(result.resultingState().findWorker(workers.get(0).getId()).orElseThrow().isTiedContract()).isTrue();
        assertThat(result.resultingState().findWorker(workers.get(1).getId()).orElseThrow().isTiedContract()).isTrue();
        assertThat(result.producedEvents())
                .filteredOn(event -> event.type().equals("WORKER_ASSIGNED"))
                .hasSize(2)
                .extracting(event -> event.description())
                .allSatisfy(description -> assertThat(description).contains("labor contract"));
    }

    @Test
    void productionPhaseReleasesEnterpriseLaborContracts() {
        GameState state = CoreTestSupport.state(2);
        GameRulesEngine engine = CoreTestSupport.engine();
        Enterprise target = CoreTestSupport.firstEmptyEnterpriseWithAtLeastSlots(state, 2);
        List<Worker> workers = CoreTestSupport.unemployedWorkers(state, ClassType.WORKER);

        var assigned = engine.apply(state, new AssignWorkersCommand("worker", List.of(
                new WorkerAssignmentOperation(workers.get(0).getId(), AssignmentTargetType.ENTERPRISE_SLOT, target.getId() + ":" + target.getSlots().get(0).getId()),
                new WorkerAssignmentOperation(workers.get(1).getId(), AssignmentTargetType.ENTERPRISE_SLOT, target.getId() + ":" + target.getSlots().get(1).getId())
        )));
        assertThat(assigned.validation().isValid()).isTrue();

        GameState production = CoreTestSupport.exhaustActionPhase(assigned.resultingState(), engine);
        GameState resolved = CoreTestSupport.resolveProduction(production, engine);

        assertThat(resolved.findWorker(workers.get(0).getId()).orElseThrow().isTiedContract()).isFalse();
        assertThat(resolved.findWorker(workers.get(1).getId()).orElseThrow().isTiedContract()).isFalse();
        assertThat(resolved.getEventLog())
                .anySatisfy(entry -> assertThat(entry.getType()).isEqualTo("CONTRACTS_RELEASED"));
    }

    @Test
    void cannotAssignMoreThanThreeWorkers() {
        GameState state = CoreTestSupport.state(2);
        GameRulesEngine engine = CoreTestSupport.engine();

        CoreTestSupport.addUnemployedWorker(state, "worker-extra-1", ClassType.WORKER, WorkerQualification.UNSKILLED);
        CoreTestSupport.addUnemployedWorker(state, "worker-extra-2", ClassType.WORKER, WorkerQualification.UNSKILLED);

        Enterprise enterpriseA = CoreTestSupport.firstEmptyEnterpriseWithAtLeastSlots(state, 2);
        Enterprise enterpriseB = state.getEnterprises().stream().filter(e -> e != enterpriseA).findFirst().orElseThrow();

        List<Worker> workers = CoreTestSupport.unemployedWorkers(state, ClassType.WORKER);
        List<WorkerAssignmentOperation> ops = new ArrayList<>();
        ops.add(new WorkerAssignmentOperation(workers.get(0).getId(), AssignmentTargetType.ENTERPRISE_SLOT, enterpriseA.getId() + ":" + enterpriseA.getSlots().get(0).getId()));
        ops.add(new WorkerAssignmentOperation(workers.get(1).getId(), AssignmentTargetType.ENTERPRISE_SLOT, enterpriseA.getId() + ":" + enterpriseA.getSlots().get(1).getId()));
        ops.add(new WorkerAssignmentOperation(workers.get(2).getId(), AssignmentTargetType.ENTERPRISE_SLOT, enterpriseB.getId() + ":" + enterpriseB.getSlots().get(0).getId()));
        ops.add(new WorkerAssignmentOperation(workers.get(3).getId(), AssignmentTargetType.ENTERPRISE_SLOT, enterpriseB.getId() + ":" + enterpriseB.getSlots().get(1).getId()));

        var result = engine.validate(state, new AssignWorkersCommand("worker", ops));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getReasonCodes()).contains(ValidationReasonCode.TOO_MANY_WORKERS_IN_ONE_ASSIGN_ACTION);
    }

    @Test
    void cannotAssignTiedWorker() {
        GameState state = CoreTestSupport.state(2);
        GameRulesEngine engine = CoreTestSupport.engine();
        Enterprise target = CoreTestSupport.firstEmptyEnterpriseWithAtLeastSlots(state, 2);
        Worker tied = state.getWorkers().stream().filter(Worker::isTiedContract).findFirst().orElseThrow();

        var result = engine.validate(state, new AssignWorkersCommand("worker", List.of(
                new WorkerAssignmentOperation(tied.getId(), AssignmentTargetType.ENTERPRISE_SLOT, target.getId() + ":" + target.getSlots().get(0).getId())
        )));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getReasonCodes()).contains(ValidationReasonCode.WORKER_TIED_BY_CONTRACT);
    }

    @Test
    void workerCanReturnUntiedEmployedWorkerToUnemployed() {
        GameState state = CoreTestSupport.state(2);
        GameRulesEngine engine = CoreTestSupport.engine();
        Worker employed = state.getWorkers().stream()
                .filter(worker -> worker.getClassType() == ClassType.WORKER)
                .filter(worker -> worker.getLocation() == WorkerLocation.ENTERPRISE_SLOT)
                .findFirst()
                .orElseThrow();
        employed.setTiedContract(false);

        var result = engine.apply(state, new AssignWorkersCommand("worker", List.of(
                new WorkerAssignmentOperation(employed.getId(), AssignmentTargetType.UNEMPLOYED, "unemployed")
        )));

        assertThat(result.validation().isValid()).isTrue();
        Worker returned = result.resultingState().findWorker(employed.getId()).orElseThrow();
        assertThat(returned.getLocation()).isEqualTo(WorkerLocation.UNEMPLOYED);
        assertThat(returned.getEnterpriseId()).isNull();
        assertThat(result.resultingState().getEventLog())
                .anySatisfy(entry -> assertThat(entry.getType()).isEqualTo("WORKER_RETURNED_TO_UNEMPLOYED"));
    }

    @Test
    void cannotPartiallyFillEnterprise() {
        GameState state = CoreTestSupport.state(2);
        GameRulesEngine engine = CoreTestSupport.engine();
        Enterprise target = CoreTestSupport.firstEmptyEnterpriseWithAtLeastSlots(state, 2);
        Worker worker = CoreTestSupport.unemployedWorkers(state, ClassType.WORKER).get(0);

        var result = engine.validate(state, new AssignWorkersCommand("worker", List.of(
                new WorkerAssignmentOperation(worker.getId(), AssignmentTargetType.ENTERPRISE_SLOT, target.getId() + ":" + target.getSlots().get(0).getId())
        )));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getReasonCodes()).contains(ValidationReasonCode.ENTERPRISE_CANNOT_BE_PARTIALLY_FILLED);
    }

    @Test
    void canReassignIfWholeActionLeavesBothEnterprisesValid() {
        GameState state = CoreTestSupport.state(2);
        GameRulesEngine engine = CoreTestSupport.engine();

        Enterprise source = state.getEnterprises().stream().filter(e -> e.isFunctioning() && e.getSlots().size() >= 2).findFirst().orElseThrow();
        Enterprise target = CoreTestSupport.firstEmptyEnterpriseWithAtLeastSlots(state, 2);

        List<Worker> sourceWorkers = source.getSlots().stream()
                .map(EnterpriseSlot::getOccupiedWorkerId)
                .map(id -> state.findWorker(id).orElseThrow())
                .toList();
        sourceWorkers.forEach(worker -> worker.setTiedContract(false));

        var command = new AssignWorkersCommand("worker", List.of(
                new WorkerAssignmentOperation(sourceWorkers.get(0).getId(), AssignmentTargetType.ENTERPRISE_SLOT, target.getId() + ":" + target.getSlots().get(0).getId()),
                new WorkerAssignmentOperation(sourceWorkers.get(1).getId(), AssignmentTargetType.ENTERPRISE_SLOT, target.getId() + ":" + target.getSlots().get(1).getId())
        ));

        var result = engine.validate(state, command);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void cannotAssignWrongQualificationToSkilledSlot() {
        GameState state = CoreTestSupport.state(2);
        GameRulesEngine engine = CoreTestSupport.engine();

        Enterprise enterpriseWithSkilledSlot = state.getEnterprises().stream()
                .filter(e -> e.getSlots().stream().anyMatch(s -> s.getRequiredQualification() == WorkerQualification.SKILLED))
                .findFirst()
                .orElseThrow();
        EnterpriseSlot skilledSlot = enterpriseWithSkilledSlot.getSlots().stream()
                .filter(s -> s.getRequiredQualification() == WorkerQualification.SKILLED)
                .findFirst()
                .orElseThrow();

        Worker unskilledWorker = CoreTestSupport.unemployedWorkers(state, ClassType.WORKER).stream()
                .filter(worker -> worker.getQualificationType() == WorkerQualification.UNSKILLED)
                .findFirst()
                .orElseThrow();

        var result = engine.validate(state, new AssignWorkersCommand("worker", List.of(
                new WorkerAssignmentOperation(unskilledWorker.getId(), AssignmentTargetType.ENTERPRISE_SLOT, enterpriseWithSkilledSlot.getId() + ":" + skilledSlot.getId())
        )));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getReasonCodes()).contains(ValidationReasonCode.SLOT_QUALIFICATION_MISMATCH);
    }
}
