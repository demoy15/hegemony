package com.example.hegemony.application;

import com.example.hegemony.application.setup.SetupSpecLoader;
import com.example.hegemony.application.setup.SetupSpecModel;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaActionCard;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaActionSymbol;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaBonus;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaCardRegistry;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaExecutionMode;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaInstructionCard;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaPolicyTag;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaSpecialAction;
import com.example.hegemony.domain.command.AssignWorkersCommand;
import com.example.hegemony.domain.command.EndTurnCommand;
import com.example.hegemony.domain.command.GameCommand;
import com.example.hegemony.domain.command.ProposeBillCommand;
import com.example.hegemony.domain.engine.ApplyCommandResult;
import com.example.hegemony.domain.engine.GameRulesEngine;
import com.example.hegemony.domain.event.DomainEvent;
import com.example.hegemony.domain.model.ActionType;
import com.example.hegemony.domain.model.BotStrategyMode;
import com.example.hegemony.domain.model.BotTurnSummary;
import com.example.hegemony.domain.model.BusinessDealCard;
import com.example.hegemony.domain.model.BusinessDealRequirement;
import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.CurrentVoteState;
import com.example.hegemony.domain.model.Enterprise;
import com.example.hegemony.domain.model.EnterpriseSlot;
import com.example.hegemony.domain.model.ExportCardOffer;
import com.example.hegemony.domain.model.ExportCardState;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.OrderedCardDeckState;
import com.example.hegemony.domain.model.PlayerState;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.PolicyState;
import com.example.hegemony.domain.model.ProposalToken;
import com.example.hegemony.domain.model.ResourceType;
import com.example.hegemony.domain.model.RoundPhase;
import com.example.hegemony.domain.model.VoteResolutionResult;
import com.example.hegemony.domain.model.VoteStance;
import com.example.hegemony.domain.model.VotingBagState;
import com.example.hegemony.domain.model.VotingCubeOwnerClass;
import com.example.hegemony.domain.model.VotingStage;
import com.example.hegemony.domain.model.Worker;
import com.example.hegemony.domain.model.WorkerLocation;
import com.example.hegemony.domain.model.WorkerQualification;
import com.example.hegemony.domain.model.WorkerSector;
import com.example.hegemony.domain.model.WorkerSlotColor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AutomaSimpleModeTurnService {
    private static final int POLITICAL_PRESSURE_CUBES = 3;
    private static final int LOAN_REPAYMENT_COST = 55;
    private static final int SELL_ENTERPRISE_FALLBACK_VALUE = 20;
    private static final int REGULAR_STORAGE_DEFAULT_CAPACITY_PER_RESOURCE = 20;
    private static final int FREE_TRADE_ZONE_CAPACITY_PER_DEAL = 2;
    private static final int STORAGE_DUTY_PER_GOOD = 1;

    private final GameRulesEngine engine;
    private final CapitalistAutomaCardRegistry cardRegistry;
    private final Map<String, SetupSpecModel.EnterpriseDefinition> capitalistEnterpriseDefinitions;

    public AutomaSimpleModeTurnService(
            GameRulesEngine engine,
            CapitalistAutomaCardRegistry cardRegistry,
            SetupSpecLoader setupSpecLoader
    ) {
        this.engine = engine;
        this.cardRegistry = cardRegistry;
        this.capitalistEnterpriseDefinitions = loadEnterpriseDefinitions(setupSpecLoader);
    }

    public Optional<ResolvedTurn> resolveAndApply(GameState original, PlayerState actor) {
        if (original == null || actor == null) {
            return Optional.empty();
        }
        if (actor.getClassType() != ClassType.CAPITALIST || actor.getBotStrategyMode() != BotStrategyMode.CARD_DRIVEN_SIMPLE_AUTOMA) {
            return Optional.empty();
        }
        if (original.getCurrentPhase() != RoundPhase.ACTIONS || !cardRegistry.isLoaded() || cardRegistry.actionCards().isEmpty()) {
            return Optional.empty();
        }
        if (original.currentPlayer() == null || !Objects.equals(original.currentPlayer().getPlayerId(), actor.getPlayerId())) {
            return Optional.empty();
        }

        GameState beforeTurn = original.copy();
        GameState working = original.copy();
        PlayerState workingActor = working.findPlayerById(actor.getPlayerId()).orElse(null);
        if (workingActor == null) {
            return Optional.empty();
        }

        List<Map<String, Object>> freeActions = runStartOfTurnAutoFreeActions(working, workingActor);
        CapitalistAutomaActionCard card = drawAutomaCard(working);
        AutomaCard mappedCard = toAutomaCard(card);
        List<GameCommand> legalCommands = engine.generateLegalCommands(working);

        List<AutomaActionEvaluation> checkedSlots = new ArrayList<>();
        AutomaActionEvaluation chosenBase = null;
        for (int slot = 1; slot <= 4; slot++) {
            boolean withBonus = slot == 1;
            AutomaActionEvaluation evaluation = tryResolveActionSlot(working, workingActor, mappedCard, legalCommands, slot, withBonus);
            checkedSlots.add(evaluation);
            if (chosenBase == null && evaluation.canExecute()) {
                chosenBase = evaluation;
            }
        }

        AutomaActionEvaluation specialEvaluation = chosenBase == null
                ? tryResolveSpecialAction(working, workingActor, mappedCard)
                : null;

        boolean usedSpecialAction = false;
        boolean usedPoliticalPressureFallback = false;
        String fallbackReason = "";
        List<String> syntheticEventSummaries = new ArrayList<>();
        List<DomainEvent> producedEvents = new ArrayList<>();
        GameCommand selectedCommand = null;
        String selectedAutomaAction = "UNRESOLVED";
        boolean firstActionBonusApplied = chosenBase != null && chosenBase.candidate().slot() == 1 && chosenBase.bonusApplied();
        Map<String, Object> resolvedTarget = new LinkedHashMap<>();
        List<String> resolvedReasonPath = new ArrayList<>();
        SnapVoteDecision snapVoteDecision = SnapVoteDecision.notApplicable();
        PolicyId proposedPolicyForSnapVote = null;

        if (chosenBase != null) {
            selectedAutomaAction = chosenBase.candidate().actionType().name();
            selectedCommand = chosenBase.primaryCommand();
            resolvedTarget.putAll(chosenBase.resolvedTarget());
            resolvedReasonPath.addAll(chosenBase.reasonPath());
        } else if (specialEvaluation.canExecute()) {
            usedSpecialAction = true;
            selectedAutomaAction = "SPECIAL_ACTION";
            resolvedTarget.putAll(specialEvaluation.resolvedTarget());
            resolvedReasonPath.addAll(specialEvaluation.reasonPath());
            syntheticEventSummaries.add(applySpecialAction(working, workingActor, mappedCard.specialAction()));
        } else {
            usedPoliticalPressureFallback = true;
            selectedAutomaAction = "APPLY_POLITICAL_PRESSURE";
            fallbackReason = resolveFallbackReason(checkedSlots, specialEvaluation);
            applyPoliticalPressure(working, workingActor);
            syntheticEventSummaries.add("Automa applied political pressure and added 3 capitalist cubes to the voting bag.");
        }

        if (selectedCommand != null) {
            ApplyCommandResult commandResult = engine.apply(working, selectedCommand);
            if (!commandResult.validation().isValid()) {
                throw new IllegalStateException("Simple automa selected illegal move: " + commandResult.validation().getErrors());
            }
            working = commandResult.resultingState();
            producedEvents.addAll(commandResult.producedEvents());

            if (selectedCommand instanceof ProposeBillCommand propose) {
                snapVoteDecision = evaluateSnapVoteDecision(working, actor.getPlayerId(), propose.policyId());
                proposedPolicyForSnapVote = propose.policyId();
            }
        } else if (chosenBase != null && chosenBase.canExecute()) {
            ActionMutationResult mutationResult = executeResolvedAutomaAction(working, actor.getPlayerId(), mappedCard, chosenBase);
            if (!mutationResult.executed()) {
                throw new IllegalStateException("Simple automa failed to execute resolved action: " + chosenBase.candidate().actionType());
            }
            if (!mutationResult.syntheticSummaries().isEmpty()) {
                syntheticEventSummaries.addAll(mutationResult.syntheticSummaries());
            }
            if (!mutationResult.resolvedTarget().isEmpty()) {
                resolvedTarget = new LinkedHashMap<>(mutationResult.resolvedTarget());
            }
            if (!mutationResult.reasonPath().isEmpty()) {
                resolvedReasonPath = new ArrayList<>(mutationResult.reasonPath());
            }
        }

        boolean snapVoteActive = working.getCurrentVoteState() != null && working.getCurrentVoteState().isExtraordinary();
        if (!snapVoteActive) {
            ApplyCommandResult endTurnResult = engine.apply(working, new EndTurnCommand());
            if (!endTurnResult.validation().isValid()) {
                throw new IllegalStateException("Simple automa failed to end turn: " + endTurnResult.validation().getErrors());
            }
            working = endTurnResult.resultingState();
            producedEvents.addAll(endTurnResult.producedEvents());
        }

        if (!snapVoteActive && proposedPolicyForSnapVote != null && snapVoteDecision.triggered()) {
            String snapVoteSummary = tryStartSnapVote(working, actor.getPlayerId(), proposedPolicyForSnapVote, snapVoteDecision);
            if (!snapVoteSummary.isBlank()) {
                syntheticEventSummaries.add(snapVoteSummary);
            }
        } else if (snapVoteActive) {
            syntheticEventSummaries.add("Automa initiated snap vote and paused its action turn until the vote resolves.");
        }

        Map<String, Object> automaTrace = new LinkedHashMap<>();
        automaTrace.put("automa", "CAPITALISTS");
        automaTrace.put("mode", "SIMPLE");
        automaTrace.put("cardId", mappedCard.id());
        automaTrace.put("currentCardNo", mappedCard.cardNo());
        automaTrace.put("checkedSlots", checkedSlots.stream().map(this::evaluationToTrace).toList());
        automaTrace.put("checksTrace", checkedSlots.stream().map(this::evaluationToTrace).toList());
        automaTrace.put("specialActionEvaluation", specialEvaluation == null ? Map.of() : evaluationToTrace(specialEvaluation));
        automaTrace.put("selectedAutomaAction", selectedAutomaAction);
        automaTrace.put("usedSpecialAction", usedSpecialAction);
        automaTrace.put("usedPoliticalPressureFallback", usedPoliticalPressureFallback);
        automaTrace.put("firstActionBonusApplied", firstActionBonusApplied);
        automaTrace.put("freeActions", freeActions);
        automaTrace.put("fallbackReason", fallbackReason);
        automaTrace.put("resolvedTarget", resolvedTarget);
        automaTrace.put("resolvedReasonPath", resolvedReasonPath);
        automaTrace.put("snapVote", snapVoteDecision.toTraceMap());

        Map<String, Object> stateChanges = buildStateChanges(beforeTurn, working, actor.getPlayerId());
        automaTrace.put("stateChanges", stateChanges);

        String decisionText = buildDecisionText(checkedSlots, specialEvaluation, selectedAutomaAction, resolvedTarget, snapVoteDecision);
        automaTrace.put("decisionText", decisionText);

        BotTurnSummary summary = new BotTurnSummary();
        summary.setActingClass(ClassType.CAPITALIST);
        summary.setActingPlayerId(actor.getPlayerId());
        summary.setSelectedMoveId(selectedCommand == null
                ? "automa:" + selectedAutomaAction.toLowerCase(Locale.ROOT)
                : selectedCommand.moveId());
        summary.setSelectedAction(selectedCommand == null ? ActionType.END_TURN : selectedCommand.type());

        Map<String, Object> chosenTargets = new LinkedHashMap<>();
        chosenTargets.put("automaAction", selectedAutomaAction);
        chosenTargets.put("cardId", mappedCard.id());
        if (!resolvedTarget.isEmpty()) {
            chosenTargets.put("resolvedTarget", resolvedTarget);
        }
        if (!resolvedReasonPath.isEmpty()) {
            chosenTargets.put("reasonPath", resolvedReasonPath);
        }
        summary.setChosenTargets(chosenTargets);

        summary.setCardModifierPathUsed(firstActionBonusApplied);
        summary.setPlannerId("capitalist-simple-automa-card-engine");
        summary.setRationale("Capitalist simple automa resolved card " + mappedCard.id() + ". " + decisionText);
        summary.setLegalOptionsConsidered(legalCommands.size());
        summary.setFallbackHeuristicMode(false);
        summary.setStrategyModeUsed(BotStrategyMode.CARD_DRIVEN_SIMPLE_AUTOMA);

        List<String> eventSummaries = new ArrayList<>(syntheticEventSummaries);
        eventSummaries.addAll(producedEvents.stream().map(DomainEvent::description).toList());
        summary.setEventSummaries(eventSummaries);
        summary.setAutomaTrace(automaTrace);

        working.setLastBotTurnSummary(summary);
        working.appendLog("BOT_TURN", "[CAPITALIST] simple automa card " + mappedCard.id() + ": " + decisionText);

        return Optional.of(new ResolvedTurn(working, summary, producedEvents));
    }

    private CapitalistAutomaActionCard drawAutomaCard(GameState state) {
        List<CapitalistAutomaActionCard> cards = cardRegistry.actionCards();
        int cardIndex = Math.floorMod(state.getEventLog().size() + state.getCurrentRound(), cards.size());
        return cards.get(cardIndex);
    }

    private List<Map<String, Object>> runStartOfTurnAutoFreeActions(GameState state, PlayerState actor) {
        return List.of();
    }

    private Map<String, Object> runChangePrices(PlayerState actor) {
        Map<String, Integer> prices = actor.getPrices() == null ? Map.of() : actor.getPrices();
        if (prices.isEmpty()) {
            return freeAction("CHANGE_PRICES", "SKIPPED", List.of("NO_PRICE_TRACK_AVAILABLE"), Map.of(), "No prices to update.");
        }

        String targetResource = prices.keySet().stream()
                .sorted()
                .max(Comparator.comparingInt(actor::getProducedResourceAmount).thenComparing(String::compareTo))
                .orElse(prices.keySet().stream().sorted().findFirst().orElse(""));
        int before = actor.getPrice(targetResource);
        int after = Math.max(1, before - 1);
        actor.setPrice(targetResource, after);

        return freeAction(
                "CHANGE_PRICES",
                "EXECUTED",
                List.of(),
                Map.of("resource", targetResource, "before", before, "after", after),
                "Capitalist automa adjusted a market price at start of turn."
        );
    }

    private Map<String, Object> runGetBenefits(PlayerState actor) {
        int pendingBenefits = actor.getResourceAmount("benefits");
        if (pendingBenefits <= 0) {
            return freeAction("GET_BENEFITS", "SKIPPED", List.of("BENEFITS_NOT_APPLICABLE"), Map.of(), "No pending benefits.");
        }
        actor.consumeResource("benefits", pendingBenefits);
        int beforeMoney = actor.getMoney();
        actor.setMoney(beforeMoney + pendingBenefits);
        return freeAction(
                "GET_BENEFITS",
                "EXECUTED",
                List.of(),
                Map.of("benefitsClaimed", pendingBenefits, "moneyBefore", beforeMoney, "moneyAfter", actor.getMoney()),
                "Automa received benefits that were available in current state."
        );
    }

    private Map<String, Object> runRepayLoan(PlayerState actor) {
        int loans = actor.getResourceAmount("loan");
        if (loans <= 0) {
            return freeAction("REPAY_LOAN", "SKIPPED", List.of("NO_LOANS_TO_REPAY"), Map.of(), "No loans to repay.");
        }
        int repayable = Math.min(loans, actor.getMoney() / LOAN_REPAYMENT_COST);
        if (repayable <= 0) {
            return freeAction("REPAY_LOAN", "SKIPPED", List.of("INSUFFICIENT_FUNDS_FOR_LOAN_REPAYMENT"), Map.of("loans", loans), "Not enough money to repay a loan.");
        }
        int beforeMoney = actor.getMoney();
        int beforeLoans = loans;
        actor.consumeResource("loan", repayable);
        actor.setMoney(beforeMoney - (repayable * LOAN_REPAYMENT_COST));
        List<String> reasonCodes = repayable < beforeLoans ? List.of("PARTIAL_LOAN_REPAYMENT") : List.of();
        return freeAction(
                "REPAY_LOAN",
                "EXECUTED",
                reasonCodes,
                Map.of(
                        "repaid", repayable,
                        "loansBefore", beforeLoans,
                        "loansAfter", actor.getResourceAmount("loan"),
                        "moneyBefore", beforeMoney,
                        "moneyAfter", actor.getMoney()
                ),
                "Automa repaid loans that were currently payable."
        );
    }

    private Map<String, Object> runBuyStorageIfOverflow(PlayerState actor) {
        return freeAction(
                "BUY_STORAGE_IF_OVERFLOW",
                "SKIPPED",
                List.of("STORAGE_PURCHASE_NOT_SUPPORTED_IN_CURRENT_SLICE"),
                Map.of(),
                "Storage purchase flow is intentionally disabled until storage subsystem is installed."
        );
    }

    private Map<String, Object> freeAction(
            String action,
            String status,
            List<String> reasonCodes,
            Map<String, Object> changes,
            String note
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", action);
        result.put("status", status);
        result.put("reasonCodes", reasonCodes == null ? List.of() : reasonCodes);
        result.put("changes", changes == null ? Map.of() : changes);
        result.put("note", note == null ? "" : note);
        return result;
    }

    private AutomaActionEvaluation tryResolveActionSlot(
            GameState state,
            PlayerState actor,
            AutomaCard card,
            List<GameCommand> legalCommands,
            int slot,
            boolean withBonus
    ) {
        CapitalistAutomaActionSymbol symbol = switch (slot) {
            case 1 -> card.actionSlot1();
            case 2 -> card.actionSlot2();
            case 3 -> card.actionSlot3();
            case 4 -> card.actionSlot4();
            default -> null;
        };
        if (symbol == null) {
            return new AutomaActionEvaluation(
                    new AutomaActionCandidate(slot, AutomaActionType.NONE, "CARD_SLOT", false),
                    false,
                    List.of("SLOT_EMPTY"),
                    List.of(),
                    null,
                    false,
                    Map.of(),
                    List.of()
            );
        }

        AutomaActionType actionType = mapSymbol(symbol);
        AutomaActionCandidate candidate = new AutomaActionCandidate(slot, actionType, "CARD_SLOT", withBonus && card.firstActionBonus() != null);

        return switch (actionType) {
            case PROPOSE_BILL -> evaluateProposeBill(candidate, card, legalCommands, withBonus);
            case ASSIGN_WORKERS -> evaluateAssignWorkers(candidate, state, actor, legalCommands);
            case BUILD_ENTERPRISE -> evaluateBuildEnterprise(candidate, state, actor, card, withBonus);
            case SELL_TO_FOREIGN_MARKET -> evaluateSellToForeignMarket(candidate, state, actor, card, withBonus);
            case MAKE_DEAL -> evaluateMakeDeal(candidate, state, actor, symbol);
            case SELL_ENTERPRISE -> evaluateSellEnterprise(candidate, state, actor, symbol);
            case RECONFIGURE_EQUIPMENT -> unsupported(candidate, "UNSUPPORTED_RECONFIGURE_EQUIPMENT_EXECUTION_PATH");
            case REACT_TO_STRIKE -> unsupported(candidate, "UNSUPPORTED_REACT_TO_STRIKE_EXECUTION_PATH");
            default -> unsupported(candidate, "UNSUPPORTED_ACTION_TYPE");
        };
    }

    private AutomaActionEvaluation tryResolveSpecialAction(
            GameState state,
            PlayerState actor,
            AutomaCard card
    ) {
        AutomaActionCandidate candidate = new AutomaActionCandidate(0, AutomaActionType.SPECIAL_ACTION, "SPECIAL_ACTION", false);
        if (card.specialAction() == null || card.specialAction().type() == null || card.specialAction().type().isBlank()) {
            return new AutomaActionEvaluation(candidate, false, List.of("SPECIAL_ACTION_NOT_DEFINED"), List.of(), null, false, Map.of(), List.of());
        }

        String type = card.specialAction().type().toUpperCase(Locale.ROOT);
        if ("DRAW_AND_FILTER_CAPITALIST_VOTE_CUBES_FROM_BAG".equals(type)) {
            return new AutomaActionEvaluation(
                    candidate,
                    true,
                    List.of(),
                    List.of("SPECIAL:DRAW_AND_FILTER_CAPITALIST_VOTE_CUBES_FROM_BAG"),
                    null,
                    false,
                    Map.of("type", type),
                    List.of("special_action_supported")
            );
        }
        return new AutomaActionEvaluation(
                candidate,
                false,
                List.of("UNSUPPORTED_SPECIAL_ACTION_" + type),
                List.of(),
                null,
                false,
                Map.of("type", type),
                List.of("special_action_not_supported")
        );
    }

    private AutomaActionEvaluation evaluateProposeBill(
            AutomaActionCandidate candidate,
            AutomaCard card,
            List<GameCommand> legalCommands,
            boolean withBonus
    ) {
        List<ProposeBillCommand> candidates = legalCommands.stream()
                .filter(command -> command.type() == ActionType.PROPOSE_BILL)
                .filter(command -> command instanceof ProposeBillCommand)
                .map(command -> (ProposeBillCommand) command)
                .toList();

        if (candidates.isEmpty()) {
            return new AutomaActionEvaluation(candidate, false, List.of("NO_LEGAL_PROPOSE_BILL_COMMAND"), List.of(), null, false, Map.of(), List.of());
        }

        List<PolicyId> preferred = card.policyTags().stream()
                .map(this::mapPolicyTagToPolicyId)
                .filter(Objects::nonNull)
                .toList();

        Optional<ProposeBillCommand> matchingPreferred = preferred.isEmpty()
                ? Optional.empty()
                : candidates.stream().filter(command -> preferred.contains(command.policyId())).findFirst();

        if (matchingPreferred.isPresent()) {
            ProposeBillCommand command = matchingPreferred.get();
            return new AutomaActionEvaluation(
                    candidate,
                    true,
                    List.of(),
                    List.of(command.moveId()),
                    command,
                    false,
                    Map.of("policyId", command.policyId().name(), "targetCourse", command.targetCourse().name()),
                    List.of("policy_tag_priority_order")
            );
        }

        if (!preferred.isEmpty() && !withBonus) {
            return new AutomaActionEvaluation(
                    candidate,
                    false,
                    List.of("POLICY_TAG_TARGET_NOT_AVAILABLE"),
                    candidates.stream().map(GameCommand::moveId).toList(),
                    null,
                    false,
                    Map.of("preferredPolicies", preferred.stream().map(Enum::name).toList()),
                    List.of("strict_policy_tag_gate")
            );
        }

        ProposeBillCommand fallback = candidates.getFirst();
        boolean bonusApplied = !preferred.isEmpty() && withBonus && card.firstActionBonus() != null;
        return new AutomaActionEvaluation(
                candidate,
                true,
                bonusApplied ? List.of("FIRST_ACTION_BONUS_RELAXED_POLICY_TAG") : List.of(),
                List.of(fallback.moveId()),
                fallback,
                bonusApplied,
                Map.of("policyId", fallback.policyId().name(), "targetCourse", fallback.targetCourse().name()),
                bonusApplied ? List.of("first_action_bonus_relaxed_policy_tag") : List.of("fallback_first_legal_policy")
        );
    }

    private AutomaActionEvaluation evaluateAssignWorkers(
            AutomaActionCandidate candidate,
            GameState state,
            PlayerState actor,
            List<GameCommand> legalCommands
    ) {
        List<AssignWorkersCommand> candidates = legalCommands.stream()
                .filter(command -> command.type() == ActionType.ASSIGN_WORKERS)
                .filter(command -> command instanceof AssignWorkersCommand)
                .map(command -> (AssignWorkersCommand) command)
                .toList();

        if (candidates.isEmpty()) {
            List<String> reasons = new ArrayList<>();
            boolean hasFreeWorkers = state.getWorkers().stream()
                    .anyMatch(worker -> worker.getClassType() == actor.getClassType()
                            && worker.getLocation() == WorkerLocation.UNEMPLOYED
                            && !worker.isTiedContract());
            if (!hasFreeWorkers) {
                reasons.add("NO_AVAILABLE_WORKERS");
            }
            boolean hasEmptyEnterprise = state.getEnterprises().stream()
                    .anyMatch(enterprise -> enterprise.getOwnerClass() == actor.getClassType() && enterprise.isFullyEmpty());
            if (!hasEmptyEnterprise) {
                reasons.add("NO_ASSIGNABLE_ENTERPRISE");
            }
            if (reasons.isEmpty()) {
                reasons.add("NO_LEGAL_ASSIGN_WORKERS_COMMAND");
            }
            return new AutomaActionEvaluation(candidate, false, reasons, List.of(), null, false, Map.of(), List.of("no_assign_workers_candidate"));
        }

        AssignWorkersCommand selected = candidates.stream()
                .max(Comparator.comparingInt(command -> command.assignments() == null ? 0 : command.assignments().size()))
                .orElse(candidates.getFirst());
        return new AutomaActionEvaluation(
                candidate,
                true,
                List.of(),
                List.of(selected.moveId()),
                selected,
                false,
                Map.of("assignmentCount", selected.assignments() == null ? 0 : selected.assignments().size()),
                List.of("max_assignments_priority")
        );
    }

    private AutomaActionEvaluation evaluateBuildEnterprise(
            AutomaActionCandidate candidate,
            GameState state,
            PlayerState actor,
            AutomaCard card,
            boolean withBonus
    ) {
        List<SetupSpecModel.EnterpriseDefinition> marketCandidates = listBuildMarketCandidates(state);
        if (marketCandidates.isEmpty()) {
            return new AutomaActionEvaluation(
                    candidate,
                    false,
                    List.of("NO_BUILDABLE_ENTERPRISE_ON_MARKET"),
                    List.of(),
                    null,
                    false,
                    Map.of(),
                    List.of("empty_market_registry")
            );
        }

        int bonusDiscount = withBonus ? resolveFirstActionMoneyDiscount(card.firstActionBonus()) : 0;
        boolean bonusApplied = false;
        List<BuildCandidatePlan> viable = new ArrayList<>();
        boolean blockedByMoney = false;
        boolean blockedByWorkers = false;

        for (SetupSpecModel.EnterpriseDefinition definition : marketCandidates) {
            int baseCost = Math.max(0, definition.getCost());
            int netCost = Math.max(0, baseCost - bonusDiscount);
            if (actor.getMoney() < netCost) {
                blockedByMoney = true;
                continue;
            }
            WorkerPlan workerPlan = planWorkersForDefinition(state, definition);
            boolean needsWorkers = !definition.isAutomated() && workerPlan.requiredSlots() > 0;
            if (needsWorkers && !workerPlan.canFullyStaff()) {
                blockedByWorkers = true;
                continue;
            }

            BuildCandidatePlan candidatePlan = new BuildCandidatePlan(
                    definition.getId(),
                    definition.getName(),
                    baseCost,
                    netCost,
                    definition.isAutomated(),
                    producedResourceFrom(definition),
                    workerPlan.requiredSlots(),
                    workerPlan.selectedWorkerIds()
            );
            viable.add(candidatePlan);
            if (baseCost > netCost) {
                bonusApplied = true;
            }
        }

        if (viable.isEmpty()) {
            List<String> reasons = new ArrayList<>();
            if (blockedByMoney) {
                reasons.add("INSUFFICIENT_FUNDS_TO_BUILD_ENTERPRISE");
            }
            if (blockedByWorkers) {
                reasons.add("NO_AVAILABLE_WORKERS_FOR_FUNCTIONING_ENTERPRISE");
            }
            if (reasons.isEmpty()) {
                reasons.add("NO_VIABLE_BUILD_ENTERPRISE_TARGET");
            }
            return new AutomaActionEvaluation(candidate, false, reasons, List.of(), null, false, Map.of(), List.of("no_viable_build_candidate"));
        }

        BuildResolution resolution = resolveBuildEnterpriseTarget(state, viable);
        List<String> reasonCodes = new ArrayList<>();
        if (bonusApplied && resolution.selected().baseCost() > resolution.selected().netCost()) {
            reasonCodes.add("FIRST_ACTION_BONUS_MONEY_DISCOUNT_APPLIED");
        }

        Map<String, Object> resolvedTarget = new LinkedHashMap<>();
        resolvedTarget.put("enterpriseId", resolution.selected().enterpriseId());
        resolvedTarget.put("enterpriseName", resolution.selected().enterpriseName());
        resolvedTarget.put("cost", resolution.selected().baseCost());
        resolvedTarget.put("netCost", resolution.selected().netCost());
        resolvedTarget.put("automated", resolution.selected().automated());
        resolvedTarget.put("assignedWorkerIds", resolution.selected().assignedWorkerIds());
        resolvedTarget.put("producedResource", resolution.selected().producedResource());

        return new AutomaActionEvaluation(
                candidate,
                true,
                reasonCodes,
                List.of("AUTOMA_BUILD_ENTERPRISE:" + resolution.selected().enterpriseId()),
                null,
                bonusApplied && resolution.selected().baseCost() > resolution.selected().netCost(),
                resolvedTarget,
                resolution.reasonPath()
        );
    }

    private AutomaActionEvaluation evaluateSellEnterprise(
            AutomaActionCandidate candidate,
            GameState state,
            PlayerState actor,
            CapitalistAutomaActionSymbol symbol
    ) {
        List<SellCandidatePlan> legal = new ArrayList<>();
        boolean tiedContractBlocked = false;

        for (Enterprise enterprise : state.getEnterprises()) {
            if (enterprise.getOwnerClass() != actor.getClassType()) {
                continue;
            }
            List<Worker> occupiedWorkers = occupiedWorkersForEnterprise(state, enterprise);
            if (occupiedWorkers.stream().anyMatch(Worker::isTiedContract)) {
                tiedContractBlocked = true;
                continue;
            }

            int sellValue = Math.max(SELL_ENTERPRISE_FALLBACK_VALUE, Math.max(0, enterprise.getCost() / 2));
            int matchingUnemployed = matchingUnemployedCount(state, enterprise);
            legal.add(new SellCandidatePlan(
                    enterprise.getId(),
                    enterprise.getName(),
                    sellValue,
                    matchingUnemployed,
                    enterprise.getSlots().size() >= 4
            ));
        }

        if (legal.isEmpty()) {
            List<String> reasons = new ArrayList<>();
            if (tiedContractBlocked) {
                reasons.add("ENTERPRISE_HAS_TIED_CONTRACT_WORKERS");
            }
            reasons.add("NO_SELLABLE_ENTERPRISE");
            return new AutomaActionEvaluation(candidate, false, reasons, List.of(), null, false, Map.of(), List.of("no_legal_sell_target"));
        }

        List<String> selectors = resolveInstructionPrioritySelectors(symbol);
        SellResolution resolution = resolveSellEnterpriseTarget(legal, selectors);
        Map<String, Object> resolvedTarget = Map.of(
                "enterpriseId", resolution.selected().enterpriseId(),
                "enterpriseName", resolution.selected().enterpriseName(),
                "sellValue", resolution.selected().sellValue()
        );
        return new AutomaActionEvaluation(
                candidate,
                true,
                List.of(),
                List.of("AUTOMA_SELL_ENTERPRISE:" + resolution.selected().enterpriseId()),
                null,
                false,
                resolvedTarget,
                resolution.reasonPath()
        );
    }

    private AutomaActionEvaluation evaluateSellToForeignMarket(
            AutomaActionCandidate candidate,
            GameState state,
            PlayerState actor,
            AutomaCard card,
            boolean withBonus
    ) {
        ExportCardState active = state.getActiveExportCard();
        if (active == null || active.getCardId() == null || active.getCardId().isBlank()) {
            return new AutomaActionEvaluation(
                    candidate,
                    false,
                    List.of("NO_ACTIVE_EXPORT_CARD"),
                    List.of(),
                    null,
                    false,
                    Map.of(),
                    List.of("export_card_missing")
            );
        }
        if (active.getAvailableOperations() <= 0) {
            return new AutomaActionEvaluation(
                    candidate,
                    false,
                    List.of("NO_AVAILABLE_EXPORT_OPERATIONS"),
                    List.of(),
                    null,
                    false,
                    Map.of("exportCardId", active.getCardId()),
                    List.of("export_operations_exhausted")
            );
        }

        ExportResolution resolution = resolveExportOperations(state, actor, card, withBonus, false);
        if (resolution.selectedOperations().isEmpty()) {
            return new AutomaActionEvaluation(
                    candidate,
                    false,
                    List.of("NO_EXPORT_OPERATION_EXECUTABLE"),
                    List.of(),
                    null,
                    false,
                    Map.of("exportCardId", active.getCardId()),
                    List.of("no_viable_export_offer")
            );
        }

        Map<String, Object> resolvedTarget = new LinkedHashMap<>();
        resolvedTarget.put("exportCardId", active.getCardId());
        resolvedTarget.put("operations", resolution.selectedOperations().stream().map(this::exportOperationToTrace).toList());
        resolvedTarget.put("baseRevenue", resolution.baseRevenue());
        resolvedTarget.put("bonusRevenue", resolution.bonusRevenue());
        resolvedTarget.put("totalRevenue", resolution.totalRevenue());

        return new AutomaActionEvaluation(
                candidate,
                true,
                List.of(),
                List.of("AUTOMA_SELL_TO_FOREIGN_MARKET:" + active.getCardId()),
                null,
                resolution.bonusRevenue() > 0,
                resolvedTarget,
                resolution.reasonPath()
        );
    }

    private AutomaActionEvaluation evaluateMakeDeal(
            AutomaActionCandidate candidate,
            GameState state,
            PlayerState actor,
            CapitalistAutomaActionSymbol symbol
    ) {
        List<BusinessDealCard> visibleDeals = currentVisibleDeals(state);
        if (visibleDeals.isEmpty()) {
            return new AutomaActionEvaluation(
                    candidate,
                    false,
                    List.of("NO_VISIBLE_BUSINESS_DEAL"),
                    List.of(),
                    null,
                    false,
                    Map.of(),
                    List.of("business_deal_market_empty")
            );
        }

        List<DealCandidatePlan> viable = new ArrayList<>();
        boolean blockedByMoney = false;
        boolean blockedByStorage = false;

        for (BusinessDealCard card : visibleDeals) {
            DealCandidatePlan plan = assessDealCandidate(actor, card);
            if (!plan.canExecute()) {
                if (plan.reasonCodes().contains("INSUFFICIENT_FUNDS_FOR_DEAL")) {
                    blockedByMoney = true;
                }
                if (plan.reasonCodes().contains("STORAGE_LIMIT_EXCEEDED")) {
                    blockedByStorage = true;
                }
                continue;
            }
            viable.add(plan);
        }

        if (viable.isEmpty()) {
            List<String> reasons = new ArrayList<>();
            if (blockedByMoney) {
                reasons.add("INSUFFICIENT_FUNDS_FOR_DEAL");
            }
            if (blockedByStorage) {
                reasons.add("STORAGE_LIMIT_EXCEEDED");
            }
            if (reasons.isEmpty()) {
                reasons.add("NO_EXECUTABLE_DEAL");
            }
            return new AutomaActionEvaluation(candidate, false, reasons, List.of(), null, false, Map.of(), List.of("no_viable_deal"));
        }

        List<String> selectors = resolveInstructionPrioritySelectors(symbol);
        DealResolution resolution = resolveDealChoice(viable, selectors);
        Map<String, Object> resolvedTarget = new LinkedHashMap<>();
        resolvedTarget.put("dealId", resolution.selected().dealId());
        resolvedTarget.put("dealTitle", resolution.selected().title());
        resolvedTarget.put("baseCost", resolution.selected().baseCost());
        resolvedTarget.put("dutyCost", resolution.selected().dutyCost());
        resolvedTarget.put("totalCost", resolution.selected().totalCost());
        resolvedTarget.put("storedInFreeTradeZone", resolution.selected().storedInFreeTradeZone());
        resolvedTarget.put("storedInRegularStorage", resolution.selected().storedInRegularStorage());

        return new AutomaActionEvaluation(
                candidate,
                true,
                List.of(),
                List.of("AUTOMA_MAKE_DEAL:" + resolution.selected().dealId()),
                null,
                false,
                resolvedTarget,
                resolution.reasonPath()
        );
    }

    private ActionMutationResult executeResolvedAutomaAction(
            GameState state,
            String actorPlayerId,
            AutomaCard card,
            AutomaActionEvaluation evaluation
    ) {
        PlayerState actor = state.findPlayerById(actorPlayerId).orElse(null);
        if (actor == null) {
            return ActionMutationResult.failed("AUTOMA_ACTOR_NOT_FOUND");
        }
        return switch (evaluation.candidate().actionType()) {
            case BUILD_ENTERPRISE -> executeBuildEnterprise(state, actor, evaluation);
            case SELL_ENTERPRISE -> executeSellEnterprise(state, actor, evaluation);
            case SELL_TO_FOREIGN_MARKET -> executeSellToForeignMarket(state, actor, card, evaluation);
            case MAKE_DEAL -> executeMakeDeal(state, actor, evaluation);
            default -> ActionMutationResult.failed("UNSUPPORTED_MUTATION_ACTION");
        };
    }

    private ActionMutationResult executeBuildEnterprise(
            GameState state,
            PlayerState actor,
            AutomaActionEvaluation evaluation
    ) {
        String enterpriseId = String.valueOf(evaluation.resolvedTarget().getOrDefault("enterpriseId", ""));
        SetupSpecModel.EnterpriseDefinition definition = capitalistEnterpriseDefinitions.get(enterpriseId);
        if (definition == null) {
            return ActionMutationResult.failed("BUILD_TARGET_DEFINITION_NOT_FOUND");
        }

        int netCost = asInt(evaluation.resolvedTarget().get("netCost"), Math.max(0, definition.getCost()));
        if (actor.getMoney() < netCost) {
            return ActionMutationResult.failed("INSUFFICIENT_FUNDS_TO_BUILD_ENTERPRISE");
        }

        Enterprise enterprise = instantiateEnterprise(definition);
        if (state.findEnterprise(enterprise.getId()).isPresent()) {
            return ActionMutationResult.failed("BUILD_TARGET_ALREADY_EXISTS");
        }

        List<String> workerIds = asStringList(evaluation.resolvedTarget().get("assignedWorkerIds"));
        Map<String, Worker> workerMap = state.getWorkers().stream()
                .collect(Collectors.toMap(Worker::getId, worker -> worker, (left, right) -> left));
        List<Worker> selectedWorkers = workerIds.stream()
                .map(workerMap::get)
                .filter(Objects::nonNull)
                .toList();
        List<String> assignedWorkerSummaries = new ArrayList<>();
        if (!enterprise.isAutomated() && !enterprise.getSlots().isEmpty()) {
            if (selectedWorkers.size() < enterprise.getSlots().size()) {
                return ActionMutationResult.failed("INSUFFICIENT_WORKERS_FOR_SELECTED_ENTERPRISE");
            }
            for (EnterpriseSlot slot : enterprise.getSlots()) {
                Worker matched = selectedWorkers.stream()
                        .filter(worker -> worker.getLocation() == WorkerLocation.UNEMPLOYED && !worker.isTiedContract())
                        .filter(worker -> slotMatchesWorker(slot, worker))
                        .findFirst()
                        .orElse(null);
                if (matched == null) {
                    return ActionMutationResult.failed("WORKER_ASSIGNMENT_CONFLICT_FOR_BUILD");
                }
                slot.setOccupiedWorkerId(matched.getId());
                matched.setLocation(WorkerLocation.ENTERPRISE_SLOT);
                matched.setEnterpriseId(enterprise.getId());
                matched.setSlotId(slot.getId());
                matched.setTiedContract(true);
                assignedWorkerSummaries.add(
                        matched.getId() + "->" + slot.getId() + " (" + workerColorLabel(matched) + ", TD)"
                );
                selectedWorkers = selectedWorkers.stream()
                        .filter(worker -> !Objects.equals(worker.getId(), matched.getId()))
                        .toList();
            }
        }

        actor.setMoney(actor.getMoney() - netCost);
        state.getEnterprises().add(enterprise);
        String workerLog = assignedWorkerSummaries.isEmpty()
                ? ""
                : " Assigned workers under labor contract: " + String.join(", ", assignedWorkerSummaries) + ".";
        state.appendLog("AUTOMA_BUILD_ENTERPRISE", actor.getPlayerId() + " built enterprise " + enterprise.getId() + " for " + netCost + "." + workerLog);

        return new ActionMutationResult(
                true,
                List.of("Automa built enterprise " + enterprise.getId() + " for " + netCost + "." + workerLog),
                evaluation.resolvedTarget(),
                evaluation.reasonPath(),
                ""
        );
    }

    private ActionMutationResult executeSellEnterprise(
            GameState state,
            PlayerState actor,
            AutomaActionEvaluation evaluation
    ) {
        String enterpriseId = String.valueOf(evaluation.resolvedTarget().getOrDefault("enterpriseId", ""));
        Enterprise enterprise = state.findEnterprise(enterpriseId).orElse(null);
        if (enterprise == null) {
            return ActionMutationResult.failed("SELL_TARGET_NOT_FOUND");
        }

        int sellValue = asInt(evaluation.resolvedTarget().get("sellValue"), Math.max(SELL_ENTERPRISE_FALLBACK_VALUE, enterprise.getCost() / 2));

        for (EnterpriseSlot slot : enterprise.getSlots()) {
            if (!slot.isOccupied()) {
                continue;
            }
            Worker worker = state.findWorker(slot.getOccupiedWorkerId()).orElse(null);
            if (worker == null) {
                continue;
            }
            if (worker.isTiedContract()) {
                return ActionMutationResult.failed("ENTERPRISE_HAS_TIED_CONTRACT_WORKERS");
            }
            worker.setLocation(WorkerLocation.UNEMPLOYED);
            worker.setEnterpriseId(null);
            worker.setSlotId(null);
            worker.setTiedContract(false);
            slot.setOccupiedWorkerId(null);
        }

        state.setEnterprises(state.getEnterprises().stream()
                .filter(item -> !Objects.equals(item.getId(), enterpriseId))
                .collect(Collectors.toCollection(ArrayList::new)));
        actor.setMoney(actor.getMoney() + sellValue);
        state.appendLog("AUTOMA_SELL_ENTERPRISE", actor.getPlayerId() + " sold enterprise " + enterpriseId + " for " + sellValue + ".");

        return new ActionMutationResult(
                true,
                List.of("Automa sold enterprise " + enterpriseId + " for " + sellValue + "."),
                evaluation.resolvedTarget(),
                evaluation.reasonPath(),
                ""
        );
    }

    private ActionMutationResult executeSellToForeignMarket(
            GameState state,
            PlayerState actor,
            AutomaCard card,
            AutomaActionEvaluation evaluation
    ) {
        ExportResolution resolution = resolveExportOperations(
                state,
                actor,
                card,
                evaluation.candidate().slot() == 1 && evaluation.candidate().bonusConsidered(),
                true
        );
        if (resolution.selectedOperations().isEmpty()) {
            return ActionMutationResult.failed("NO_EXPORT_OPERATION_EXECUTABLE");
        }

        int gained = 0;
        for (ExportOperation operation : resolution.selectedOperations()) {
            int consumed = consumeResourceWithFreeTradeZone(actor, operation.resourceId(), operation.quantity());
            if (consumed < operation.quantity()) {
                return ActionMutationResult.failed("EXPORT_RESOURCE_CONSUMPTION_FAILED");
            }
            gained += operation.revenue();
        }
        actor.setMoney(actor.getMoney() + gained + resolution.bonusRevenue());
        ExportCardState activeCard = state.getActiveExportCard();
        activeCard.setAvailableOperations(Math.max(0, activeCard.getAvailableOperations() - resolution.selectedOperations().size()));
        state.setActiveExportCard(activeCard);

        List<String> opSummary = resolution.selectedOperations().stream()
                .map(op -> op.resourceId() + " x" + op.quantity())
                .toList();
        state.appendLog("AUTOMA_EXPORT", actor.getPlayerId() + " exported via " + activeCard.getCardId() + ": " + String.join(", ", opSummary) + ".");

        Map<String, Object> resolvedTarget = new LinkedHashMap<>(evaluation.resolvedTarget());
        resolvedTarget.put("executedOperations", resolution.selectedOperations().stream().map(this::exportOperationToTrace).toList());

        return new ActionMutationResult(
                true,
                List.of("Automa sold to foreign market: " + String.join(", ", opSummary) + "."),
                resolvedTarget,
                resolution.reasonPath(),
                ""
        );
    }

    private ActionMutationResult executeMakeDeal(
            GameState state,
            PlayerState actor,
            AutomaActionEvaluation evaluation
    ) {
        String dealId = String.valueOf(evaluation.resolvedTarget().getOrDefault("dealId", ""));
        BusinessDealCard card = state.findBusinessDealCard(dealId).orElse(null);
        if (card == null) {
            return ActionMutationResult.failed("DEAL_CARD_NOT_FOUND");
        }
        DealCandidatePlan plan = assessDealCandidate(actor, card);
        if (!plan.canExecute()) {
            return ActionMutationResult.failed("DEAL_NO_LONGER_EXECUTABLE");
        }

        if (actor.getMoney() < plan.totalCost()) {
            return ActionMutationResult.failed("INSUFFICIENT_FUNDS_FOR_DEAL");
        }
        actor.setMoney(actor.getMoney() - plan.totalCost());
        for (Map.Entry<String, Integer> entry : plan.storedInRegularStorage().entrySet()) {
            actor.addProducedResource(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Integer> entry : plan.storedInFreeTradeZone().entrySet()) {
            actor.addProducedResource(freeTradeZoneResourceKey(entry.getKey()), entry.getValue());
        }

        consumeBusinessDealCardFromMarket(state, dealId);
        state.appendLog(
                "AUTOMA_MAKE_DEAL",
                actor.getPlayerId() + " made deal " + dealId + " (cost=" + plan.totalCost()
                        + ", free_trade_zone=" + plan.storedInFreeTradeZone()
                        + ", regular_storage=" + plan.storedInRegularStorage() + ")."
        );

        Map<String, Object> resolvedTarget = new LinkedHashMap<>(evaluation.resolvedTarget());
        resolvedTarget.put("storedInFreeTradeZone", plan.storedInFreeTradeZone());
        resolvedTarget.put("storedInRegularStorage", plan.storedInRegularStorage());

        return new ActionMutationResult(
                true,
                List.of("Automa made deal " + dealId + "."),
                resolvedTarget,
                evaluation.reasonPath(),
                ""
        );
    }

    private String applySpecialAction(GameState state, PlayerState actor, CapitalistAutomaSpecialAction specialAction) {
        if (specialAction == null || specialAction.type() == null) {
            return "Special action skipped: missing definition.";
        }
        String type = specialAction.type().toUpperCase(Locale.ROOT);
        if (!"DRAW_AND_FILTER_CAPITALIST_VOTE_CUBES_FROM_BAG".equals(type)) {
            return "Special action skipped: unsupported special action type " + type + ".";
        }

        int toDraw = parseIntParam(specialAction.params(), "cubes_to_draw", 6);
        VotingBagState bag = state.getVotingBag();
        int beforeWorker = bag.getWorker();
        int beforeMiddle = bag.getMiddleClass();
        int removedWorker = Math.min(toDraw, beforeWorker);
        int remaining = Math.max(0, toDraw - removedWorker);
        int removedMiddle = Math.min(remaining, beforeMiddle);
        bag.setWorker(beforeWorker - removedWorker);
        bag.setMiddleClass(beforeMiddle - removedMiddle);
        return "Special action executed: filtered " + (removedWorker + removedMiddle)
                + " non-capitalist cubes from bag (worker=" + removedWorker + ", middle_class=" + removedMiddle + ").";
    }

    private void applyPoliticalPressure(GameState state, PlayerState actor) {
        state.getVotingBag().add(VotingCubeOwnerClass.CAPITALIST, POLITICAL_PRESSURE_CUBES);
    }

    private String resolveFallbackReason(List<AutomaActionEvaluation> checkedSlots, AutomaActionEvaluation special) {
        if (special != null && !special.canExecute()) {
            return "All base slots unavailable; special action unavailable: " + String.join(", ", special.reasonCodes());
        }
        String slotReasons = checkedSlots.stream()
                .map(evaluation -> "slot" + evaluation.candidate().slot() + "=" + String.join("|", evaluation.reasonCodes()))
                .reduce((left, right) -> left + "; " + right)
                .orElse("no slot details");
        return "All base slots unavailable: " + slotReasons;
    }

    private String buildDecisionText(
            List<AutomaActionEvaluation> checkedSlots,
            AutomaActionEvaluation special,
            String selectedAutomaAction,
            Map<String, Object> resolvedTarget,
            SnapVoteDecision snapVoteDecision
    ) {
        List<String> segments = new ArrayList<>();
        for (AutomaActionEvaluation evaluation : checkedSlots) {
            if (evaluation.canExecute()) {
                segments.add("Slot " + evaluation.candidate().slot() + ": " + evaluation.candidate().actionType() + " executed.");
                break;
            }
            segments.add("Slot " + evaluation.candidate().slot() + " unavailable: " + String.join(", ", evaluation.reasonCodes()) + ".");
        }
        if (special != null && checkedSlots.stream().noneMatch(AutomaActionEvaluation::canExecute)) {
            if (special.canExecute()) {
                segments.add("Special action executed.");
            } else {
                segments.add("Special action unavailable: " + String.join(", ", special.reasonCodes()) + ".");
            }
        }
        if ("APPLY_POLITICAL_PRESSURE".equals(selectedAutomaAction)) {
            segments.add("Political pressure applied.");
        }
        if (!resolvedTarget.isEmpty()) {
            segments.add("Resolved target: " + compactResolvedTarget(resolvedTarget) + ".");
        }
        if (snapVoteDecision.applicable()) {
            segments.add("Snap vote " + (snapVoteDecision.triggered() ? "triggered" : "skipped")
                    + " (" + String.join(", ", snapVoteDecision.reasonCodes()) + ").");
        }
        return String.join(" ", segments);
    }

    private Map<String, Object> evaluationToTrace(AutomaActionEvaluation evaluation) {
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("slot", evaluation.candidate().slot());
        trace.put("source", evaluation.candidate().source());
        trace.put("actionType", evaluation.candidate().actionType().name());
        trace.put("bonusConsidered", evaluation.candidate().bonusConsidered());
        trace.put("bonusApplied", evaluation.bonusApplied());
        trace.put("canExecute", evaluation.canExecute());
        trace.put("reasonCodes", evaluation.reasonCodes());
        trace.put("plannedCommands", evaluation.plannedCommands());
        trace.put("resolvedTarget", evaluation.resolvedTarget());
        trace.put("reasonPath", evaluation.reasonPath());
        return trace;
    }

    private Map<String, Object> buildStateChanges(GameState before, GameState after, String actorPlayerId) {
        Map<String, Object> changes = new LinkedHashMap<>();
        PlayerState beforeActor = before.findPlayerById(actorPlayerId).orElse(null);
        PlayerState afterActor = after.findPlayerById(actorPlayerId).orElse(null);
        if (beforeActor != null && afterActor != null) {
            Map<String, Object> actorChanges = new LinkedHashMap<>();
            if (beforeActor.getMoney() != afterActor.getMoney()) {
                actorChanges.put("money", Map.of("before", beforeActor.getMoney(), "after", afterActor.getMoney()));
            }
            if (!Objects.equals(beforeActor.getPrices(), afterActor.getPrices())) {
                actorChanges.put("prices", Map.of("before", beforeActor.getPrices(), "after", afterActor.getPrices()));
            }
            if (!Objects.equals(beforeActor.getProducedResourceStorage(), afterActor.getProducedResourceStorage())) {
                actorChanges.put("producedResourceStorage", Map.of(
                        "before", beforeActor.getProducedResourceStorage(),
                        "after", afterActor.getProducedResourceStorage()
                ));
            }
            if (!Objects.equals(beforeActor.getGoodsAndServicesArea(), afterActor.getGoodsAndServicesArea())) {
                actorChanges.put("goodsAndServicesArea", Map.of(
                        "before", beforeActor.getGoodsAndServicesArea(),
                        "after", afterActor.getGoodsAndServicesArea()
                ));
            }
            if (beforeActor.getInfluence() != afterActor.getInfluence()) {
                actorChanges.put("influence", Map.of("before", beforeActor.getInfluence(), "after", afterActor.getInfluence()));
            }
            if (!actorChanges.isEmpty()) {
                changes.put("actor", actorChanges);
            }
        }

        Set<String> beforeEnterprises = before.getEnterprises().stream().map(Enterprise::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> afterEnterprises = after.getEnterprises().stream().map(Enterprise::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        if (!beforeEnterprises.equals(afterEnterprises)) {
            Set<String> added = new LinkedHashSet<>(afterEnterprises);
            added.removeAll(beforeEnterprises);
            Set<String> removed = new LinkedHashSet<>(beforeEnterprises);
            removed.removeAll(afterEnterprises);
            changes.put("enterprises", Map.of(
                    "beforeCount", beforeEnterprises.size(),
                    "afterCount", afterEnterprises.size(),
                    "added", added,
                    "removed", removed
            ));
        }

        VotingBagState beforeBag = before.getVotingBag();
        VotingBagState afterBag = after.getVotingBag();
        if (beforeBag.getWorker() != afterBag.getWorker()
                || beforeBag.getMiddleClass() != afterBag.getMiddleClass()
                || beforeBag.getCapitalist() != afterBag.getCapitalist()) {
            changes.put("votingBag", Map.of(
                    "before", Map.of(
                            "worker", beforeBag.getWorker(),
                            "middleClass", beforeBag.getMiddleClass(),
                            "capitalist", beforeBag.getCapitalist()
                    ),
                    "after", Map.of(
                            "worker", afterBag.getWorker(),
                            "middleClass", afterBag.getMiddleClass(),
                            "capitalist", afterBag.getCapitalist()
                    )
            ));
        }
        changes.put("phase", Map.of("before", before.getCurrentPhase().name(), "after", after.getCurrentPhase().name()));
        changes.put("currentPlayer", Map.of(
                "before", before.currentPlayer() == null ? "" : before.currentPlayer().getPlayerId(),
                "after", after.currentPlayer() == null ? "" : after.currentPlayer().getPlayerId()
        ));
        return changes;
    }

    private int parseIntParam(Map<String, Object> params, String key, int defaultValue) {
        if (params == null || params.isEmpty()) {
            return defaultValue;
        }
        Object raw = params.get(key);
        if (raw == null) {
            return defaultValue;
        }
        if (raw instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(raw));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private AutomaCard toAutomaCard(CapitalistAutomaActionCard card) {
        List<CapitalistAutomaActionSymbol> checks = card.checks() == null ? List.of() : card.checks();
        return new AutomaCard(
                "capitalist-" + card.cardNo(),
                card.cardNo(),
                checks.size() > 0 ? checks.get(0) : null,
                checks.size() > 1 ? checks.get(1) : null,
                checks.size() > 2 ? checks.get(2) : null,
                checks.size() > 3 ? checks.get(3) : null,
                card.bonus(),
                card.policyTags() == null ? List.of() : card.policyTags(),
                card.specialAction(),
                0
        );
    }

    private AutomaActionType mapSymbol(CapitalistAutomaActionSymbol symbol) {
        return switch (symbol) {
            case PROPOSE_BILL -> AutomaActionType.PROPOSE_BILL;
            case BUILD_ENTERPRISE -> AutomaActionType.BUILD_ENTERPRISE;
            case SELL_ON_EXTERNAL_MARKET -> AutomaActionType.SELL_TO_FOREIGN_MARKET;
            case ASSIGN_WORKERS -> AutomaActionType.ASSIGN_WORKERS;
            case SELL_ENTERPRISE -> AutomaActionType.SELL_ENTERPRISE;
            case LOBBY_INTERESTS -> AutomaActionType.MAKE_DEAL;
            case REACT_TO_STRIKE -> AutomaActionType.REACT_TO_STRIKE;
            case RECONFIGURE_EQUIPMENT -> AutomaActionType.RECONFIGURE_EQUIPMENT;
        };
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

    private Map<String, SetupSpecModel.EnterpriseDefinition> loadEnterpriseDefinitions(SetupSpecLoader setupSpecLoader) {
        if (setupSpecLoader == null) {
            return Map.of();
        }
        try {
            SetupSpecModel spec = setupSpecLoader.load();
            return spec.getCapitalistEnterpriseDefinitions() == null
                    ? Map.of()
                    : new HashMap<>(spec.getCapitalistEnterpriseDefinitions());
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private List<SetupSpecModel.EnterpriseDefinition> listBuildMarketCandidates(GameState state) {
        if (capitalistEnterpriseDefinitions.isEmpty()) {
            return List.of();
        }
        Set<String> existingIds = state.getEnterprises().stream()
                .map(Enterprise::getId)
                .collect(Collectors.toSet());
        return capitalistEnterpriseDefinitions.values().stream()
                .filter(Objects::nonNull)
                .filter(definition -> !definition.isStarting())
                .filter(definition -> definition.getId() != null && !definition.getId().isBlank())
                .filter(definition -> !existingIds.contains(definition.getId()))
                .sorted(Comparator.comparingInt(SetupSpecModel.EnterpriseDefinition::getCost)
                        .thenComparing(definition -> definition.getId().toLowerCase(Locale.ROOT)))
                .toList();
    }

    private int resolveFirstActionMoneyDiscount(CapitalistAutomaBonus bonus) {
        if (bonus == null || bonus.effectType() == null) {
            return 0;
        }
        String effectType = bonus.effectType().toLowerCase(Locale.ROOT);
        if (!effectType.contains("money_discount")) {
            return 0;
        }
        return Math.max(0, parseIntParam(bonus.effectParams(), "amount", 0));
    }

    private WorkerPlan planWorkersForDefinition(GameState state, SetupSpecModel.EnterpriseDefinition definition) {
        List<SlotTemplate> requiredSlots = buildSlotTemplates(definition);
        if (requiredSlots.isEmpty()) {
            return new WorkerPlan(true, 0, List.of());
        }

        List<Worker> availableWorkers = state.getWorkers().stream()
                .filter(worker -> worker.getLocation() == WorkerLocation.UNEMPLOYED)
                .filter(worker -> !worker.isTiedContract())
                .sorted(Comparator
                        .comparing((Worker worker) -> worker.getClassType() == ClassType.WORKER ? 0 : 1)
                        .thenComparing(worker -> worker.getQualificationType() == WorkerQualification.SKILLED ? 0 : 1)
                        .thenComparing(Worker::getId))
                .collect(Collectors.toCollection(ArrayList::new));

        List<String> selectedWorkerIds = new ArrayList<>();
        Set<String> reserved = new LinkedHashSet<>();
        for (SlotTemplate template : requiredSlots) {
            Worker selected = availableWorkers.stream()
                    .filter(worker -> !reserved.contains(worker.getId()))
                    .filter(worker -> slotMatchesTemplate(template, worker))
                    .findFirst()
                    .orElse(null);
            if (selected == null) {
                return new WorkerPlan(false, requiredSlots.size(), selectedWorkerIds);
            }
            reserved.add(selected.getId());
            selectedWorkerIds.add(selected.getId());
        }
        return new WorkerPlan(true, requiredSlots.size(), selectedWorkerIds);
    }

    private List<SlotTemplate> buildSlotTemplates(SetupSpecModel.EnterpriseDefinition definition) {
        if (definition == null || definition.getWorkers() == null || definition.getWorkers().getSlots() == null) {
            return List.of();
        }
        List<SlotTemplate> templates = new ArrayList<>();
        for (SetupSpecModel.WorkerSlotDefinition slotDefinition : definition.getWorkers().getSlots()) {
            int count = Math.max(0, slotDefinition.getCount());
            for (int i = 0; i < count; i++) {
                WorkerSector sector = slotDefinition.getColor() == null ? null : slotDefinition.getColor().toWorkerSector();
                templates.add(new SlotTemplate(
                        slotDefinition.getQualification() == null ? WorkerQualification.UNSKILLED : slotDefinition.getQualification(),
                        sector
                ));
            }
        }
        return templates;
    }

    private boolean slotMatchesTemplate(SlotTemplate template, Worker worker) {
        if (template.requiredQualification() == WorkerQualification.UNSKILLED) {
            return true;
        }
        if (worker.getQualificationType() != WorkerQualification.SKILLED) {
            return false;
        }
        if (template.requiredSector() == null) {
            return true;
        }
        return template.requiredSector() == worker.getSector();
    }

    private BuildResolution resolveBuildEnterpriseTarget(GameState state, List<BuildCandidatePlan> viable) {
        List<BuildCandidatePlan> pool = new ArrayList<>(viable);
        List<String> reasonPath = new ArrayList<>();
        if (pool.size() == 1) {
            reasonPath.add("single_viable_target");
            return new BuildResolution(pool.getFirst(), reasonPath);
        }

        Set<String> functioningProducedResources = state.getEnterprises().stream()
                .filter(enterprise -> enterprise.getOwnerClass() == ClassType.CAPITALIST)
                .filter(Enterprise::isFunctioning)
                .flatMap(enterprise -> enterprise.getProducedResources().keySet().stream())
                .collect(Collectors.toSet());
        List<BuildCandidatePlan> missingResource = pool.stream()
                .filter(candidate -> candidate.producedResource() != null && !candidate.producedResource().isBlank())
                .filter(candidate -> !functioningProducedResources.contains(candidate.producedResource()))
                .toList();
        if (!missingResource.isEmpty() && missingResource.size() < pool.size()) {
            pool = new ArrayList<>(missingResource);
            reasonPath.add("produces_missing_resource");
        }

        if (pool.size() > 1) {
            int minCost = pool.stream().mapToInt(BuildCandidatePlan::netCost).min().orElse(Integer.MAX_VALUE);
            List<BuildCandidatePlan> cheaper = pool.stream()
                    .filter(candidate -> candidate.netCost() == minCost)
                    .toList();
            if (!cheaper.isEmpty() && cheaper.size() < pool.size()) {
                pool = new ArrayList<>(cheaper);
                reasonPath.add("cheaper_viable_option");
            }
        }

        if (pool.size() > 1) {
            int minSlots = pool.stream().mapToInt(BuildCandidatePlan::requiredSlots).min().orElse(Integer.MAX_VALUE);
            List<BuildCandidatePlan> lowerUnionRisk = pool.stream()
                    .filter(candidate -> candidate.requiredSlots() == minSlots)
                    .toList();
            if (!lowerUnionRisk.isEmpty() && lowerUnionRisk.size() < pool.size()) {
                pool = new ArrayList<>(lowerUnionRisk);
                reasonPath.add("avoids_union_trigger");
            }
        }

        pool.sort(Comparator.comparing(candidate -> candidate.enterpriseId().toLowerCase(Locale.ROOT)));
        if (reasonPath.isEmpty()) {
            reasonPath.add("deterministic_id_tiebreaker");
        }
        return new BuildResolution(pool.getFirst(), reasonPath);
    }

    private List<Worker> occupiedWorkersForEnterprise(GameState state, Enterprise enterprise) {
        if (enterprise == null || enterprise.getSlots() == null) {
            return List.of();
        }
        return enterprise.getSlots().stream()
                .map(EnterpriseSlot::getOccupiedWorkerId)
                .filter(Objects::nonNull)
                .map(workerId -> state.findWorker(workerId).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    private int matchingUnemployedCount(GameState state, Enterprise enterprise) {
        List<Worker> available = state.getWorkers().stream()
                .filter(worker -> worker.getLocation() == WorkerLocation.UNEMPLOYED)
                .filter(worker -> !worker.isTiedContract())
                .toList();
        int count = 0;
        for (EnterpriseSlot slot : enterprise.getSlots()) {
            if (slot.isOccupied()) {
                continue;
            }
            boolean matched = available.stream().anyMatch(worker -> slotMatchesWorker(slot, worker));
            if (matched) {
                count++;
            }
        }
        return count;
    }

    private SellResolution resolveSellEnterpriseTarget(List<SellCandidatePlan> legal, List<String> selectors) {
        List<SellCandidatePlan> pool = new ArrayList<>(legal);
        List<String> reasonPath = new ArrayList<>();
        List<String> normalizedSelectors = selectors.isEmpty()
                ? List.of("highest_sell_value", "fewest_matching_unemployed_available", "random")
                : selectors;

        for (String selector : normalizedSelectors) {
            if (pool.size() <= 1) {
                break;
            }
            switch (selector) {
                case "highest_sell_value" -> {
                    int maxValue = pool.stream().mapToInt(SellCandidatePlan::sellValue).max().orElse(Integer.MIN_VALUE);
                    List<SellCandidatePlan> filtered = pool.stream().filter(candidate -> candidate.sellValue() == maxValue).toList();
                    if (!filtered.isEmpty() && filtered.size() < pool.size()) {
                        pool = new ArrayList<>(filtered);
                        reasonPath.add("highest_sell_value");
                    }
                }
                case "would_enable_union_if_functioning_with_worker_class" -> {
                    List<SellCandidatePlan> filtered = pool.stream().filter(SellCandidatePlan::wouldEnableUnion).toList();
                    if (!filtered.isEmpty() && filtered.size() < pool.size()) {
                        pool = new ArrayList<>(filtered);
                        reasonPath.add("would_enable_union_if_functioning_with_worker_class");
                    }
                }
                case "fewest_matching_unemployed_available" -> {
                    int minAvailable = pool.stream().mapToInt(SellCandidatePlan::matchingUnemployed).min().orElse(Integer.MAX_VALUE);
                    List<SellCandidatePlan> filtered = pool.stream().filter(candidate -> candidate.matchingUnemployed() == minAvailable).toList();
                    if (!filtered.isEmpty() && filtered.size() < pool.size()) {
                        pool = new ArrayList<>(filtered);
                        reasonPath.add("fewest_matching_unemployed_available");
                    }
                }
                default -> {
                    // Keep deterministic order, random selector is resolved by id fallback below.
                }
            }
        }

        pool.sort(Comparator.comparing(candidate -> candidate.enterpriseId().toLowerCase(Locale.ROOT)));
        if (reasonPath.isEmpty()) {
            reasonPath.add("deterministic_id_tiebreaker");
        }
        return new SellResolution(pool.getFirst(), reasonPath);
    }

    private ExportResolution resolveExportOperations(
            GameState state,
            PlayerState actor,
            AutomaCard card,
            boolean withBonus,
            boolean executionPhase
    ) {
        ExportCardState active = state.getActiveExportCard();
        if (active == null || active.getOffers() == null || active.getOffers().isEmpty()) {
            return new ExportResolution(List.of(), 0, 0, 0, List.of("no_export_offers"));
        }

        int operationsLimit = Math.max(0, active.getAvailableOperations());
        if (operationsLimit <= 0) {
            return new ExportResolution(List.of(), 0, 0, 0, List.of("no_available_operations"));
        }

        Map<String, Integer> pool = resourcePoolFor(actor);
        List<ExportOperation> ranked = active.getOffers().stream()
                .filter(offer -> offer != null && offer.getQuantity() > 0 && offer.getRevenue() > 0)
                .map(offer -> new ExportOperation(offer.getResourceId(), offer.getQuantity(), offer.getRevenue(), exportOperationKey(offer)))
                .sorted(Comparator
                        .comparingInt(ExportOperation::revenue).reversed()
                        .thenComparingInt(ExportOperation::quantity)
                        .thenComparing(operation -> operation.resourceId().toLowerCase(Locale.ROOT)))
                .toList();

        List<ExportOperation> selected = new ArrayList<>();
        Set<String> usedOperationKeys = new LinkedHashSet<>();
        int remainingOperations = operationsLimit;
        for (ExportOperation operation : ranked) {
            if (remainingOperations <= 0) {
                break;
            }
            if (!usedOperationKeys.add(operation.operationKey())) {
                continue;
            }
            int available = pool.getOrDefault(normalizeResourceKey(operation.resourceId()), 0);
            if (available < operation.quantity()) {
                continue;
            }
            pool.put(normalizeResourceKey(operation.resourceId()), available - operation.quantity());
            selected.add(operation);
            remainingOperations--;
        }

        int baseRevenue = selected.stream().mapToInt(ExportOperation::revenue).sum();
        int bonusPerOperation = withBonus ? resolveExportBonusPerOperation(card.firstActionBonus()) : 0;
        int bonusRevenue = bonusPerOperation * selected.size();
        List<String> reasonPath = new ArrayList<>();
        reasonPath.add("operations_sorted_by_revenue");
        if (selected.size() < operationsLimit) {
            reasonPath.add("limited_by_resources_or_unique_operations");
        } else {
            reasonPath.add("limited_by_available_operations");
        }
        if (bonusRevenue > 0) {
            reasonPath.add("first_action_export_bonus");
        }
        return new ExportResolution(selected, baseRevenue, bonusRevenue, baseRevenue + bonusRevenue, reasonPath);
    }

    private int resolveExportBonusPerOperation(CapitalistAutomaBonus bonus) {
        if (bonus == null || bonus.effectType() == null) {
            return 0;
        }
        String effectType = bonus.effectType().toLowerCase(Locale.ROOT);
        if (!effectType.contains("money_per_export_operation")) {
            return 0;
        }
        return Math.max(0, parseIntParam(bonus.effectParams(), "amount", 0));
    }

    private String exportOperationKey(ExportCardOffer offer) {
        return normalizeResourceKey(offer.getResourceId()) + "|" + offer.getQuantity() + "|" + offer.getRevenue();
    }

    private Map<String, Object> exportOperationToTrace(ExportOperation operation) {
        return Map.of(
                "resourceId", normalizeResourceKey(operation.resourceId()),
                "quantity", operation.quantity(),
                "revenue", operation.revenue()
        );
    }

    private List<BusinessDealCard> currentVisibleDeals(GameState state) {
        OrderedCardDeckState deck = state.getBusinessDealDeck();
        if (deck == null || deck.getVisibleCardIds() == null || deck.getVisibleCardIds().isEmpty()) {
            return List.of();
        }
        return deck.getVisibleCardIds().stream()
                .map(id -> state.getBusinessDealCards().stream().filter(card -> Objects.equals(card.getId(), id)).findFirst().orElse(null))
                .filter(Objects::nonNull)
                .map(BusinessDealCard::copy)
                .toList();
    }

    private DealCandidatePlan assessDealCandidate(PlayerState actor, BusinessDealCard card) {
        int baseCost = Math.max(0, card.getThresholdAmount());
        int ftzCapacity = FREE_TRADE_ZONE_CAPACITY_PER_DEAL;

        Map<String, Integer> ftzStorage = new LinkedHashMap<>();
        Map<String, Integer> regularStorage = new LinkedHashMap<>();
        int dutyCost = 0;
        int totalGoods = 0;

        for (BusinessDealRequirement requirement : card.getRequirements()) {
            if (requirement == null || requirement.getAmount() <= 0 || requirement.getResourceId() == null) {
                continue;
            }
            String resource = normalizeResourceKey(requirement.getResourceId());
            int amount = Math.max(0, requirement.getAmount());
            totalGoods += amount;

            int toFtz = Math.min(ftzCapacity, amount);
            int toRegular = amount - toFtz;
            ftzCapacity -= toFtz;
            if (toFtz > 0) {
                ftzStorage.merge(resource, toFtz, Integer::sum);
            }
            if (toRegular > 0) {
                int current = actor.getProducedResourceAmount(resource);
                int capacity = REGULAR_STORAGE_DEFAULT_CAPACITY_PER_RESOURCE;
                if (current + toRegular > capacity) {
                    return new DealCandidatePlan(
                            card.getId(),
                            card.getTitle(),
                            false,
                            baseCost,
                            0,
                            baseCost,
                            Map.of(),
                            Map.of(),
                            0,
                            List.of("STORAGE_LIMIT_EXCEEDED")
                    );
                }
                regularStorage.merge(resource, toRegular, Integer::sum);
                dutyCost += toRegular * STORAGE_DUTY_PER_GOOD;
            }
        }

        int totalCost = baseCost + dutyCost;
        if (actor.getMoney() < totalCost) {
            return new DealCandidatePlan(
                    card.getId(),
                    card.getTitle(),
                    false,
                    baseCost,
                    dutyCost,
                    totalCost,
                    ftzStorage,
                    regularStorage,
                    0,
                    List.of("INSUFFICIENT_FUNDS_FOR_DEAL")
            );
        }

        int goodsValue = card.getRequirements().stream()
                .filter(Objects::nonNull)
                .mapToInt(requirement -> {
                    String resource = normalizeResourceKey(requirement.getResourceId());
                    int price = Math.max(1, actor.getPrice(resource));
                    return Math.max(0, requirement.getAmount()) * price;
                })
                .sum();

        return new DealCandidatePlan(
                card.getId(),
                card.getTitle(),
                true,
                baseCost,
                dutyCost,
                totalCost,
                ftzStorage,
                regularStorage,
                goodsValue - totalCost,
                List.of()
        );
    }

    private DealResolution resolveDealChoice(List<DealCandidatePlan> viable, List<String> selectors) {
        List<DealCandidatePlan> pool = new ArrayList<>(viable);
        List<String> reasonPath = new ArrayList<>();
        List<String> normalizedSelectors = selectors.isEmpty()
                ? List.of("highest_net_value", "lowest_total_cost", "highest_goods_volume", "random")
                : selectors;

        for (String selector : normalizedSelectors) {
            if (pool.size() <= 1) {
                break;
            }
            switch (selector) {
                case "highest_net_value", "highest_sell_value" -> {
                    int maxValue = pool.stream().mapToInt(DealCandidatePlan::netValueScore).max().orElse(Integer.MIN_VALUE);
                    List<DealCandidatePlan> filtered = pool.stream().filter(candidate -> candidate.netValueScore() == maxValue).toList();
                    if (!filtered.isEmpty() && filtered.size() < pool.size()) {
                        pool = new ArrayList<>(filtered);
                        reasonPath.add("highest_net_value");
                    }
                }
                case "lowest_total_cost", "fewest_matching_unemployed_available" -> {
                    int minCost = pool.stream().mapToInt(DealCandidatePlan::totalCost).min().orElse(Integer.MAX_VALUE);
                    List<DealCandidatePlan> filtered = pool.stream().filter(candidate -> candidate.totalCost() == minCost).toList();
                    if (!filtered.isEmpty() && filtered.size() < pool.size()) {
                        pool = new ArrayList<>(filtered);
                        reasonPath.add("lowest_total_cost");
                    }
                }
                case "highest_goods_volume" -> {
                    int maxGoods = pool.stream()
                            .mapToInt(candidate -> candidate.storedInFreeTradeZone().values().stream().mapToInt(Integer::intValue).sum()
                                    + candidate.storedInRegularStorage().values().stream().mapToInt(Integer::intValue).sum())
                            .max()
                            .orElse(0);
                    List<DealCandidatePlan> filtered = pool.stream()
                            .filter(candidate -> candidate.storedInFreeTradeZone().values().stream().mapToInt(Integer::intValue).sum()
                                    + candidate.storedInRegularStorage().values().stream().mapToInt(Integer::intValue).sum() == maxGoods)
                            .toList();
                    if (!filtered.isEmpty() && filtered.size() < pool.size()) {
                        pool = new ArrayList<>(filtered);
                        reasonPath.add("highest_goods_volume");
                    }
                }
                default -> {
                    // Keep deterministic order.
                }
            }
        }

        pool.sort(Comparator.comparing(DealCandidatePlan::dealId));
        if (reasonPath.isEmpty()) {
            reasonPath.add("deterministic_id_tiebreaker");
        }
        return new DealResolution(pool.getFirst(), reasonPath);
    }

    private void consumeBusinessDealCardFromMarket(GameState state, String dealId) {
        OrderedCardDeckState deck = state.getBusinessDealDeck();
        if (deck == null) {
            return;
        }

        List<String> order = new ArrayList<>(deck.getOrderedCardIds() == null ? List.of() : deck.getOrderedCardIds());
        List<String> visible = new ArrayList<>(deck.getVisibleCardIds() == null ? List.of() : deck.getVisibleCardIds());
        order.removeIf(id -> Objects.equals(id, dealId));
        visible.removeIf(id -> Objects.equals(id, dealId));
        deck.setOrderedCardIds(order);

        if (order.isEmpty()) {
            deck.setVisibleCardIds(List.of());
            deck.setNextCardIndex(0);
            state.setBusinessDealDeck(deck);
            return;
        }

        int window = Math.max(0, Math.min(deck.getVisibleWindowSize(), order.size()));
        int cursor = Math.floorMod(deck.getNextCardIndex(), order.size());
        int guard = 0;
        while (visible.size() < window && guard++ < order.size() * 2) {
            String candidate = order.get(cursor);
            cursor = (cursor + 1) % order.size();
            if (!visible.contains(candidate)) {
                visible.add(candidate);
            }
        }
        deck.setVisibleCardIds(visible);
        deck.setNextCardIndex(cursor);
        state.setBusinessDealDeck(deck);
    }

    private SnapVoteDecision evaluateSnapVoteDecision(GameState state, String actorPlayerId, PolicyId policyId) {
        PlayerState actor = state.findPlayerById(actorPlayerId).orElse(null);
        if (actor == null || policyId == null) {
            return SnapVoteDecision.notApplicable();
        }

        List<String> reasonCodes = new ArrayList<>();
        boolean canTrigger = true;
        if (actor.getInfluence() < 1) {
            canTrigger = false;
            reasonCodes.add("INSUFFICIENT_INFLUENCE_FOR_SNAP_VOTE");
        }

        PolicyState policy = state.findPolicy(policyId).orElse(null);
        ProposalToken token = policy == null ? null : policy.getOccupyingProposalToken();
        if (token == null || !Objects.equals(actorPlayerId, token.getOwnerPlayerId())) {
            canTrigger = false;
            reasonCodes.add("NO_OWN_PENDING_PROPOSAL_FOR_SNAP_VOTE");
        }

        int supportersInfluence = Math.max(0, actor.getInfluence() - 1);
        int opponentsInfluence = state.getPlayers().stream()
                .filter(player -> !Objects.equals(player.getPlayerId(), actorPlayerId))
                .mapToInt(PlayerState::getInfluence)
                .sum();
        if (supportersInfluence <= opponentsInfluence) {
            canTrigger = false;
            reasonCodes.add("INSUFFICIENT_INFLUENCE_ADVANTAGE");
        }

        if (state.getTurnOrder().getActiveClasses().size() == 2) {
            int actorCubes = votingBagCount(state, cubeOwnerForClass(actor.getClassType()));
            int opponentCubes = state.getPlayers().stream()
                    .filter(player -> !Objects.equals(player.getPlayerId(), actorPlayerId))
                    .map(player -> cubeOwnerForClass(player.getClassType()))
                    .mapToInt(owner -> votingBagCount(state, owner))
                    .sum();
            if (actorCubes < opponentCubes) {
                canTrigger = false;
                reasonCodes.add("INSUFFICIENT_BAG_CUBE_ADVANTAGE");
            }
        }

        if (state.getCurrentRound() >= 5 && policyId == PolicyId.POLICY_7_IMMIGRATION) {
            canTrigger = false;
            reasonCodes.add("LAST_ROUND_MIGRATION_BILL_NOT_ELIGIBLE");
        }

        if (canTrigger) {
            reasonCodes.add("INFLUENCE_ADVANTAGE_CONFIRMED");
        }

        return new SnapVoteDecision(true, canTrigger, reasonCodes, 1);
    }

    private String tryStartSnapVote(
            GameState state,
            String actorPlayerId,
            PolicyId policyId,
            SnapVoteDecision decision
    ) {
        if (!decision.triggered()) {
            return "";
        }
        if (state.getCurrentPhase() != RoundPhase.ACTIONS) {
            return "Snap vote decision was positive, but game already moved out of ACTIONS phase.";
        }

        PlayerState actor = state.findPlayerById(actorPlayerId).orElse(null);
        PolicyState policy = state.findPolicy(policyId).orElse(null);
        if (actor == null || policy == null || policy.getOccupyingProposalToken() == null) {
            return "Snap vote skipped: missing pending policy or actor.";
        }
        if (actor.getInfluence() < decision.influenceCost()) {
            return "Snap vote skipped: influence changed and is no longer sufficient.";
        }

        actor.setInfluence(actor.getInfluence() - decision.influenceCost());
        CurrentVoteState session = new CurrentVoteState();
        session.setActiveProposalPolicyId(policy.getId());
        session.setProposalAuthorPlayerId(actorPlayerId);
        session.setCurrentCourseBeforeVote(policy.getCurrentCourse());
        session.setTargetCourse(policy.getOccupyingProposalToken().getTargetCourse());
        session.setVotingStage(VotingStage.DECLARE_STANCES);
        session.setResult(VoteResolutionResult.PENDING);
        session.setExtraordinary(true);
        session.getStanceByPlayer().put(actorPlayerId, VoteStance.FOR);
        state.setCurrentVoteState(session);
        state.appendLog("AUTOMA_SNAP_VOTE", actorPlayerId + " initiated snap vote on " + policyId + ".");
        return "Automa initiated snap vote on " + policyId + " by spending 1 influence.";
    }

    private VotingCubeOwnerClass cubeOwnerForClass(ClassType classType) {
        if (classType == null) {
            return null;
        }
        return switch (classType) {
            case WORKER -> VotingCubeOwnerClass.WORKER;
            case MIDDLE_CLASS -> VotingCubeOwnerClass.MIDDLE_CLASS;
            case CAPITALIST -> VotingCubeOwnerClass.CAPITALIST;
            case STATE -> null;
        };
    }

    private int votingBagCount(GameState state, VotingCubeOwnerClass ownerClass) {
        if (ownerClass == null || state == null || state.getVotingBag() == null) {
            return 0;
        }
        return switch (ownerClass) {
            case WORKER -> state.getVotingBag().getWorker();
            case MIDDLE_CLASS -> state.getVotingBag().getMiddleClass();
            case CAPITALIST -> state.getVotingBag().getCapitalist();
        };
    }

    private List<String> resolveInstructionPrioritySelectors(CapitalistAutomaActionSymbol symbol) {
        if (symbol == null) {
            return List.of();
        }
        for (CapitalistAutomaInstructionCard instructionCard : cardRegistry.instructionCards()) {
            if (!instructionCard.appliesTo(symbol, CapitalistAutomaExecutionMode.SIMPLE)) {
                continue;
            }
            for (Map<String, Object> rule : instructionCard.structuredRules()) {
                Object kindRaw = rule.get("kind");
                String kind = kindRaw == null ? "" : String.valueOf(kindRaw).toLowerCase(Locale.ROOT);
                if (!kind.contains("target_selection")) {
                    continue;
                }

                Object appliesToRaw = rule.get("applies_to");
                if (appliesToRaw != null) {
                    String appliesTo = String.valueOf(appliesToRaw).toUpperCase(Locale.ROOT);
                    if (!Objects.equals(appliesTo, symbol.name())) {
                        continue;
                    }
                }

                Object prioritiesRaw = rule.get("choose_one_by_priority");
                if (prioritiesRaw instanceof List<?> priorities) {
                    return priorities.stream()
                            .map(String::valueOf)
                            .map(selector -> selector.toLowerCase(Locale.ROOT))
                            .toList();
                }
            }
        }
        return List.of();
    }

    private Enterprise instantiateEnterprise(SetupSpecModel.EnterpriseDefinition definition) {
        Enterprise enterprise = new Enterprise();
        enterprise.setId(definition.getId());
        enterprise.setName(definition.getName() == null || definition.getName().isBlank() ? definition.getId() : definition.getName());
        enterprise.setCategory(definition.getCategory() == null ? "general" : definition.getCategory());
        enterprise.setCost(Math.max(0, definition.getCost()));
        enterprise.setOwnerClass(ClassType.CAPITALIST);
        enterprise.setSector(mapSectorFromCategory(definition.getCategory()));
        enterprise.setWageLevel(2);
        enterprise.setAutomated(definition.isAutomated());

        SetupSpecModel.ProductionDefinition production = definition.getProduction();
        if (production != null && production.getOutput() != null && !production.getOutput().isBlank()) {
            String output = normalizeResourceKey(production.getOutput());
            int amount = Math.max(0, production.getAmount());
            enterprise.setProducedResources(Map.of(output, amount));
            enterprise.setProductionAmount(amount);
            enterprise.setProductionPerWorkers(production.getPerWorkers());
        } else {
            enterprise.setProducedResources(Map.of());
            enterprise.setProductionAmount(0);
            enterprise.setProductionPerWorkers(null);
        }

        SetupSpecModel.WageTrack wageTrack = definition.getWages();
        if (wageTrack != null) {
            Map<String, Integer> wages = new HashMap<>();
            if (wageTrack.getLow() != null) {
                wages.put("low", Math.max(0, wageTrack.getLow()));
            }
            if (wageTrack.getMedium() != null) {
                wages.put("medium", Math.max(0, wageTrack.getMedium()));
            }
            if (wageTrack.getHigh() != null) {
                wages.put("high", Math.max(0, wageTrack.getHigh()));
            }
            enterprise.setWageTrack(wages);
        } else {
            enterprise.setWageTrack(Map.of());
        }
        enterprise.setSlots(buildSlots(definition));
        return enterprise;
    }

    private List<EnterpriseSlot> buildSlots(SetupSpecModel.EnterpriseDefinition definition) {
        if (definition == null || definition.getWorkers() == null || definition.getWorkers().getSlots() == null) {
            return List.of();
        }
        List<EnterpriseSlot> slots = new ArrayList<>();
        int index = 1;
        for (SetupSpecModel.WorkerSlotDefinition slotDefinition : definition.getWorkers().getSlots()) {
            int count = Math.max(0, slotDefinition.getCount());
            for (int i = 0; i < count; i++) {
                WorkerSlotColor color = slotDefinition.getColor();
                WorkerSector sector = color == null ? null : color.toWorkerSector();
                slots.add(new EnterpriseSlot(
                        definition.getId() + "-slot-" + index++,
                        slotDefinition.getQualification() == null ? WorkerQualification.UNSKILLED : slotDefinition.getQualification(),
                        color,
                        sector,
                        null
                ));
            }
        }
        return slots;
    }

    private WorkerSector mapSectorFromCategory(String category) {
        if (category == null || category.isBlank()) {
            return WorkerSector.GENERAL;
        }
        return switch (category.toLowerCase(Locale.ROOT)) {
            case "food" -> WorkerSector.GREEN;
            case "luxury" -> WorkerSector.BLUE;
            case "healthcare" -> WorkerSector.RED;
            case "education" -> WorkerSector.ORANGE;
            case "media", "influence" -> WorkerSector.PURPLE;
            default -> WorkerSector.GENERAL;
        };
    }

    private boolean slotMatchesWorker(EnterpriseSlot slot, Worker worker) {
        if (slot.getRequiredQualification() == WorkerQualification.UNSKILLED) {
            return true;
        }
        if (worker.getQualificationType() != WorkerQualification.SKILLED) {
            return false;
        }
        if (slot.getRequiredSector() == null) {
            return true;
        }
        return slot.getRequiredSector() == worker.getSector();
    }

    private String workerColorLabel(Worker worker) {
        if (worker == null || worker.getSector() == null || worker.getSector() == WorkerSector.GENERAL) {
            return "GRAY";
        }
        return worker.getSector().name();
    }

    private String producedResourceFrom(SetupSpecModel.EnterpriseDefinition definition) {
        if (definition == null || definition.getProduction() == null || definition.getProduction().getOutput() == null) {
            return "";
        }
        return normalizeResourceKey(definition.getProduction().getOutput());
    }

    private Map<String, Integer> resourcePoolFor(PlayerState actor) {
        Map<String, Integer> pool = new HashMap<>();
        for (ResourceType resourceType : ResourceType.values()) {
            String key = resourceType.id();
            int regular = actor.getResourceAmount(key);
            int ftz = actor.getProducedResourceAmount(freeTradeZoneResourceKey(key));
            pool.put(key, regular + ftz);
        }
        return pool;
    }

    private int consumeResourceWithFreeTradeZone(PlayerState actor, String resourceId, int amount) {
        String key = normalizeResourceKey(resourceId);
        int remaining = Math.max(0, amount);
        if (remaining <= 0) {
            return 0;
        }

        int ftzConsumed = actor.consumeProducedResource(freeTradeZoneResourceKey(key), remaining);
        remaining -= ftzConsumed;
        int regularConsumed = remaining <= 0 ? 0 : actor.consumeResource(key, remaining);
        return ftzConsumed + regularConsumed;
    }

    private String freeTradeZoneResourceKey(String resourceId) {
        return "ftz_" + normalizeResourceKey(resourceId);
    }

    private String normalizeResourceKey(String resourceId) {
        ResourceType type = ResourceType.fromRaw(resourceId);
        return type == null ? String.valueOf(resourceId).toLowerCase(Locale.ROOT) : type.id();
    }

    private List<String> asStringList(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(String::valueOf).toList();
    }

    private int asInt(Object raw, int fallback) {
        if (raw == null) {
            return fallback;
        }
        if (raw instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(raw));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String compactResolvedTarget(Map<String, Object> resolvedTarget) {
        if (resolvedTarget == null || resolvedTarget.isEmpty()) {
            return "";
        }
        List<String> keyParts = new ArrayList<>();
        if (resolvedTarget.containsKey("enterpriseId")) {
            keyParts.add("enterprise=" + resolvedTarget.get("enterpriseId"));
        }
        if (resolvedTarget.containsKey("dealId")) {
            keyParts.add("deal=" + resolvedTarget.get("dealId"));
        }
        if (resolvedTarget.containsKey("policyId")) {
            keyParts.add("policy=" + resolvedTarget.get("policyId"));
        }
        if (resolvedTarget.containsKey("exportCardId")) {
            keyParts.add("exportCard=" + resolvedTarget.get("exportCardId"));
        }
        if (keyParts.isEmpty()) {
            keyParts.add(resolvedTarget.toString());
        }
        return String.join(", ", keyParts);
    }

    private AutomaActionEvaluation unsupported(AutomaActionCandidate candidate, String reason) {
        return new AutomaActionEvaluation(candidate, false, List.of(reason), List.of(), null, false, Map.of(), List.of("unsupported"));
    }

    public record ResolvedTurn(GameState state, BotTurnSummary summary, List<DomainEvent> events) {
    }

    public record AutomaActionCandidate(
            int slot,
            AutomaActionType actionType,
            String source,
            boolean bonusConsidered
    ) {
    }

    public record AutomaActionEvaluation(
            AutomaActionCandidate candidate,
            boolean canExecute,
            List<String> reasonCodes,
            List<String> plannedCommands,
            GameCommand primaryCommand,
            boolean bonusApplied,
            Map<String, Object> resolvedTarget,
            List<String> reasonPath
    ) {
    }

    private enum AutomaActionType {
        NONE,
        PROPOSE_BILL,
        ASSIGN_WORKERS,
        BUILD_ENTERPRISE,
        SELL_TO_FOREIGN_MARKET,
        MAKE_DEAL,
        SELL_ENTERPRISE,
        RECONFIGURE_EQUIPMENT,
        REACT_TO_STRIKE,
        SPECIAL_ACTION
    }

    private record AutomaCard(
            String id,
            int cardNo,
            CapitalistAutomaActionSymbol actionSlot1,
            CapitalistAutomaActionSymbol actionSlot2,
            CapitalistAutomaActionSymbol actionSlot3,
            CapitalistAutomaActionSymbol actionSlot4,
            CapitalistAutomaBonus firstActionBonus,
            List<CapitalistAutomaPolicyTag> policyTags,
            CapitalistAutomaSpecialAction specialAction,
            int influenceValue
    ) {
    }

    private record WorkerPlan(
            boolean canFullyStaff,
            int requiredSlots,
            List<String> selectedWorkerIds
    ) {
    }

    private record SlotTemplate(
            WorkerQualification requiredQualification,
            WorkerSector requiredSector
    ) {
    }

    private record BuildCandidatePlan(
            String enterpriseId,
            String enterpriseName,
            int baseCost,
            int netCost,
            boolean automated,
            String producedResource,
            int requiredSlots,
            List<String> assignedWorkerIds
    ) {
    }

    private record BuildResolution(
            BuildCandidatePlan selected,
            List<String> reasonPath
    ) {
    }

    private record SellCandidatePlan(
            String enterpriseId,
            String enterpriseName,
            int sellValue,
            int matchingUnemployed,
            boolean wouldEnableUnion
    ) {
    }

    private record SellResolution(
            SellCandidatePlan selected,
            List<String> reasonPath
    ) {
    }

    private record ExportOperation(
            String resourceId,
            int quantity,
            int revenue,
            String operationKey
    ) {
    }

    private record ExportResolution(
            List<ExportOperation> selectedOperations,
            int baseRevenue,
            int bonusRevenue,
            int totalRevenue,
            List<String> reasonPath
    ) {
    }

    private record DealCandidatePlan(
            String dealId,
            String title,
            boolean canExecute,
            int baseCost,
            int dutyCost,
            int totalCost,
            Map<String, Integer> storedInFreeTradeZone,
            Map<String, Integer> storedInRegularStorage,
            int netValueScore,
            List<String> reasonCodes
    ) {
    }

    private record DealResolution(
            DealCandidatePlan selected,
            List<String> reasonPath
    ) {
    }

    private record ActionMutationResult(
            boolean executed,
            List<String> syntheticSummaries,
            Map<String, Object> resolvedTarget,
            List<String> reasonPath,
            String failureReason
    ) {
        private static ActionMutationResult failed(String reason) {
            return new ActionMutationResult(false, List.of(), Map.of(), List.of(), reason == null ? "" : reason);
        }
    }

    private record SnapVoteDecision(
            boolean applicable,
            boolean triggered,
            List<String> reasonCodes,
            int influenceCost
    ) {
        private static SnapVoteDecision notApplicable() {
            return new SnapVoteDecision(false, false, List.of("NOT_APPLICABLE"), 0);
        }

        private Map<String, Object> toTraceMap() {
            return Map.of(
                    "applicable", applicable,
                    "triggered", triggered,
                    "reasonCodes", reasonCodes,
                    "influenceCost", influenceCost
            );
        }
    }
}
