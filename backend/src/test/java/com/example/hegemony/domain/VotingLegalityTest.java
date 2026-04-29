package com.example.hegemony.domain;

import com.example.hegemony.domain.command.CommitVoteInfluenceCommand;
import com.example.hegemony.domain.command.DeclareVoteStanceCommand;
import com.example.hegemony.domain.engine.GameRulesEngine;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.rules.ValidationReasonCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VotingLegalityTest {
    @Test
    void proposerMustBeFor() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateWithPendingVote(2, PolicyId.POLICY_3_TAXATION, PolicyCourse.B);

        var result = engine.validate(state, new DeclareVoteStanceCommand("worker", PolicyId.POLICY_3_TAXATION, "AGAINST"));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getReasonCodes()).contains(ValidationReasonCode.PROPOSER_CANNOT_VOTE_AGAINST);
    }

    @Test
    void cannotAbstain() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateWithPendingVote(2, PolicyId.POLICY_3_TAXATION, PolicyCourse.B);

        var result = engine.validate(state, new DeclareVoteStanceCommand("capitalist", PolicyId.POLICY_3_TAXATION, "ABSTAIN"));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getReasonCodes()).contains(ValidationReasonCode.INVALID_STANCE);
    }

    @Test
    void cannotSubmitStanceTwice() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateWithPendingVote(2, PolicyId.POLICY_3_TAXATION, PolicyCourse.B);

        var first = engine.apply(state, new DeclareVoteStanceCommand("capitalist", PolicyId.POLICY_3_TAXATION, "AGAINST"));
        assertThat(first.validation().isValid()).isTrue();

        var secondValidation = engine.validate(first.resultingState(), new DeclareVoteStanceCommand("capitalist", PolicyId.POLICY_3_TAXATION, "AGAINST"));
        assertThat(secondValidation.isValid()).isFalse();
        assertThat(secondValidation.getReasonCodes()).contains(ValidationReasonCode.STANCE_ALREADY_SUBMITTED);
    }

    @Test
    void cannotCommitMoreInfluenceThanAvailable() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateWithPendingVote(2, PolicyId.POLICY_3_TAXATION, PolicyCourse.B);
        GameState commitStage = engine.apply(state, new DeclareVoteStanceCommand("capitalist", PolicyId.POLICY_3_TAXATION, "AGAINST")).resultingState();

        int available = commitStage.findPlayerById("capitalist").orElseThrow().getInfluence();
        var result = engine.validate(commitStage, new CommitVoteInfluenceCommand("capitalist", available + 1));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getReasonCodes()).contains(ValidationReasonCode.INFLUENCE_EXCEEDS_AVAILABLE);
    }

    @Test
    void cannotCommitInfluenceBeforeCubeDrawStage() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateWithPendingVote(2, PolicyId.POLICY_3_TAXATION, PolicyCourse.B);

        var result = engine.validate(state, new CommitVoteInfluenceCommand("worker", 0));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getReasonCodes()).contains(ValidationReasonCode.NOT_CURRENT_VOTING_STAGE);
    }

    @Test
    void cannotVoteOutsideVotingPhase() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(2);

        var result = engine.validate(state, new DeclareVoteStanceCommand("worker", PolicyId.POLICY_3_TAXATION, "FOR"));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getReasonCodes()).contains(ValidationReasonCode.NOT_IN_VOTING_PHASE);
    }
}

