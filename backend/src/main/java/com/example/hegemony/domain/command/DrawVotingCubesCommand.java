package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;

public record DrawVotingCubesCommand(String actorPlayerId, int count) implements GameCommand {
    @Override
    public ActionType type() {
        return ActionType.DRAW_VOTING_CUBES;
    }

    @Override
    public String moveId() {
        return "draw-voting-cubes:" + actorPlayerId + ":" + count;
    }

    @Override
    public String summary() {
        return "Draw " + count + " voting cubes from the bag.";
    }
}
