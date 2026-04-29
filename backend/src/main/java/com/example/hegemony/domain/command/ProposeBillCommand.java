package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;

public record ProposeBillCommand(
        String actorPlayerId,
        PolicyId policyId,
        PolicyCourse targetCourse
) implements GameCommand {
    @Override
    public ActionType type() {
        return ActionType.PROPOSE_BILL;
    }

    @Override
    public String moveId() {
        return "propose-bill:" + actorPlayerId + ":" + policyId + ":" + targetCourse;
    }

    @Override
    public String summary() {
        return "Propose bill on " + policyId + " to " + targetCourse;
    }
}
