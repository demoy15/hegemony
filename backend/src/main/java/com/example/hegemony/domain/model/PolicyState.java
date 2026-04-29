package com.example.hegemony.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PolicyState {
    private PolicyId id;
    private PolicyCourse currentCourse;
    private ProposalToken occupyingProposalToken;
    private boolean locked;

    public PolicyState() {
    }

    public PolicyState(PolicyId id, PolicyCourse currentCourse, ProposalToken occupyingProposalToken, boolean locked) {
        this.id = id;
        this.currentCourse = currentCourse;
        this.occupyingProposalToken = occupyingProposalToken;
        this.locked = locked;
    }

    public PolicyState copy() {
        return new PolicyState(
                id,
                currentCourse,
                occupyingProposalToken == null ? null : occupyingProposalToken.copy(),
                locked
        );
    }
}
