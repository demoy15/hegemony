package com.example.hegemony.domain.event;

public interface DomainEvent {
    String type();

    String description();
}
