package com.example.hegemony.domain.event;

public record WorkerAssignedEvent(
        String workerId,
        String enterpriseId,
        String slotId
) implements DomainEvent {
    @Override
    public String type() {
        return "WORKER_ASSIGNED";
    }

    @Override
    public String description() {
        return "worker " + workerId + " assigned to enterprise " + enterpriseId + " slot " + slotId
                + " and tied by labor contract.";
    }
}
