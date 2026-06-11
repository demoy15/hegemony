package com.example.hegemony.domain;

import com.example.hegemony.domain.command.AddVotingCubesCommand;
import com.example.hegemony.domain.command.CallExtraordinaryVoteCommand;
import com.example.hegemony.domain.command.CommitVoteInfluenceCommand;
import com.example.hegemony.domain.command.DeclareVoteStanceCommand;
import com.example.hegemony.domain.command.DrawVotingCubesCommand;
import com.example.hegemony.domain.command.ProposeBillCommand;
import com.example.hegemony.domain.engine.GameRulesEngine;
import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.VotingStage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VotingBagRefillTest {
    @Test
    void humanActionAddsThreeOwnVotingCubesImmediately() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(2);
        state.getVotingBag().setWorker(1);

        var result = engine.apply(state, new AddVotingCubesCommand("worker"));

        assertThat(result.validation().isValid()).isTrue();
        assertThat(result.resultingState().getVotingBag().getWorker()).isEqualTo(4);
    }

    @Test
    void extraordinaryVoteStartsSessionThenResolvesWithoutRefillingBagAndKeepsActionPhase() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(2);
        state.findPlayerById("worker").orElseThrow().setInfluence(1);
        state.getVotingBag().setWorker(0);
        state.getVotingBag().setMiddleClass(5);
        state.getVotingBag().setCapitalist(0);

        state = engine.apply(state, new ProposeBillCommand("worker", PolicyId.POLICY_3_TAXATION, PolicyCourse.B)).resultingState();
        var result = engine.apply(state, new CallExtraordinaryVoteCommand("worker", PolicyId.POLICY_3_TAXATION));

        assertThat(result.validation().isValid()).isTrue();
        GameState vote = result.resultingState();
        assertThat(vote.getCurrentPhase()).isEqualTo(com.example.hegemony.domain.model.RoundPhase.ACTIONS);
        assertThat(vote.findPlayerById("worker").orElseThrow().getInfluence()).isEqualTo(0);
        assertThat(vote.getCurrentVoteState()).isNotNull();
        assertThat(vote.getCurrentVoteState().isExtraordinary()).isTrue();
        assertThat(vote.getCurrentVoteState().getVotingStage()).isEqualTo(VotingStage.DRAW_BAG_CUBES);
        assertThat(vote.getCurrentVoteState().getStanceByPlayer())
                .containsEntry("worker", com.example.hegemony.domain.model.VoteStance.FOR)
                .containsEntry("capitalist", com.example.hegemony.domain.model.VoteStance.AGAINST);

        vote = engine.apply(vote, new DrawVotingCubesCommand("worker", 5)).resultingState();
        assertThat(vote.getCurrentVoteState().getVotingStage()).isEqualTo(VotingStage.COMMIT_INFLUENCE);
        assertThat(vote.getCurrentVoteState().getDrawnVotingCubes()).hasSize(5);

        vote = engine.apply(vote, new CommitVoteInfluenceCommand("worker", 0)).resultingState();
        vote = engine.apply(vote, new CommitVoteInfluenceCommand("capitalist", 0)).resultingState();

        assertThat(vote.getCurrentPhase()).isEqualTo(com.example.hegemony.domain.model.RoundPhase.ACTIONS);
        assertThat(CoreTestSupport.policy(vote, PolicyId.POLICY_3_TAXATION).getCurrentCourse()).isEqualTo(PolicyCourse.B);
        assertThat(CoreTestSupport.policy(vote, PolicyId.POLICY_3_TAXATION).getOccupyingProposalToken()).isNull();
        assertThat(vote.getVotingBag().getMiddleClass()).isEqualTo(5);
    }

    @Test
    void refillBag_2Players_includesNeutralMiddleClassRule() {
        GameState votingState = CoreTestSupport.stateWithPendingVote(2, PolicyId.POLICY_3_TAXATION, PolicyCourse.B);
        int expectedWorker = ceilHalf(votingState.findPlayerById("worker").orElseThrow().getPopulation());
        int expectedCapitalist = ceilHalf((int) votingState.getEnterprises().stream()
                .filter(e -> e.getOwnerClass() == ClassType.CAPITALIST)
                .filter(e -> e.isFunctioning())
                .count());
        int expectedMiddleClass = 5;

        assertThat(votingState.getVotingBag().getWorker()).isEqualTo(expectedWorker);
        assertThat(votingState.getVotingBag().getCapitalist()).isEqualTo(expectedCapitalist);
        assertThat(votingState.getVotingBag().getMiddleClass()).isEqualTo(expectedMiddleClass);
    }

    @Test
    void extraordinaryVoteMustBeCalledImmediatelyAfterProposal() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(2);
        state.findPlayerById("worker").orElseThrow().setInfluence(2);

        state = engine.apply(state, new ProposeBillCommand("worker", PolicyId.POLICY_3_TAXATION, PolicyCourse.B)).resultingState();
        state = engine.apply(state, new AddVotingCubesCommand("worker")).resultingState();

        var result = engine.validate(state, new CallExtraordinaryVoteCommand("worker", PolicyId.POLICY_3_TAXATION));

        assertThat(result.isValid()).isFalse();
    }

    @Test
    void refillBag_3Players_usesPopulationAndFunctioningEnterpriseRules() {
        GameState votingState = CoreTestSupport.stateWithPendingVote(3, PolicyId.POLICY_3_TAXATION, PolicyCourse.B);
        int expectedWorker = ceilHalf(votingState.findPlayerById("worker").orElseThrow().getPopulation());
        int expectedCapitalist = ceilHalf((int) votingState.getEnterprises().stream()
                .filter(e -> e.getOwnerClass() == ClassType.CAPITALIST)
                .filter(e -> e.isFunctioning())
                .count());

        int middlePopulationTerm = ceilHalf(votingState.findPlayerById("middle_class").orElseThrow().getPopulation());
        int middleFunctioningTerm = ceilHalf((int) votingState.getEnterprises().stream()
                .filter(e -> e.getOwnerClass() == ClassType.MIDDLE_CLASS)
                .filter(e -> e.isFunctioning())
                .count());
        int expectedMiddleClass = Math.max(middlePopulationTerm, middleFunctioningTerm);

        assertThat(votingState.getVotingBag().getWorker()).isEqualTo(expectedWorker);
        assertThat(votingState.getVotingBag().getCapitalist()).isEqualTo(expectedCapitalist);
        assertThat(votingState.getVotingBag().getMiddleClass()).isEqualTo(expectedMiddleClass);
    }

    @Test
    void emptyBag_drawTriggersDoubleRefill() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateWithPendingVote(2, PolicyId.POLICY_3_TAXATION, PolicyCourse.B);
        state.getVotingBag().setWorker(0);
        state.getVotingBag().setMiddleClass(0);
        state.getVotingBag().setCapitalist(0);

        var declared = engine.apply(state, new DeclareVoteStanceCommand("capitalist", PolicyId.POLICY_3_TAXATION, "AGAINST"));
        assertThat(declared.validation().isValid()).isTrue();

        GameState afterDraw = engine.apply(declared.resultingState(), new DrawVotingCubesCommand("worker", 5)).resultingState();
        int workerGain = ceilHalf(afterDraw.findPlayerById("worker").orElseThrow().getPopulation());
        int capitalistGain = ceilHalf((int) afterDraw.getEnterprises().stream()
                .filter(e -> e.getOwnerClass() == ClassType.CAPITALIST)
                .filter(e -> e.isFunctioning())
                .count());
        int expectedTotalAfterDoubleRefillAndDraw = (2 * (workerGain + capitalistGain + 5)) - 5;

        assertThat(afterDraw.getCurrentVoteState()).isNotNull();
        assertThat(afterDraw.getCurrentVoteState().getDrawnVotingCubes()).hasSize(5);
        assertThat(afterDraw.getVotingBag().totalCubes()).isEqualTo(expectedTotalAfterDoubleRefillAndDraw);
        assertThat(afterDraw.getEventLog()).anyMatch(entry ->
                "VOTING_CUBES_DRAWN".equals(entry.getType())
                        && (entry.getMessage().contains("WORKER=")
                        || entry.getMessage().contains("MIDDLE_CLASS=")
                        || entry.getMessage().contains("CAPITALIST="))
                        && entry.getMessage().contains("votes FOR=")
                        && entry.getMessage().contains("AGAINST=")
                        && entry.getMessage().contains("NEUTRAL="));
    }

    @Test
    void humanCanChooseHowManyVotingCubesToDraw() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateWithPendingVote(2, PolicyId.POLICY_3_TAXATION, PolicyCourse.B);
        state = engine.apply(state, new DeclareVoteStanceCommand("capitalist", PolicyId.POLICY_3_TAXATION, "AGAINST")).resultingState();

        int totalBefore = state.getVotingBag().totalCubes();
        var result = engine.apply(state, new DrawVotingCubesCommand("worker", 3));

        assertThat(result.validation().isValid()).isTrue();
        assertThat(result.resultingState().getCurrentVoteState().getDrawnVotingCubes()).hasSize(3);
        assertThat(result.resultingState().getVotingBag().totalCubes()).isEqualTo(totalBefore - 3);
        assertThat(result.resultingState().getCurrentVoteState().getVotingStage()).isEqualTo(VotingStage.COMMIT_INFLUENCE);
    }

    private static int ceilHalf(int value) {
        return (value + 1) / 2;
    }
}
