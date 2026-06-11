package com.example.hegemony.domain.automa.capitalist;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class CapitalistAutomaSymbolMap {
    private static final Map<String, CapitalistAutomaActionSymbol> ACTION_CHECKS = Map.of(
            "blue_building", CapitalistAutomaActionSymbol.BUILD_ENTERPRISE,
            "ballot_hand", CapitalistAutomaActionSymbol.PROPOSE_BILL,
            "pink_hand", CapitalistAutomaActionSymbol.REACT_TO_STRIKE,
            "gray_gear", CapitalistAutomaActionSymbol.RECONFIGURE_EQUIPMENT,
            "external_market_ops", CapitalistAutomaActionSymbol.SELL_ON_EXTERNAL_MARKET,
            "worker_assignment", CapitalistAutomaActionSymbol.ASSIGN_WORKERS,
            "sell_building", CapitalistAutomaActionSymbol.SELL_ENTERPRISE,
            "lobby_interests", CapitalistAutomaActionSymbol.LOBBY_INTERESTS
    );

    private static final Map<String, CapitalistAutomaPolicyTag> POLICY_TAGS = Map.of(
            "fiscal", CapitalistAutomaPolicyTag.POLICY_FISCAL,
            "labor_market", CapitalistAutomaPolicyTag.POLICY_LABOR_MARKET,
            "tax", CapitalistAutomaPolicyTag.POLICY_TAX,
            "healthcare", CapitalistAutomaPolicyTag.POLICY_HEALTHCARE,
            "education", CapitalistAutomaPolicyTag.POLICY_EDUCATION,
            "foreign_trade", CapitalistAutomaPolicyTag.POLICY_FOREIGN_TRADE,
            "migration", CapitalistAutomaPolicyTag.POLICY_MIGRATION
    );

    private static final Map<String, CapitalistAutomaPriorityCode> PRIORITY_CODES = Map.of(
            "PP", CapitalistAutomaPriorityCode.BUILD_ENTERPRISE_PRIORITY,
            "PVR", CapitalistAutomaPriorityCode.SELL_ON_EXTERNAL_MARKET_PRIORITY,
            "OD", CapitalistAutomaPriorityCode.SPECIAL_ACTION_PRIORITY
    );

    private static final Map<String, String> RESOURCE_TAGS = Map.of(
            "food", "FOOD",
            "luxury", "LUXURY",
            "healthcare", "HEALTHCARE",
            "education", "EDUCATION",
            "influence", "INFLUENCE"
    );

    private static final Map<String, String> MISC_TAGS = Map.of(
            "unemployed", "UNEMPLOYED",
            "functioning_enterprise", "FUNCTIONING_ENTERPRISE",
            "non_functioning_enterprise", "NON_FUNCTIONING_ENTERPRISE",
            "automated_enterprise", "AUTOMATED_ENTERPRISE",
            "demonstration", "DEMONSTRATION",
            "proposal_token", "BILL_TOKEN",
            "voting_cubes", "VOTING_CUBES"
    );

    public Optional<CapitalistAutomaActionSymbol> parseActionCheck(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            return Optional.empty();
        }
        String normalized = rawCode.trim().toLowerCase(Locale.ROOT);
        if (ACTION_CHECKS.containsKey(normalized)) {
            return Optional.of(ACTION_CHECKS.get(normalized));
        }
        try {
            return Optional.of(CapitalistAutomaActionSymbol.valueOf(rawCode.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public Optional<CapitalistAutomaPolicyTag> parsePolicyTag(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            return Optional.empty();
        }
        String normalized = rawCode.trim().toLowerCase(Locale.ROOT);
        if (POLICY_TAGS.containsKey(normalized)) {
            return Optional.of(POLICY_TAGS.get(normalized));
        }
        try {
            return Optional.of(CapitalistAutomaPolicyTag.valueOf(rawCode.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public Optional<CapitalistAutomaPriorityCode> parsePriorityCode(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            return Optional.empty();
        }
        String normalized = rawCode.trim().toUpperCase(Locale.ROOT);
        if (PRIORITY_CODES.containsKey(normalized)) {
            return Optional.of(PRIORITY_CODES.get(normalized));
        }
        try {
            return Optional.of(CapitalistAutomaPriorityCode.valueOf(normalized));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public Optional<String> parseResourceTag(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            return Optional.empty();
        }
        String normalized = rawCode.trim().toLowerCase(Locale.ROOT);
        return Optional.ofNullable(RESOURCE_TAGS.get(normalized));
    }

    public Optional<String> parseMiscTag(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            return Optional.empty();
        }
        String normalized = rawCode.trim().toLowerCase(Locale.ROOT);
        return Optional.ofNullable(MISC_TAGS.get(normalized));
    }
}
