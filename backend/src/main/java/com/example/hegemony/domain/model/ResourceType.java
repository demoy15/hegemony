package com.example.hegemony.domain.model;

import java.util.Locale;

public enum ResourceType {
    FOOD("food"),
    LUXURY("luxury"),
    HEALTHCARE("healthcare"),
    EDUCATION("education"),
    INFLUENCE("influence"),
    MEDIA_INFLUENCE("media_influence");

    private final String id;

    ResourceType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static ResourceType fromRaw(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (ResourceType type : values()) {
            if (type.id.equals(normalized) || type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return null;
    }
}
