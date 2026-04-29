package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;
import com.example.hegemony.domain.model.PolicyId;

public record DeclareVoteStanceCommand(
        String actorPlayerId,
        PolicyId policyId,
        String stance
) implements GameCommand {
    @Override
    public ActionType type() {
        return ActionType.DECLARE_VOTE_STANCE;
    }

    @Override
    public String moveId() {
        return "declare-vote-stance:" + actorPlayerId + ":" + policyId + ":" + stance;
    }

    @Override
    public String summary() {
        return "Declare vote stance for " + policyId;
    }
}
