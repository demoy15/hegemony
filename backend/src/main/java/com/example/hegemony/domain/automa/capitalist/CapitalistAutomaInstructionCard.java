package com.example.hegemony.domain.automa.capitalist;

import java.util.List;
import java.util.Map;

public record CapitalistAutomaInstructionCard(
        String id,
        CapitalistAutomaInstructionType type,
        CapitalistAutomaActionSymbol appliesTo,
        CapitalistAutomaInstructionMode mode,
        String rawTextRu,
        List<Map<String, Object>> structuredRules,
        List<String> notes
) {
    public boolean appliesTo(CapitalistAutomaActionSymbol symbol, CapitalistAutomaExecutionMode executionMode) {
        if (symbol == null || executionMode == null || !mode.supports(executionMode)) {
            return false;
        }
        return appliesTo == null || appliesTo == symbol;
    }
}
