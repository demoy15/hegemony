package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record AssignWorkersCommand(
        String actorPlayerId,
        List<WorkerAssignmentOperation> assignments
) implements GameCommand {
    public AssignWorkersCommand {
        assignments = assignments == null ? List.of() : List.copyOf(assignments);
    }

    @Override
    public ActionType type() {
        return ActionType.ASSIGN_WORKERS;
    }

    @Override
    public String moveId() {
        String workerIds = assignments.stream().map(WorkerAssignmentOperation::workerId).collect(Collectors.joining(","));
        return "assign-workers:" + actorPlayerId + ":" + workerIds;
    }

    @Override
    public String summary() {
        return "Assign " + assignments.size() + " worker(s)";
    }

    public List<WorkerAssignmentOperation> mutableAssignments() {
        return new ArrayList<>(assignments);
    }
}
