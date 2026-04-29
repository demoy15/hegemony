package com.example.hegemony.domain.event;

import com.example.hegemony.domain.model.PolicyId;

public record VoteInfluenceCommittedEvent(String actorPlayerId, PolicyId policyId, int amount) implements DomainEvent {
    @Override
    public String type() {
        return "VOTE_INFLUENCE_COMMITTED";
    }

    @Override
    public String description() {
        return actorPlayerId + " committed " + amount + " influence to vote on " + policyId + ".";
    }
}
