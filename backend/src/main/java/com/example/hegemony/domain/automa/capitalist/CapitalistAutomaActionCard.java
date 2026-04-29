package com.example.hegemony.domain.automa.capitalist;

import java.util.List;

public record CapitalistAutomaActionCard(
        int cardNo,
        List<CapitalistAutomaActionSymbol> checks,
        List<CapitalistAutomaPolicyTag> policyTags,
        CapitalistAutomaBonus bonus,
        CapitalistAutomaSpecialAction specialAction,
        String rawTextRu,
        String imageRef,
        String transcriptionConfidence
) {
}
