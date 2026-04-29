package com.example.hegemony.domain.event;

import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.VoteResolutionResult;

public record VoteResolvedEvent(
        PolicyId policyId,
        VoteResolutionResult result,
        PolicyCourse fromCourse,
        PolicyCourse targetCourse,
        int totalFor,
        int totalAgainst
) implements DomainEvent {
    @Override
    public String type() {
        return "VOTE_RESOLVED";
    }

    @Override
    public String description() {
        return "Vote on " + policyId + " resolved as " + result + " (" + totalFor + " FOR vs " + totalAgainst + " AGAINST).";
    }
}
