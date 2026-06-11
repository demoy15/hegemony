package com.example.hegemony.domain.automa.worker;

import java.util.List;
import java.util.Map;

public record WorkerAutomaInstructionCard(
        String id,
        String type,
        WorkerAutomaActionSymbol appliesTo,
        String mode,
        String rawTextRu,
        List<Map<String, Object>> structuredRules,
        List<String> notes
) {
}
