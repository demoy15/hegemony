package com.example.hegemony.domain.carddata;

import com.example.hegemony.domain.model.ClassType;

import java.util.List;
import java.util.Map;

public record BotActionCardDefinition(
        String botCardId,
        ClassType supportedClass,
        List<String> orderedActionCandidates,
        String firstActionBonus,
        List<String> policyPreferences,
        String specialAction,
        int influenceValue,
        String supportedExecutionStatus,
        Map<String, Object> metadata
) {
}
