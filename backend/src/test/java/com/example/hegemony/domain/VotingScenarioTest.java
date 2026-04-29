package com.example.hegemony.domain;

import com.example.hegemony.bot.LegalMoveBot;
import com.example.hegemony.domain.command.CommitVoteInfluenceCommand;
import com.example.hegemony.domain.command.DeclareVoteStanceCommand;
import com.example.hegemony.domain.command.ProposeBillCommand;
import com.example.hegemony.domain.engine.GameRulesEngine;
import com.example.hegemony.domain.model.InterpretedVote;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.PlayerControlMode;
import com.example.hegemony.domain.model.RoundPhase;
import com.example.hegemony.domain.model.VoteResolutionResult;
import com.example.hegemony.domain.model.VoteStance;
import com.example.hegemony.domain.model.VotingStage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VotingScenarioTest {
    @Test
    void capitalistAutomaStanceOpposesWorkerClass() {
        GameRulesEngine engine = CoreTestSupport.engine();
        var state = CoreTestSupport.stateWithPendingVote(2, PolicyId.POLICY_3_TAXATION, PolicyCourse.B);
        state.findPlayerById("capitalist").orElseThrow().setControlMode(PlayerControlMode.AUTOMA_SIMPLE);

        var capitalistStances = engine.generateLegalCommands(state).stream()
                .filter(DeclareVoteStanceCommand.class::isInstance)
                .map(DeclareVoteStanceCommand.class::cast)
                .filter(command -> command.actorPlayerId().equals("capitalist"))
                .toList();

        assertThat(capitalistStances).singleElement()
                .extracting(DeclareVoteStanceCommand::stance)
                .isEqualTo(VoteStance.AGAINST.name());
    }

    @Test
    void automaCommitsMinimumInfluenceWhenItCanFlipLosingSideAlone() {
        GameRulesEngine engine = CoreTestSupport.engine();
        var state = CoreTestSupport.stateWithPendingVote(2, PolicyId.POLICY_2_LABOR_MARKET, PolicyCourse.A);
        state.findPlayerById("capitalist").orElseThrow().setControlMode(PlayerControlMode.AUTOMA_SIMPLE);
        state.findPlayerById("capitalist").orElseThrow().setInfluence(2);

        var session = state.getCurrentVoteState();
        session.getStanceByPlayer().put("capitalist", VoteStance.AGAINST);
        session.setVotingStage(VotingStage.COMMIT_INFLUENCE);
        session.getInterpretedVotes().put(InterpretedVote.FOR.name(), 2);
        session.getInterpretedVotes().put(InterpretedVote.AGAINST.name(), 1);

        var capitalistCommits = engine.generateLegalCommands(state).stream()
                .filter(CommitVoteInfluenceCommand.class::isInstance)
                .map(CommitVoteInfluenceCommand.class::cast)
                .filter(command -> command.actorPlayerId().equals("capitalist"))
                .toList();

        assertThat(capitalistCommits).singleElement()
                .extracting(CommitVoteInfluenceCommand::influenceAmount)
                .isEqualTo(2);
    }

    @Test
    void rejectedProposalTokenIsRemovedAndCurrentVotingPhaseAdvances() {
        GameRulesEngine engine = CoreTestSupport.engine();
        var state = CoreTestSupport.stateWithPendingVote(2, PolicyId.POLICY_3_TAXATION, PolicyCourse.B);
        state.getVotingBag().setWorker(0);
        state.getVotingBag().setMiddleClass(0);
        state.getVotingBag().setCapitalist(5);

        state = engine.apply(state, new DeclareVoteStanceCommand("capitalist", PolicyId.POLICY_3_TAXATION, "AGAINST")).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("worker", 0)).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("capitalist", 0)).resultingState();

        assertThat(state.getLastProposalResolution().getResult()).isEqualTo(VoteResolutionResult.REJECTED);
        assertThat(CoreTestSupport.policy(state, PolicyId.POLICY_3_TAXATION).getOccupyingProposalToken()).isNull();
        assertThat(state.getCurrentVoteState()).isNull();
        assertThat(state.getTurnOrder().getPhase()).isEqualTo(RoundPhase.SCORING);
    }

    @Test
    void proposeBill_then_vote_then_policyChanges() {
        GameRulesEngine engine = CoreTestSupport.engine();
        var state = CoreTestSupport.state(2);

        state = engine.apply(state, new ProposeBillCommand("worker", PolicyId.POLICY_3_TAXATION, PolicyCourse.B)).resultingState();
        state = CoreTestSupport.exhaustActionPhase(state, engine);
        state = engine.apply(state, new com.example.hegemony.domain.command.ResolveProductionPhaseCommand("worker")).resultingState();
        state = engine.apply(state, new com.example.hegemony.domain.command.AdvanceToVotingCommand("worker")).resultingState();
        state.getVotingBag().setWorker(0);
        state.getVotingBag().setMiddleClass(5);
        state.getVotingBag().setCapitalist(0);

        state = engine.apply(state, new DeclareVoteStanceCommand("capitalist", PolicyId.POLICY_3_TAXATION, "AGAINST")).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("worker", 0)).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("capitalist", 0)).resultingState();

        assertThat(CoreTestSupport.policy(state, PolicyId.POLICY_3_TAXATION).getCurrentCourse()).isEqualTo(PolicyCourse.B);
        assertThat(CoreTestSupport.policy(state, PolicyId.POLICY_3_TAXATION).getOccupyingProposalToken()).isNull();
    }

    @Test
    void multiplePendingProposalsResolveInPolicyOrder() {
        GameRulesEngine engine = CoreTestSupport.engine();
        var state = CoreTestSupport.state(2);

        state = engine.apply(state, new ProposeBillCommand("worker", PolicyId.POLICY_3_TAXATION, PolicyCourse.B)).resultingState();
        state = engine.apply(state, new ProposeBillCommand("worker", PolicyId.POLICY_1_FISCAL, PolicyCourse.B)).resultingState();
        state = CoreTestSupport.exhaustActionPhase(state, engine);
        state = engine.apply(state, new com.example.hegemony.domain.command.ResolveProductionPhaseCommand("worker")).resultingState();
        state = engine.apply(state, new com.example.hegemony.domain.command.AdvanceToVotingCommand("worker")).resultingState();

        assertThat(state.getCurrentVoteState()).isNotNull();
        assertThat(state.getCurrentVoteState().getActiveProposalPolicyId()).isEqualTo(PolicyId.POLICY_1_FISCAL);

        state.getVotingBag().setWorker(0);
        state.getVotingBag().setMiddleClass(5);
        state.getVotingBag().setCapitalist(0);
        state = engine.apply(state, new DeclareVoteStanceCommand("capitalist", PolicyId.POLICY_1_FISCAL, "AGAINST")).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("worker", 0)).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("capitalist", 0)).resultingState();

        assertThat(state.getCurrentVoteState()).isNotNull();
        assertThat(state.getCurrentVoteState().getActiveProposalPolicyId()).isEqualTo(PolicyId.POLICY_3_TAXATION);

        state.getVotingBag().setWorker(0);
        state.getVotingBag().setMiddleClass(5);
        state.getVotingBag().setCapitalist(0);
        state = engine.apply(state, new DeclareVoteStanceCommand("capitalist", PolicyId.POLICY_3_TAXATION, "AGAINST")).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("worker", 0)).resultingState();
        state = engine.apply(state, new CommitVoteInfluenceCommand("capitalist", 0)).resultingState();

        assertThat(state.pendingPoliciesInOrder()).isEmpty();
        assertThat(state.getCurrentVoteState()).isNull();
        assertThat(state.getLastProposalResolution()).isNotNull();
        assertThat(state.getLastProposalResolution().getPolicyId()).isEqualTo(PolicyId.POLICY_3_TAXATION);
    }

    @Test
    void voteFlow_botUsesOnlyLegalCommands() {
        GameRulesEngine engine = CoreTestSupport.engine();
        LegalMoveBot bot = new LegalMoveBot();
        var state = CoreTestSupport.stateWithPendingVote(2, PolicyId.POLICY_3_TAXATION, PolicyCourse.B);

        int guard = 0;
        while (state.getTurnOrder().getPhase() == RoundPhase.VOTING && guard++ < 20) {
            var decision = bot.chooseMove(state, engine);
            var validation = engine.validate(state, decision.selectedCommand());
            assertThat(validation.isValid()).isTrue();

            var applied = engine.apply(state, decision.selectedCommand());
            assertThat(applied.validation().isValid()).isTrue();
            state = applied.resultingState();
        }

        assertThat(guard).isLessThan(21);
        assertThat(state.getTurnOrder().getPhase()).isEqualTo(RoundPhase.SCORING);
        assertThat(state.getLastProposalResolution()).isNotNull();
    }
}
