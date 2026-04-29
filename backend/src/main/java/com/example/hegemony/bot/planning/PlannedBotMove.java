package com.example.hegemony.bot.planning;

import java.util.Map;

public record PlannedBotMove(
        ActionPlan actionPlan,
        String plannerId,
        String rationale,
        boolean fallbackHeuristicMode,
        int legalOptionsConsidered,
        Map<String, Object> debugTrace
) {
}
