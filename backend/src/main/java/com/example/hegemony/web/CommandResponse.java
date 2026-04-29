package com.example.hegemony.web;

import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.rules.ValidationReasonCode;

import java.util.List;

public record CommandResponse(
        boolean accepted,
        List<String> errors,
        List<ValidationReasonCode> reasonCodes,
        List<EventView> events,
        GameState gameState
) {
}
