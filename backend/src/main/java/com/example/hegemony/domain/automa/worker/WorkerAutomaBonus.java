package com.example.hegemony.domain.automa.worker;

import java.util.Map;

public record WorkerAutomaBonus(
        String trigger,
        String effectType,
        Map<String, Object> effectParams,
        String rawTextRu
) {
}
