package com.example.hegemony.application;

import com.example.hegemony.domain.event.DomainEvent;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.rules.ValidationReasonCode;

import java.util.List;

public record CommandExecutionResult(
        boolean accepted,
        List<String> errors,
        List<ValidationReasonCode> reasonCodes,
        List<DomainEvent> events,
        GameState gameState
) {
}
