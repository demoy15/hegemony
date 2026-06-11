package com.example.hegemony.domain.model;

import java.util.Locale;

public enum WorkerSlotColor {
    GRAY,
    GREEN,
    BLUE,
    RED,
    WHITE,
    ORANGE,
    PURPLE;

    public static WorkerSlotColor fromRaw(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return WorkerSlotColor.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public WorkerSector toWorkerSector() {
        return switch (this) {
            case GREEN -> WorkerSector.GREEN;
            case BLUE -> WorkerSector.BLUE;
            case RED -> WorkerSector.WHITE;
            case WHITE -> WorkerSector.WHITE;
            case ORANGE -> WorkerSector.ORANGE;
            case PURPLE -> WorkerSector.PURPLE;
            case GRAY -> null;
        };
    }
}
