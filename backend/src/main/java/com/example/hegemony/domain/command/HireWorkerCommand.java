package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;

public record HireWorkerCommand(int count) implements GameCommand {
    @Override
    public ActionType type() {
        return ActionType.HIRE_WORKER;
    }

    @Override
    public String moveId() {
        return "hire-worker:" + count;
    }

    @Override
    public String summary() {
        return "Hire " + count + " worker(s)";
    }
}
