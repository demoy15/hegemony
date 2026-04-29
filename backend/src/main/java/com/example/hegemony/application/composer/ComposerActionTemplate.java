package com.example.hegemony.application.composer;

import com.example.hegemony.domain.model.ActionType;

import java.util.Map;

public record ComposerActionTemplate(
        ActionType actionType,
        String summary,
        boolean supported,
        String supportNote,
        Map<String, Object> template,
        boolean futureModifierSlot
) {
}
