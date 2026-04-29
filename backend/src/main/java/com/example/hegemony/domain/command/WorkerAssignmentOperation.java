package com.example.hegemony.domain.command;

public record WorkerAssignmentOperation(
        String workerId,
        AssignmentTargetType targetType,
        String targetId
) {
}
