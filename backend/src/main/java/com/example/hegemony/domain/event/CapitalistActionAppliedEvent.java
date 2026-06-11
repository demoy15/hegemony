package com.example.hegemony.domain.event;

public record CapitalistActionAppliedEvent(String type, String description) implements DomainEvent {
}
