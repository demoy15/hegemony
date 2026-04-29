package com.example.hegemony.domain.model;

import java.util.Locale;

public enum WorkerSlotColor {
    GRAY,
    GREEN,
    BLUE,
    WHITE,
    RED,
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
            case WHITE -> WorkerSector.WHITE;
            case RED -> WorkerSector.RED;
            case ORANGE -> WorkerSector.ORANGE;
            case PURPLE -> WorkerSector.PURPLE;
            case GRAY -> null;
        };
    }
}
