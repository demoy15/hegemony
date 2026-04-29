package com.example.hegemony.domain.event;

import com.example.hegemony.domain.model.PlayerRole;

public record WorkerHiredEvent(PlayerRole role, int workers, int totalCost) implements DomainEvent {
    @Override
    public String type() {
        return "WORKER_HIRED";
    }

    @Override
    public String description() {
        return role + " hired " + workers + " worker(s) for $" + totalCost + ".";
    }
}
