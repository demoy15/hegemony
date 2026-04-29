package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;
import com.example.hegemony.domain.model.WorkerSlotColor;

public record ConsumeEducationCommand(String actorPlayerId, String workerId, WorkerSlotColor targetColor) implements GameCommand {
    public ConsumeEducationCommand(String actorPlayerId) {
        this(actorPlayerId, "", WorkerSlotColor.WHITE);
    }

    @Override
    public ActionType type() {
        return ActionType.CONSUME_EDUCATION;
    }

    @Override
    public String moveId() {
        return "consume-education:" + actorPlayerId + ":" + workerId + ":" + targetColor;
    }

    @Override
    public String summary() {
        return "Consume education for current population and train one worker";
    }
}
