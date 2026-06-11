package com.example.hegemony.bot.planning;

import com.example.hegemony.domain.automa.worker.WorkerAutomaActionCard;
import com.example.hegemony.domain.automa.worker.WorkerAutomaActionSymbol;
import com.example.hegemony.domain.automa.worker.WorkerAutomaCardRegistry;
import com.example.hegemony.domain.automa.worker.WorkerAutomaPolicyTag;
import com.example.hegemony.domain.command.AssignWorkersCommand;
import com.example.hegemony.domain.command.BuyGoodsAndServicesCommand;
import com.example.hegemony.domain.command.GameCommand;
import com.example.hegemony.domain.command.PlaceStrikesCommand;
import com.example.hegemony.domain.command.ProposeBillCommand;
import com.example.hegemony.domain.model.ActionType;
import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.OrderedCardDeckState;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.PlayerState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class WorkerAutomaCardPlanner implements BotCardPlanner {
    private static final String DECK_ID = "worker-simple-automa";

    private final WorkerAutomaCardRegistry cardRegistry;

    public WorkerAutomaCardPlanner(WorkerAutomaCardRegistry cardRegistry) {
        this.cardRegistry = cardRegistry;
    }

    @Override
    public boolean isReady(ClassType classType) {
        return classType == ClassType.WORKER && cardRegistry.isLoaded() && !cardRegistry.actionCards().isEmpty();
    }

    @Override
    public Optional<PlannedBotMove> plan(GameState state, ClassType classType, List<GameCommand> legalCommands) {
        if (classType != ClassType.WORKER || legalCommands == null || legalCommands.isEmpty() || !isReady(classType)) {
            return Optional.empty();
        }

        PlayerState actor = state.currentPlayer();
        if (actor == null || actor.getClassType() != ClassType.WORKER) {
            return Optional.empty();
        }

        WorkerAutomaActionCard currentCard = drawCard(state);
        List<Map<String, Object>> checksTrace = new ArrayList<>();
        GameCommand selectedFromCard = null;
        WorkerAutomaActionSymbol selectedSymbol = null;

        for (WorkerAutomaActionSymbol symbol : currentCard.checks()) {
            ActionType mapped = mapToActionType(symbol);
            List<GameCommand> candidates = legalCommands.stream()
                    .filter(command -> command.type() == mapped)
                    .toList();
            checksTrace.add(Map.of(
                    "symbol", symbol.name(),
                    "mappedActionType", mapped.name(),
                    "legalCandidates", candidates.size()
            ));
            if (selectedFromCard == null && !candidates.isEmpty()) {
                selectedFromCard = selectCommandForSymbol(symbol, candidates, currentCard.policyTags());
                selectedSymbol = symbol;
            }
        }

        boolean cardDrivenSelection = selectedFromCard != null;
        GameCommand selected = cardDrivenSelection
                ? selectedFromCard
                : legalCommands.stream()
                .max(Comparator.comparingInt(command -> fallbackScore(command.type())))
                .orElseGet(legalCommands::getFirst);

        ActionPlan plan = new ActionPlan(
                selected,
                cardDrivenSelection ? ActionPlanSupportStatus.SUPPORTED : ActionPlanSupportStatus.FALLBACK_HEURISTIC,
                cardDrivenSelection
                        ? "WORKER_SIMPLE_AUTOMA_CARD_ENGINE"
                        : "WORKER_SIMPLE_AUTOMA_FALLBACK: no card check produced a legal command."
        );
        boolean bonusApplied = canApplyBonus(currentCard.bonus(), selectedSymbol);
        plan.setOptionalCardReference("worker-" + currentCard.cardNo());
        if (bonusApplied) {
            plan.setOptionalModifier(currentCard.bonus().effectType());
        }

        Map<String, Object> trace = new HashMap<>();
        trace.put("automa", "WORKERS");
        trace.put("datasetStatus", cardRegistry.datasetStatus());
        trace.put("cardReference", "worker-" + currentCard.cardNo());
        trace.put("currentCardNo", currentCard.cardNo());
        trace.put("cardChecks", currentCard.checks().stream().map(Enum::name).toList());
        trace.put("policyTags", currentCard.policyTags().stream().map(Enum::name).toList());
        trace.put("checksTrace", checksTrace);
        trace.put("selectedCheckSymbol", selectedSymbol == null ? "NONE" : selectedSymbol.name());
        trace.put("selectedMoveId", selected.moveId());
        trace.put("selectedActionType", selected.type().name());
        trace.put("specialActionType", currentCard.specialAction() == null ? "NONE" : currentCard.specialAction().type());
        trace.put("bonus", currentCard.bonus() == null ? Map.of("trigger", "NONE", "applied", false) : Map.of(
                "trigger", currentCard.bonus().trigger(),
                "effectType", currentCard.bonus().effectType(),
                "effectParams", currentCard.bonus().effectParams(),
                "rawTextRu", currentCard.bonus().rawTextRu(),
                "applied", bonusApplied
        ));
        trace.put("specialAction", currentCard.specialAction() == null ? Map.of("type", "NONE") : Map.of(
                "type", currentCard.specialAction().type(),
                "params", currentCard.specialAction().params(),
                "conditions", currentCard.specialAction().conditions(),
                "rawTextRu", currentCard.specialAction().rawTextRu()
        ));
        trace.put("deck", Map.of(
                "deckId", state.getWorkerAutomaDeck().getDeckId(),
                "nextCardIndex", state.getWorkerAutomaDeck().getNextCardIndex(),
                "refreshCount", state.getWorkerAutomaDeck().getRefreshCount(),
                "visibleCardIds", state.getWorkerAutomaDeck().getVisibleCardIds()
        ));
        trace.put("instructionCardIds", cardRegistry.instructionCards().stream()
                .map(instruction -> instruction.id())
                .toList());
        trace.put("legalActionTypes", legalCommands.stream().map(command -> command.type().name()).toList());

        String rationale = cardDrivenSelection
                ? "Worker automa drew action card #" + currentCard.cardNo()
                + " and selected " + selected.type() + " via ordered simple-mode checks."
                : "Worker automa card #" + currentCard.cardNo()
                + " had no currently legal checked action; explicit legal fallback selected " + selected.type() + ".";

        return Optional.of(new PlannedBotMove(
                plan,
                "worker-automa-card-engine",
                rationale,
                !cardDrivenSelection,
                legalCommands.size(),
                trace
        ));
    }

    private WorkerAutomaActionCard drawCard(GameState state) {
        List<String> cardIds = cardRegistry.actionCards().stream()
                .map(card -> "worker-" + card.cardNo())
                .toList();
        OrderedCardDeckState deck = state.getWorkerAutomaDeck();
        if (needsRefresh(deck, cardIds)) {
            refreshDeck(deck, cardIds, state.getCurrentRound(), "INITIALIZE_OR_EXHAUSTED");
        }

        String drawnCardId = deck.getOrderedCardIds().get(deck.getNextCardIndex());
        deck.setNextCardIndex(deck.getNextCardIndex() + 1);
        deck.setVisibleCardIds(List.of(drawnCardId));
        int cardNo = parseCardNo(drawnCardId);
        return cardRegistry.findActionCard(cardNo).orElseGet(() -> cardRegistry.actionCards().getFirst());
    }

    private boolean needsRefresh(OrderedCardDeckState deck, List<String> cardIds) {
        if (!DECK_ID.equals(deck.getDeckId())) {
            return true;
        }
        if (deck.getOrderedCardIds().isEmpty() || deck.getNextCardIndex() >= deck.getOrderedCardIds().size()) {
            return true;
        }
        return !new HashSet<>(deck.getOrderedCardIds()).equals(new HashSet<>(cardIds));
    }

    private void refreshDeck(OrderedCardDeckState deck, List<String> cardIds, int round, String reason) {
        List<String> shuffled = new ArrayList<>(cardIds);
        Collections.shuffle(shuffled);
        deck.setDeckId(DECK_ID);
        deck.setOrderedCardIds(shuffled);
        deck.setVisibleCardIds(List.of());
        deck.setVisibleWindowSize(1);
        deck.setNextCardIndex(0);
        deck.setRefreshCount(deck.getRefreshCount() + 1);
        deck.setLastRefreshedRound(round);
        deck.setLastRefreshReason(reason);
    }

    private int parseCardNo(String cardId) {
        if (cardId == null || !cardId.startsWith("worker-")) {
            return -1;
        }
        try {
            return Integer.parseInt(cardId.substring("worker-".length()));
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private ActionType mapToActionType(WorkerAutomaActionSymbol symbol) {
        return switch (symbol) {
            case PROPOSE_BILL -> ActionType.PROPOSE_BILL;
            case ASSIGN_WORKERS -> ActionType.ASSIGN_WORKERS;
            case BUY_GOODS_AND_SERVICES -> ActionType.BUY_GOODS_AND_SERVICES;
            case PLACE_STRIKES -> ActionType.PLACE_STRIKES;
        };
    }

    private GameCommand selectCommandForSymbol(
            WorkerAutomaActionSymbol symbol,
            List<GameCommand> candidates,
            List<WorkerAutomaPolicyTag> policyTags
    ) {
        return switch (symbol) {
            case PROPOSE_BILL -> selectProposeBill(candidates, policyTags);
            case ASSIGN_WORKERS -> candidates.stream()
                    .filter(command -> command instanceof AssignWorkersCommand)
                    .map(command -> (AssignWorkersCommand) command)
                    .max(Comparator.comparingInt(command -> command.assignments().size()))
                    .map(command -> (GameCommand) command)
                    .orElse(candidates.getFirst());
            case BUY_GOODS_AND_SERVICES -> candidates.stream()
                    .filter(command -> command instanceof BuyGoodsAndServicesCommand)
                    .map(command -> (BuyGoodsAndServicesCommand) command)
                    .max(Comparator.comparingInt(this::buyGoodsScore))
                    .map(command -> (GameCommand) command)
                    .orElse(candidates.getFirst());
            case PLACE_STRIKES -> candidates.stream()
                    .filter(command -> command instanceof PlaceStrikesCommand)
                    .map(command -> (PlaceStrikesCommand) command)
                    .max(Comparator.comparingInt(command -> command.enterpriseIds() == null ? 0 : command.enterpriseIds().size()))
                    .map(command -> (GameCommand) command)
                    .orElse(candidates.getFirst());
        };
    }

    private GameCommand selectProposeBill(List<GameCommand> candidates, List<WorkerAutomaPolicyTag> policyTags) {
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
        return candidates.getFirst();
    }

    private int buyGoodsScore(BuyGoodsAndServicesCommand command) {
        int resourceScore = switch (command.resourceType() == null ? "" : command.resourceType().toUpperCase()) {
            case "HEALTHCARE" -> 400;
            case "EDUCATION" -> 300;
            case "LUXURY" -> 200;
            default -> 50;
        };
        int quantity = command.purchases().stream().mapToInt(item -> Math.max(0, item.quantity())).sum();
        return resourceScore + quantity;
    }

    private PolicyId mapPolicyTagToPolicyId(WorkerAutomaPolicyTag policyTag) {
        if (policyTag == null) {
            return null;
        }
        return switch (policyTag) {
            case POLICY_FISCAL -> PolicyId.POLICY_1_FISCAL;
            case POLICY_LABOR_MARKET -> PolicyId.POLICY_2_LABOR_MARKET;
            case POLICY_TAX -> PolicyId.POLICY_3_TAXATION;
            case POLICY_HEALTHCARE -> PolicyId.POLICY_4_HEALTHCARE_AND_BENEFITS;
            case POLICY_EDUCATION -> PolicyId.POLICY_5_EDUCATION;
            case POLICY_FOREIGN_TRADE -> PolicyId.POLICY_6_FOREIGN_TRADE;
            case POLICY_MIGRATION -> PolicyId.POLICY_7_IMMIGRATION;
        };
    }

    private boolean canApplyBonus(com.example.hegemony.domain.automa.worker.WorkerAutomaBonus bonus, WorkerAutomaActionSymbol selectedSymbol) {
        if (bonus == null || selectedSymbol == null || bonus.trigger() == null) {
            return false;
        }
        return switch (bonus.trigger()) {
            case "assign_workers_this_turn" -> selectedSymbol == WorkerAutomaActionSymbol.ASSIGN_WORKERS;
            case "buy_goods_and_services_this_turn" -> selectedSymbol == WorkerAutomaActionSymbol.BUY_GOODS_AND_SERVICES;
            case "propose_bill_this_turn" -> selectedSymbol == WorkerAutomaActionSymbol.PROPOSE_BILL;
            default -> false;
        };
    }

    private int fallbackScore(ActionType actionType) {
        return switch (actionType) {
            case ASSIGN_WORKERS -> 260;
            case BUY_GOODS_AND_SERVICES -> 250;
            case PROPOSE_BILL -> 240;
            case PLACE_STRIKES -> 230;
            case PLACE_DEMONSTRATION -> 220;
            case ADD_VOTING_CUBES,
                    CALL_EXTRAORDINARY_VOTE -> 200;
            case ADVANCE_GAME_FLOW -> 180;
            case ADVANCE_TO_VOTING,
                    DECLARE_VOTE_STANCE,
                    DRAW_VOTING_CUBES,
                    COMMIT_VOTE_INFLUENCE -> 150;
            case ADVANCE_TO_PRODUCTION,
                    RESOLVE_PRODUCTION_PHASE,
                    ADVANCE_TO_SCORING,
                    RESOLVE_SCORING_PHASE,
                    ADVANCE_TO_NEXT_ROUND,
                    RESOLVE_PREPARATION_PHASE,
                    REFRESH_BUSINESS_DEALS,
                    ADVANCE_ROUND -> 100;
            default -> 10;
        };
    }
}
