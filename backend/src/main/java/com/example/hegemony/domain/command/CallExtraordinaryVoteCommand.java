package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;
import com.example.hegemony.domain.model.PolicyId;

public record CallExtraordinaryVoteCommand(
        String actorPlayerId,
        PolicyId policyId
) implements GameCommand {
    @Override
    public ActionType type() {
        return ActionType.CALL_EXTRAORDINARY_VOTE;
    }

    @Override
    public String moveId() {
        return "call-extraordinary-vote:" + actorPlayerId + ":" + policyId;
    }

    @Override
    public String summary() {
        return "Call extraordinary vote on " + policyId + ".";
    }
}
