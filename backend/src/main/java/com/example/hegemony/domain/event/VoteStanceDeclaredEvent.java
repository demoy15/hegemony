package com.example.hegemony.domain.event;

import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.VoteStance;

public record VoteStanceDeclaredEvent(String actorPlayerId, PolicyId policyId, VoteStance stance) implements DomainEvent {
    @Override
    public String type() {
        return "VOTE_STANCE_DECLARED";
    }

    @Override
    public String description() {
        return actorPlayerId + " declared " + stance + " for " + policyId + ".";
    }
}
