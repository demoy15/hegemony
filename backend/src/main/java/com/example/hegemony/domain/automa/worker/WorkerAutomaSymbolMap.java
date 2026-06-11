package com.example.hegemony.domain.automa.worker;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class WorkerAutomaSymbolMap {
    private static final Map<String, WorkerAutomaActionSymbol> ACTION_SYMBOLS = Map.of(
            "PROPOSE_BILL", WorkerAutomaActionSymbol.PROPOSE_BILL,
            "ASSIGN_WORKERS", WorkerAutomaActionSymbol.ASSIGN_WORKERS,
            "BUY_GOODS_AND_SERVICES", WorkerAutomaActionSymbol.BUY_GOODS_AND_SERVICES,
            "PLACE_STRIKES", WorkerAutomaActionSymbol.PLACE_STRIKES
    );

    private static final Map<String, WorkerAutomaPolicyTag> POLICY_TAGS = Map.of(
            "POLICY_FISCAL", WorkerAutomaPolicyTag.POLICY_FISCAL,
            "POLICY_LABOR_MARKET", WorkerAutomaPolicyTag.POLICY_LABOR_MARKET,
            "POLICY_TAX", WorkerAutomaPolicyTag.POLICY_TAX,
            "POLICY_HEALTHCARE", WorkerAutomaPolicyTag.POLICY_HEALTHCARE,
            "POLICY_EDUCATION", WorkerAutomaPolicyTag.POLICY_EDUCATION,
            "POLICY_FOREIGN_TRADE", WorkerAutomaPolicyTag.POLICY_FOREIGN_TRADE,
            "POLICY_MIGRATION", WorkerAutomaPolicyTag.POLICY_MIGRATION
    );

    public Optional<WorkerAutomaActionSymbol> parseActionCheck(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ACTION_SYMBOLS.get(raw.trim().toUpperCase(Locale.ROOT)));
    }

    public Optional<WorkerAutomaPolicyTag> parsePolicyTag(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(POLICY_TAGS.get(raw.trim().toUpperCase(Locale.ROOT)));
    }
}
