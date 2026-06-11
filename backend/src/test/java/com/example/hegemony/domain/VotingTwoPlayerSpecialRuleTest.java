package com.example.hegemony.domain;

import com.example.hegemony.domain.command.CommitVoteInfluenceCommand;
import com.example.hegemony.domain.command.DeclareVoteStanceCommand;
import com.example.hegemony.domain.command.DrawVotingCubesCommand;
import com.example.hegemony.domain.engine.GameRulesEngine;
import com.example.hegemony.domain.model.InterpretedVote;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.VoteResolutionResult;
import com.example.hegemony.domain.model.VotingCubeOwnerClass;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VotingTwoPlayerSpecialRuleTest {
    @Test
    void neutralMiddleClassCubesAreDiscarded() {
        GameRulesEngine engine = CoreTestSupport.engine();
        var state = CoreTestSupport.stateWithPendingVote(2, PolicyId.POLICY_3_TAXATION, PolicyCourse.B);
        state.getVotingBag().setWorker(0);
        state.getVotingBag().setMiddleClass(5);
        state.getVotingBag().setCapitalist(0);

        state = engine.apply(state, new DeclareVoteStanceCommand("capitalist", PolicyId.POLICY_3_TAXATION, "AGAINST")).resultingState();
        state = engine.apply(state, new DrawVotingCubesCommand("worker", 5)).resultingState();

        assertThat(state.getCurrentVoteState()).isNotNull();
        assertThat(state.getCurrentVoteState().getDrawnVotingCubes())
                .allSatisfy(cube -> {
                    assertThat(cube.getOwnerClass()).isEqualTo(VotingCubeOwnerClass.MIDDLE_CLASS);
                    assertThat(cube.getInterpretedVote()).isEqualTo(InterpretedVote.NEUTRAL);
                });

        state = engine.apply(state, new CommitVoteInfluenceCommand("worker", 0)).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("capitalist", 0)).resultingState();

        assertThat(state.getVotingBag().getMiddleClass()).isEqualTo(0);
    }

    @Test
    void allNeutralDrawAndNoInfluenceStillPasses() {
        GameRulesEngine engine = CoreTestSupport.engine();
        var state = CoreTestSupport.stateWithPendingVote(2, PolicyId.POLICY_3_TAXATION, PolicyCourse.B);
        state.getVotingBag().setWorker(0);
        state.getVotingBag().setMiddleClass(5);
        state.getVotingBag().setCapitalist(0);

        state = engine.apply(state, new DeclareVoteStanceCommand("capitalist", PolicyId.POLICY_3_TAXATION, "AGAINST")).resultingState();
        state = engine.apply(state, new DrawVotingCubesCommand("worker", 5)).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("worker", 0)).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("capitalist", 0)).resultingState();

        assertThat(state.getLastProposalResolution()).isNotNull();
        assertThat(state.getLastProposalResolution().getResult()).isEqualTo(VoteResolutionResult.PASSED);
    }
}
