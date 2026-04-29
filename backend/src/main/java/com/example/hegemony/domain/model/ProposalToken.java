package com.example.hegemony.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProposalToken {
    private String id;
    private String ownerPlayerId;
    private ClassType ownerClass;
    private boolean available = true;
    private PolicyCourse targetCourse;
    private PolicyId policyId;

    public ProposalToken() {
    }

    public ProposalToken(
            String id,
            String ownerPlayerId,
            ClassType ownerClass,
            boolean available,
            PolicyCourse targetCourse,
            PolicyId policyId
    ) {
        this.id = id;
        this.ownerPlayerId = ownerPlayerId;
        this.ownerClass = ownerClass;
        this.available = available;
        this.targetCourse = targetCourse;
        this.policyId = policyId;
    }

    public ProposalToken copy() {
        return new ProposalToken(id, ownerPlayerId, ownerClass, available, targetCourse, policyId);
    }
}
