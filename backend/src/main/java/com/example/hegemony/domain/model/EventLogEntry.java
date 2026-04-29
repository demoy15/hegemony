package com.example.hegemony.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventLogEntry {
    private long id;
    private String type;
    private String message;

    public EventLogEntry() {
    }

    public EventLogEntry(long id, String type, String message) {
        this.id = id;
        this.type = type;
        this.message = message;
    }
}
