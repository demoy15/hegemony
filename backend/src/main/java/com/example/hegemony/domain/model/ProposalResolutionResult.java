package com.example.hegemony.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProposalResolutionResult {
    private PolicyId policyId;
    private VoteResolutionResult result = VoteResolutionResult.PENDING;
    private PolicyCourse fromCourse;
    private PolicyCourse targetCourse;

    public ProposalResolutionResult() {
    }

    public ProposalResolutionResult(PolicyId policyId, VoteResolutionResult result, PolicyCourse fromCourse, PolicyCourse targetCourse) {
        this.policyId = policyId;
        this.result = result;
        this.fromCourse = fromCourse;
        this.targetCourse = targetCourse;
    }

    public ProposalResolutionResult copy() {
        return new ProposalResolutionResult(policyId, result, fromCourse, targetCourse);
    }
}
