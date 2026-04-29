package com.example.hegemony.domain.automa.capitalist;

import java.util.Map;

public record CapitalistAutomaBonus(
        String trigger,
        String effectType,
        Map<String, Object> effectParams,
        String rawTextRu
) {
}
