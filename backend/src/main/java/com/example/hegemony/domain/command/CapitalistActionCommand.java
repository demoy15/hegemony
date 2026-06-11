package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;

import java.util.Map;

public record CapitalistActionCommand(
        ActionType actionType,
        String actorPlayerId,
        Map<String, Object> parameters
) implements GameCommand {
    @Override
    public ActionType type() {
        return actionType;
    }

    @Override
    public String moveId() {
        String target = targetId();
        return actionType.name().toLowerCase() + ":" + actorPlayerId + (target.isBlank() ? "" : ":" + target);
    }

    @Override
    public String summary() {
        return "Capitalist action " + actionType + " by " + actorPlayerId + ".";
    }

    private String targetId() {
        if (parameters == null || parameters.isEmpty()) {
            return "";
        }
        for (String key : java.util.List.of("enterpriseId", "dealId", "resourceType")) {
            Object value = parameters.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value);
            }
        }
        return "";
    }
}
