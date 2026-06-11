package com.example.hegemony.domain;

import com.example.hegemony.domain.command.CommitVoteInfluenceCommand;
import com.example.hegemony.domain.command.DeclareVoteStanceCommand;
import com.example.hegemony.domain.engine.GameRulesEngine;
import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.VoteResolutionResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VotingResolutionTest {
    @Test
    void votePassesOnTie() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateWithPendingVote(2, PolicyId.POLICY_3_TAXATION, PolicyCourse.B);
        state.getVotingBag().setWorker(0);
        state.getVotingBag().setMiddleClass(5);
        state.getVotingBag().setCapitalist(0);

        state = engine.apply(state, new DeclareVoteStanceCommand("capitalist", PolicyId.POLICY_3_TAXATION, "AGAINST")).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("worker", 0)).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("capitalist", 0)).resultingState();

        assertThat(state.getLastProposalResolution()).isNotNull();
        assertThat(state.getLastProposalResolution().getResult()).isEqualTo(VoteResolutionResult.PASSED);
    }

    @Test
    void voteRejectsWhenAgainstGreater() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateWithPendingVote(2, PolicyId.POLICY_3_TAXATION, PolicyCourse.B);
        state.getVotingBag().setWorker(0);
        state.getVotingBag().setMiddleClass(0);
        state.getVotingBag().setCapitalist(5);

        state = engine.apply(state, new DeclareVoteStanceCommand("capitalist", PolicyId.POLICY_3_TAXATION, "AGAINST")).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("worker", 0)).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("capitalist", 0)).resultingState();

        assertThat(state.getLastProposalResolution()).isNotNull();
        assertThat(state.getLastProposalResolution().getResult()).isEqualTo(VoteResolutionResult.REJECTED);
    }

    @Test
    void passedProposalUpdatesPolicyCourse() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateWithPendingVote(2, PolicyId.POLICY_3_TAXATION, PolicyCourse.B);
        state.getVotingBag().setWorker(0);
        state.getVotingBag().setMiddleClass(5);
        state.getVotingBag().setCapitalist(0);

        state = engine.apply(state, new DeclareVoteStanceCommand("capitalist", PolicyId.POLICY_3_TAXATION, "AGAINST")).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("worker", 0)).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("capitalist", 0)).resultingState();

        assertThat(CoreTestSupport.policy(state, PolicyId.POLICY_3_TAXATION).getCurrentCourse()).isEqualTo(PolicyCourse.B);
    }

    @Test
    void passedFiscalPolicyAddsUnlockedStateEnterpriseRows() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateWithPendingVote(2, PolicyId.POLICY_1_FISCAL, PolicyCourse.B);
        long before = state.getEnterprises().stream()
                .filter(enterprise -> enterprise.getOwnerClass() == ClassType.STATE)
                .count();
        state.getVotingBag().setWorker(5);
        state.getVotingBag().setMiddleClass(0);
        state.getVotingBag().setCapitalist(0);

        state = engine.apply(state, new DeclareVoteStanceCommand("capitalist", PolicyId.POLICY_1_FISCAL, "AGAINST")).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("worker", 0)).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("capitalist", 0)).resultingState();

        assertThat(CoreTestSupport.policy(state, PolicyId.POLICY_1_FISCAL).getCurrentCourse()).isEqualTo(PolicyCourse.B);
        assertThat(state.getEnterprises().stream()
                .filter(enterprise -> enterprise.getOwnerClass() == ClassType.STATE)
                .count()).isEqualTo(before + 3);
        assertThat(state.findEnterprise("state_hospital_2")).isPresent();
        assertThat(state.findEnterprise("state_university_2")).isPresent();
        assertThat(state.findEnterprise("state_media_2")).isPresent();
    }

    @Test
    void rejectedProposalLeavesPolicyCourseUntouched() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateWithPendingVote(2, PolicyId.POLICY_3_TAXATION, PolicyCourse.B);
        state.getVotingBag().setWorker(0);
        state.getVotingBag().setMiddleClass(0);
        state.getVotingBag().setCapitalist(5);

        state = engine.apply(state, new DeclareVoteStanceCommand("capitalist", PolicyId.POLICY_3_TAXATION, "AGAINST")).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("worker", 0)).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("capitalist", 0)).resultingState();

        assertThat(CoreTestSupport.policy(state, PolicyId.POLICY_3_TAXATION).getCurrentCourse()).isEqualTo(PolicyCourse.A);
    }

    @Test
    void committedInfluenceAddsVotesToPlayerStanceAndIsSpent() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateWithPendingVote(2, PolicyId.POLICY_3_TAXATION, PolicyCourse.B);
        state.findPlayerById("worker").orElseThrow().setInfluence(2);
        state.findPlayerById("capitalist").orElseThrow().setInfluence(1);
        state.getVotingBag().setWorker(0);
        state.getVotingBag().setMiddleClass(0);
        state.getVotingBag().setCapitalist(1);

        state = engine.apply(state, new DeclareVoteStanceCommand("capitalist", PolicyId.POLICY_3_TAXATION, "AGAINST")).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("worker", 2)).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("capitalist", 1)).resultingState();

        assertThat(state.getLastProposalResolution()).isNotNull();
        assertThat(state.getLastProposalResolution().getResult()).isEqualTo(VoteResolutionResult.PASSED);
        assertThat(state.findPlayerById("worker").orElseThrow().getInfluence()).isEqualTo(0);
        assertThat(state.findPlayerById("capitalist").orElseThrow().getInfluence()).isEqualTo(0);
    }

    @Test
    void proposalTokenReturnsToAuthorAfterResolution() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(2);
        int before = state.findPlayerById("worker").orElseThrow().availableProposalTokens();

        state = engine.apply(state, new com.example.hegemony.domain.command.ProposeBillCommand(
                "worker",
                PolicyId.POLICY_3_TAXATION,
                PolicyCourse.B
        )).resultingState();
        state = CoreTestSupport.exhaustActionPhase(state, engine);
        state = engine.apply(state, new com.example.hegemony.domain.command.ResolveProductionPhaseCommand("worker")).resultingState();
        state = engine.apply(state, new com.example.hegemony.domain.command.AdvanceToVotingCommand("worker")).resultingState();
        state.getVotingBag().setWorker(0);
        state.getVotingBag().setMiddleClass(5);
        state.getVotingBag().setCapitalist(0);

        state = engine.apply(state, new DeclareVoteStanceCommand("capitalist", PolicyId.POLICY_3_TAXATION, "AGAINST")).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("worker", 0)).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("capitalist", 0)).resultingState();

        assertThat(state.findPlayerById("worker").orElseThrow().availableProposalTokens()).isEqualTo(before);
        assertThat(CoreTestSupport.policy(state, PolicyId.POLICY_3_TAXATION).getOccupyingProposalToken()).isNull();
    }

    @Test
    void winningSupportersReceiveVpAccordingToCurrentSliceRule() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateWithPendingVote(3, PolicyId.POLICY_3_TAXATION, PolicyCourse.B);
        state.getVotingBag().setWorker(5);
        state.getVotingBag().setMiddleClass(0);
        state.getVotingBag().setCapitalist(0);

        state = engine.apply(state, new DeclareVoteStanceCommand("middle_class", PolicyId.POLICY_3_TAXATION, "AGAINST")).resultingState();
        state = engine.apply(state, new DeclareVoteStanceCommand("capitalist", PolicyId.POLICY_3_TAXATION, "FOR")).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("worker", 0)).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("middle_class", 0)).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("capitalist", 1)).resultingState();

        assertThat(state.getLastProposalResolution()).isNotNull();
        assertThat(state.getLastProposalResolution().getResult()).isEqualTo(VoteResolutionResult.PASSED);
        assertThat(state.findPlayerById("worker").orElseThrow().getVictoryPoints()).isEqualTo(3);
        assertThat(state.findPlayerById("capitalist").orElseThrow().getVictoryPoints()).isEqualTo(1);
        assertThat(state.findPlayerById("middle_class").orElseThrow().getVictoryPoints()).isEqualTo(0);
    }

    @Test
    void stateSupporterReceivesVpWhenPassedProposalUsesStateInfluence() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateWithPendingVote(4, PolicyId.POLICY_3_TAXATION, PolicyCourse.B);
        state.getVotingBag().setWorker(5);
        state.getVotingBag().setMiddleClass(0);
        state.getVotingBag().setCapitalist(0);

        state = engine.apply(state, new DeclareVoteStanceCommand("middle_class", PolicyId.POLICY_3_TAXATION, "AGAINST")).resultingState();
        state = engine.apply(state, new DeclareVoteStanceCommand("capitalist", PolicyId.POLICY_3_TAXATION, "AGAINST")).resultingState();
        state = engine.apply(state, new DeclareVoteStanceCommand("state", PolicyId.POLICY_3_TAXATION, "FOR")).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("worker", 0)).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("middle_class", 0)).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("capitalist", 0)).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("state", 1)).resultingState();

        assertThat(state.getLastProposalResolution()).isNotNull();
        assertThat(state.getLastProposalResolution().getResult()).isEqualTo(VoteResolutionResult.PASSED);
        assertThat(state.findPlayerById("state").orElseThrow().getVictoryPoints()).isEqualTo(1);
    }
}
