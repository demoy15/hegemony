package com.example.hegemony.domain.event;

import com.example.hegemony.domain.model.PolicyId;

public record VotingPhaseStartedEvent(int round, int proposalsCount, PolicyId firstPolicy) implements DomainEvent {
    @Override
    public String type() {
        return "VOTING_PHASE_STARTED";
    }

    @Override
    public String description() {
        return "Voting phase started in round " + round + " with " + proposalsCount + " pending proposal(s).";
    }
}
