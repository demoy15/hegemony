package com.example.hegemony.domain.automa.worker;

import java.util.List;

public record WorkerAutomaActionCard(
        int cardNo,
        List<WorkerAutomaActionSymbol> checks,
        List<WorkerAutomaPolicyTag> policyTags,
        WorkerAutomaBonus bonus,
        WorkerAutomaSpecialAction specialAction,
        String rawTextRu,
        String imageRef,
        String transcriptionConfidence
) {
}
