package com.example.hegemony.domain.automa.capitalist;

import com.example.hegemony.domain.command.GameCommand;

import java.util.Map;

public record CapitalistAutomaInterpretationResult(
        GameCommand selectedCommand,
        boolean cardDrivenSelection,
        boolean usedSpecialActionFallback,
        boolean bonusApplied,
        Map<String, Object> trace,
        String rationale
) {
}
