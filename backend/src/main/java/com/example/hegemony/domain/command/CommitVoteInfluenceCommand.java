package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;

public record CommitVoteInfluenceCommand(
        String actorPlayerId,
        int influenceAmount
) implements GameCommand {
    @Override
    public ActionType type() {
        return ActionType.COMMIT_VOTE_INFLUENCE;
    }

    @Override
    public String moveId() {
        return "commit-vote-influence:" + actorPlayerId + ":" + influenceAmount;
    }

    @Override
    public String summary() {
        return "Commit influence for current vote";
    }
}
