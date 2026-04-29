package com.example.hegemony.domain.event;

import com.example.hegemony.domain.model.PolicyId;

public record VotingCubesDrawnEvent(PolicyId policyId, int cubesDrawn) implements DomainEvent {
    @Override
    public String type() {
        return "VOTING_CUBES_DRAWN";
    }

    @Override
    public String description() {
        return "Drawn " + cubesDrawn + " cubes for vote on " + policyId + ".";
    }
}
