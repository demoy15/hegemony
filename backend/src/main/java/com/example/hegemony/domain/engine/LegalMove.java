package com.example.hegemony.domain.engine;

import com.example.hegemony.domain.model.ActionType;

import java.util.Collections;
import java.util.Map;

public record LegalMove(
        String id,
        ActionType actionType,
        String summary,
        boolean legacyDemo,
        Map<String, Object> template
) {
    public LegalMove(String id, ActionType actionType, String summary) {
        this(id, actionType, summary, false, Collections.emptyMap());
    }
}
