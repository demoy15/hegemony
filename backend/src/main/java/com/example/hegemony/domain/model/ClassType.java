package com.example.hegemony.domain.model;

public enum ClassType {
    WORKER,
    MIDDLE_CLASS,
    CAPITALIST,
    STATE;

    public String playerId() {
        return name().toLowerCase();
    }
}
