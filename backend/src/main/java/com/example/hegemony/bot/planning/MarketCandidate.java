package com.example.hegemony.bot.planning;

import com.example.hegemony.domain.model.ClassType;

public record MarketCandidate(
        String enterpriseId,
        ClassType ownerClass,
        int freeSlots,
        int wageLevel,
        String decisionSource
) {
}
