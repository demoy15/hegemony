package com.example.hegemony.domain.engine;

import com.example.hegemony.domain.event.DomainEvent;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.rules.ValidationResult;

import java.util.List;

public record ApplyCommandResult(
        ValidationResult validation,
        GameState resultingState,
        List<DomainEvent> producedEvents
) {
}
