package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;

public record AddVotingCubesCommand(String actorPlayerId) implements GameCommand {
    @Override
    public ActionType type() {
        return ActionType.ADD_VOTING_CUBES;
    }

    @Override
    public String moveId() {
        return "add-voting-cubes:" + actorPlayerId;
    }

    @Override
    public String summary() {
        return "Add 3 voting cubes for " + actorPlayerId + ".";
    }
}
