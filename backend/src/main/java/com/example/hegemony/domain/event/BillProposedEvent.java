package com.example.hegemony.domain.event;

import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;

public record BillProposedEvent(
        ClassType proposer,
        PolicyId policyId,
        PolicyCourse fromCourse,
        PolicyCourse targetCourse,
        String proposalTokenId
) implements DomainEvent {
    @Override
    public String type() {
        return "BILL_PROPOSED";
    }

    @Override
    public String description() {
        return proposer + " proposed bill on " + policyId + " from " + fromCourse + " to " + targetCourse + ".";
    }
}
