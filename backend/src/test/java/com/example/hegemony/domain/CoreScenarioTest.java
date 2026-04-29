package com.example.hegemony.domain;

import com.example.hegemony.domain.command.AssignWorkersCommand;
import com.example.hegemony.domain.command.AssignmentTargetType;
import com.example.hegemony.domain.command.ProposeBillCommand;
import com.example.hegemony.domain.command.WorkerAssignmentOperation;
import com.example.hegemony.domain.engine.GameRulesEngine;
import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.Enterprise;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.Worker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CoreScenarioTest {
    @Test
    void proposeBill_createsPendingProposalOnPolicy() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);

        var result = engine.apply(state, new ProposeBillCommand("worker", PolicyId.POLICY_3_TAXATION, PolicyCourse.B));

        assertThat(result.validation().isValid()).isTrue();
        assertThat(CoreTestSupport.policy(result.resultingState(), PolicyId.POLICY_3_TAXATION).getOccupyingProposalToken()).isNotNull();
        assertThat(CoreTestSupport.policy(result.resultingState(), PolicyId.POLICY_3_TAXATION)
                .getOccupyingProposalToken().getTargetCourse()).isEqualTo(PolicyCourse.B);
    }

    @Test
    void assignWorkers_marksWorkersAsTied() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(2);
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
    }

    @Test
    void setupThenAssignWorkersThenLegalMovesStillConsistent() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(2);
        Enterprise target = CoreTestSupport.firstEmptyEnterpriseWithAtLeastSlots(state, 2);
        List<Worker> workers = CoreTestSupport.unemployedWorkers(state, ClassType.WORKER);

        var result = engine.apply(state, new AssignWorkersCommand("worker", List.of(
                new WorkerAssignmentOperation(workers.get(0).getId(), AssignmentTargetType.ENTERPRISE_SLOT, target.getId() + ":" + target.getSlots().get(0).getId()),
                new WorkerAssignmentOperation(workers.get(1).getId(), AssignmentTargetType.ENTERPRISE_SLOT, target.getId() + ":" + target.getSlots().get(1).getId())
        )));

        var legalMoves = engine.generateLegalMoves(result.resultingState());
        assertThat(result.validation().isValid()).isTrue();
        assertThat(legalMoves).isNotEmpty();
        assertThat(legalMoves).extracting(move -> move.id()).doesNotHaveDuplicates();
    }
}
