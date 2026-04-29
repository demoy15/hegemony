package com.example.hegemony.domain.automa.capitalist;

import com.example.hegemony.domain.command.GameCommand;
import com.example.hegemony.domain.command.ProposeBillCommand;
import com.example.hegemony.domain.command.AssignWorkersCommand;
import com.example.hegemony.domain.model.ActionType;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.PlayerState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CapitalistAutomaInterpreter {
    public CapitalistAutomaInterpretationResult interpret(
            GameState state,
            CapitalistAutomaExecutionMode mode,
            CapitalistAutomaActionCard actionCard,
            List<CapitalistAutomaInstructionCard> instructionCards,
            List<GameCommand> legalCommands
    ) {
        List<Map<String, Object>> checksTrace = new ArrayList<>();
        List<Map<String, Object>> priorityShiftUnsupported = new ArrayList<>();
        GameCommand selectedFromChecks = null;
        CapitalistAutomaActionSymbol selectedSymbol = null;
        String selectedByInstruction = null;

        for (CapitalistAutomaActionSymbol symbol : actionCard.checks()) {
            Optional<ActionType> mapped = mapToActionType(symbol);
            List<CapitalistAutomaInstructionCard> matchingInstructions = instructionCards.stream()
                    .filter(card -> card.appliesTo(symbol, mode))
                    .toList();

            List<GameCommand> candidates = mapped
                    .map(actionType -> legalCommands.stream().filter(command -> command.type() == actionType).toList())
                    .orElse(List.of());

            boolean supported = mapped.isPresent();
            boolean legalAvailable = !candidates.isEmpty();
            boolean instructionGatePassed = evaluateInstructionGate(matchingInstructions, state, mode);

            checksTrace.add(Map.of(
                    "symbol", symbol.name(),
                    "mappedActionType", mapped.map(ActionType::name).orElse("UNSUPPORTED"),
                    "supported", supported,
                    "legalCandidates", candidates.size(),
                    "instructionGatePassed", instructionGatePassed,
                    "instructionIds", matchingInstructions.stream().map(CapitalistAutomaInstructionCard::id).toList(),
                    "mode", mode.name()
            ));

            if (mode == CapitalistAutomaExecutionMode.COMPLEX) {
                for (CapitalistAutomaInstructionCard instruction : matchingInstructions) {
                    for (Map<String, Object> rule : instruction.structuredRules()) {
                        String kind = String.valueOf(rule.getOrDefault("kind", ""));
                        if (kind.contains("priority_shift")) {
                            priorityShiftUnsupported.add(Map.of(
                                    "instructionId", instruction.id(),
                                    "kind", kind,
                                    "status", "UNSUPPORTED_PRIORITY_ROW_MODEL"
                            ));
                        }
                    }
                }
            }

            if (selectedFromChecks == null && supported && legalAvailable && instructionGatePassed) {
                selectedFromChecks = selectCommandForSymbol(symbol, candidates, actionCard.policyTags());
                selectedSymbol = symbol;
                if (!matchingInstructions.isEmpty()) {
                    selectedByInstruction = matchingInstructions.getFirst().id();
                }
            }
        }

        boolean usedSpecialFallback = false;
        GameCommand selected = selectedFromChecks;
        if (selected == null) {
            selected = chooseBySpecialActionFallback(actionCard.specialAction(), legalCommands);
            usedSpecialFallback = selected != null;
        }

        boolean cardDrivenSelection = true;
        if (selected == null) {
            selected = legalCommands.stream()
                    .max(Comparator.comparingInt(command -> fallbackScore(command.type())))
                    .orElseGet(legalCommands::getFirst);
            cardDrivenSelection = false;
        }

        boolean bonusApplied = canApplyBonus(actionCard.bonus(), selectedSymbol);
        Map<String, Object> bonusTrace = new HashMap<>();
        if (actionCard.bonus() != null) {
            bonusTrace.put("trigger", actionCard.bonus().trigger());
            bonusTrace.put("effectType", actionCard.bonus().effectType());
            bonusTrace.put("effectParams", actionCard.bonus().effectParams());
            bonusTrace.put("rawTextRu", actionCard.bonus().rawTextRu());
        } else {
            bonusTrace.put("trigger", "NONE");
        }
        bonusTrace.put("applied", bonusApplied);

        Map<String, Object> trace = new HashMap<>();
        trace.put("currentCardNo", actionCard.cardNo());
        trace.put("mode", mode.name());
        trace.put("cardChecks", actionCard.checks().stream().map(Enum::name).toList());
        trace.put("policyTags", actionCard.policyTags().stream().map(Enum::name).toList());
        trace.put("checksTrace", checksTrace);
        trace.put("priorityChangesApplied", List.of());
        trace.put("priorityChangesUnsupported", priorityShiftUnsupported);
        trace.put("selectedCheckSymbol", selectedSymbol == null ? "NONE" : selectedSymbol.name());
        trace.put("selectedInstructionCardId", selectedByInstruction == null ? "NONE" : selectedByInstruction);
        trace.put("specialActionType", actionCard.specialAction() == null ? "NONE" : actionCard.specialAction().type());
        trace.put("usedSpecialActionFallback", usedSpecialFallback);
        trace.put("bonus", bonusTrace);
        trace.put("selectedAction", selected.type().name());
        trace.put("legalOptionsConsidered", legalCommands.size());
        trace.put("unsupportedMarkers", checksTrace.stream().filter(entry -> "UNSUPPORTED".equals(entry.get("mappedActionType"))).toList());

        String rationale;
        if (cardDrivenSelection) {
            rationale = "Capitalist automa interpreted action card #" + actionCard.cardNo()
                    + " in " + mode.name() + " mode and selected " + selected.type()
                    + (usedSpecialFallback ? " via special-action fallback." : " via ordered checks.");
        } else {
            rationale = "Capitalist automa card #" + actionCard.cardNo()
                    + " had no currently legal interpreted action; explicit legal fallback selected " + selected.type() + ".";
        }

        return new CapitalistAutomaInterpretationResult(
                selected,
                cardDrivenSelection,
                usedSpecialFallback,
                bonusApplied,
                trace,
                rationale
        );
    }

    private Optional<ActionType> mapToActionType(CapitalistAutomaActionSymbol symbol) {
        return switch (symbol) {
            case PROPOSE_BILL -> Optional.of(ActionType.PROPOSE_BILL);
            case ASSIGN_WORKERS -> Optional.of(ActionType.ASSIGN_WORKERS);
            case BUILD_ENTERPRISE,
                    SELL_ON_EXTERNAL_MARKET,
                    REACT_TO_STRIKE,
                    RECONFIGURE_EQUIPMENT,
                    SELL_ENTERPRISE,
                    LOBBY_INTERESTS -> Optional.empty();
        };
    }

    private GameCommand selectCommandForSymbol(
            CapitalistAutomaActionSymbol symbol,
            List<GameCommand> candidates,
            List<CapitalistAutomaPolicyTag> policyTags
    ) {
        if (symbol == CapitalistAutomaActionSymbol.PROPOSE_BILL) {
            List<PolicyId> preferredPolicies = policyTags.stream()
                    .map(this::mapPolicyTagToPolicyId)
                    .filter(policyId -> policyId != null)
                    .toList();
            for (PolicyId preferredPolicy : preferredPolicies) {
                Optional<GameCommand> matched = candidates.stream()
                        .filter(command -> command instanceof ProposeBillCommand)
                        .map(command -> (ProposeBillCommand) command)
                        .filter(command -> command.policyId() == preferredPolicy)
                        .map(command -> (GameCommand) command)
                        .findFirst();
                if (matched.isPresent()) {
                    return matched.get();
                }
            }
        }

        if (symbol == CapitalistAutomaActionSymbol.ASSIGN_WORKERS) {
            return candidates.stream()
                    .filter(command -> command instanceof AssignWorkersCommand)
                    .map(command -> (AssignWorkersCommand) command)
                    .max(Comparator.comparingInt(command -> command.assignments().size()))
                    .map(command -> (GameCommand) command)
                    .orElse(candidates.getFirst());
        }
        return candidates.getFirst();
    }

    private boolean evaluateInstructionGate(
            List<CapitalistAutomaInstructionCard> instructions,
            GameState state,
            CapitalistAutomaExecutionMode mode
    ) {
        if (instructions == null || instructions.isEmpty()) {
            return true;
        }
        PlayerState actor = state.currentPlayer();
        if (actor == null) {
            return true;
        }

        for (CapitalistAutomaInstructionCard instruction : instructions) {
            for (Map<String, Object> rule : instruction.structuredRules()) {
                Object kind = rule.get("kind");
                if (kind == null) {
                    continue;
                }
                String kindText = String.valueOf(kind);
                if (!kindText.contains("precondition")) {
                    continue;
                }

                Object requireRaw = rule.get("require");
                if (!(requireRaw instanceof Map<?, ?> requireMap)) {
                    continue;
                }

                if (Boolean.TRUE.equals(requireMap.get("available_bill_token")) && actor.availableProposalTokens() <= 0) {
                    return false;
                }

                if (mode == CapitalistAutomaExecutionMode.SIMPLE
                        && Boolean.TRUE.equals(requireMap.get("card_targets_non_preferred_policy_course"))
                        && state.getPolicies().stream().noneMatch(policy -> policy.getOccupyingProposalToken() == null)) {
                    return false;
                }
            }
        }
        return true;
    }

    private PolicyId mapPolicyTagToPolicyId(CapitalistAutomaPolicyTag policyTag) {
        if (policyTag == null) {
            return null;
        }
        return switch (policyTag) {
            case POLICY_LABOR_MARKET -> PolicyId.POLICY_2_LABOR_MARKET;
            case POLICY_TAX -> PolicyId.POLICY_3_TAXATION;
            case POLICY_HEALTHCARE -> PolicyId.POLICY_4_HEALTHCARE_AND_BENEFITS;
            case POLICY_EDUCATION -> PolicyId.POLICY_5_EDUCATION;
            case POLICY_FOREIGN_TRADE -> PolicyId.POLICY_6_FOREIGN_TRADE;
            case POLICY_MIGRATION -> PolicyId.POLICY_7_IMMIGRATION;
        };
    }

    private GameCommand chooseBySpecialActionFallback(CapitalistAutomaSpecialAction specialAction, List<GameCommand> legalCommands) {
        if (specialAction == null || specialAction.type() == null || specialAction.type().isBlank()) {
            return null;
        }
        Optional<GameCommand> advanceFlow = legalCommands.stream()
                .filter(command -> command.type() == ActionType.ADVANCE_GAME_FLOW)
                .findFirst();
        if (advanceFlow.isPresent()) {
            return advanceFlow.get();
        }
        return legalCommands.stream()
                .filter(command -> command.type().name().startsWith("ADVANCE_"))
                .findFirst()
                .orElse(null);
    }

    private boolean canApplyBonus(CapitalistAutomaBonus bonus, CapitalistAutomaActionSymbol selectedSymbol) {
        if (bonus == null || selectedSymbol == null || bonus.trigger() == null) {
            return false;
        }
        return switch (bonus.trigger()) {
            case "built_enterprise_this_turn" -> selectedSymbol == CapitalistAutomaActionSymbol.BUILD_ENTERPRISE;
            case "sold_on_external_market_this_turn" -> selectedSymbol == CapitalistAutomaActionSymbol.SELL_ON_EXTERNAL_MARKET;
            default -> false;
        };
    }

    private int fallbackScore(ActionType actionType) {
        return switch (actionType) {
            case PROPOSE_BILL -> 240;
            case ASSIGN_WORKERS -> 220;
            case ADVANCE_GAME_FLOW -> 180;
            case ADVANCE_TO_VOTING -> 160;
            case DECLARE_VOTE_STANCE -> 140;
            case COMMIT_VOTE_INFLUENCE -> 120;
            case ADVANCE_TO_PRODUCTION,
                    RESOLVE_PRODUCTION_PHASE,
                    ADVANCE_TO_SCORING,
                    RESOLVE_SCORING_PHASE,
                    ADVANCE_TO_NEXT_ROUND,
                    RESOLVE_PREPARATION_PHASE,
                    REFRESH_BUSINESS_DEALS,
                    ADVANCE_ROUND -> 110;
            default -> 10;
        };
    }
}
