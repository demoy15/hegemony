package com.example.hegemony.domain;

import com.example.hegemony.domain.command.ProposeBillCommand;
import com.example.hegemony.domain.engine.GameRulesEngine;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.ProposalToken;
import com.example.hegemony.domain.rules.ValidationReasonCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProposeBillLegalityTest {
    @Test
    void canProposeAdjacentCourse() {
        GameState state = CoreTestSupport.state(4);
        GameRulesEngine engine = CoreTestSupport.engine();

        var result = engine.validate(state, new ProposeBillCommand("worker", PolicyId.POLICY_3_TAXATION, PolicyCourse.B));

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void canProposeNonAdjacentCourse() {
        GameState state = CoreTestSupport.state(4);
        GameRulesEngine engine = CoreTestSupport.engine();

        var result = engine.validate(state, new ProposeBillCommand("worker", PolicyId.POLICY_3_TAXATION, PolicyCourse.C));

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void cannotProposeWhenPolicyAlreadyHasProposal() {
        GameState state = CoreTestSupport.state(4);
        GameRulesEngine engine = CoreTestSupport.engine();
        CoreTestSupport.policy(state, PolicyId.POLICY_3_TAXATION)
                .setOccupyingProposalToken(new ProposalToken(
                        "t1",
                        "worker",
                        state.currentPlayer().getClassType(),
                        false,
                        PolicyCourse.B,
                        PolicyId.POLICY_3_TAXATION
                ));

        var result = engine.validate(state, new ProposeBillCommand("worker", PolicyId.POLICY_3_TAXATION, PolicyCourse.B));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getReasonCodes()).contains(ValidationReasonCode.POLICY_ALREADY_HAS_PROPOSAL);
    }

    @Test
    void cannotProposeWithoutAvailableToken() {
        GameState state = CoreTestSupport.state(4);
        GameRulesEngine engine = CoreTestSupport.engine();
        state.findPlayerById("worker").orElseThrow().getProposalTokens().forEach(token -> token.setAvailable(false));

        var result = engine.validate(state, new ProposeBillCommand("worker", PolicyId.POLICY_3_TAXATION, PolicyCourse.B));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getReasonCodes()).contains(ValidationReasonCode.NO_AVAILABLE_PROPOSAL_TOKEN);
    }

    @Test
    void cannotProposeOutOfTurn() {
        GameState state = CoreTestSupport.state(4);
        GameRulesEngine engine = CoreTestSupport.engine();

        var result = engine.validate(state, new ProposeBillCommand("capitalist", PolicyId.POLICY_3_TAXATION, PolicyCourse.B));

        assertThat(result.isValid()).isFalse();
        assertThat(result.getReasonCodes()).contains(ValidationReasonCode.NOT_CURRENT_PLAYER);
    }
}
