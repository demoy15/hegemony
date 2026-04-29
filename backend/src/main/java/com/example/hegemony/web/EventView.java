package com.example.hegemony.web;

import com.example.hegemony.domain.event.DomainEvent;

public record EventView(String type, String description) {
    public static EventView from(DomainEvent event) {
        return new EventView(event.type(), event.description());
    }
}
