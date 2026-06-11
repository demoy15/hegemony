package com.example.hegemony.application;

import com.example.hegemony.bot.LegalMoveBot;
import com.example.hegemony.bot.planning.ActionPlan;
import com.example.hegemony.bot.planning.ActionPlanSupportStatus;
import com.example.hegemony.bot.planning.BotCardPlanner;
import com.example.hegemony.bot.planning.ClassBotPlanner;
import com.example.hegemony.bot.planning.MarketCandidate;
import com.example.hegemony.bot.planning.MarketCandidateProvider;
import com.example.hegemony.bot.planning.PlannedBotMove;
import com.example.hegemony.domain.carddata.BotActionCardCatalog;
import com.example.hegemony.domain.carddata.EnterpriseCardCatalog;
import com.example.hegemony.domain.command.AssignWorkersCommand;
import com.example.hegemony.domain.command.BuyGoodsAndServicesCommand;
import com.example.hegemony.domain.command.CallExtraordinaryVoteCommand;
import com.example.hegemony.domain.command.CommitVoteInfluenceCommand;
import com.example.hegemony.domain.command.DeclareVoteStanceCommand;
import com.example.hegemony.domain.command.EndTurnCommand;
import com.example.hegemony.domain.command.GameCommand;
import com.example.hegemony.domain.command.ProposeBillCommand;
import com.example.hegemony.domain.command.RefreshBusinessDealsCommand;
import com.example.hegemony.domain.engine.ApplyCommandResult;
import com.example.hegemony.domain.engine.GameRulesEngine;
import com.example.hegemony.domain.event.DomainEvent;
import com.example.hegemony.domain.model.ActionType;
import com.example.hegemony.domain.model.BotStrategyMode;
import com.example.hegemony.domain.model.BotTurnSummary;
import com.example.hegemony.domain.model.CardReadinessState;
import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.PlayerControlMode;
import com.example.hegemony.domain.model.PlayerState;
import com.example.hegemony.domain.model.RoundPhase;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class BotTurnService {
    private final GameStateRepository repository;
    private final GameRulesEngine engine;
    private final LegalMoveBot legalMoveBot;
    private final List<ClassBotPlanner> classPlanners;
    private final List<BotCardPlanner> botCardPlanners;
    private final EnterpriseCardCatalog enterpriseCardCatalog;
    private final BotActionCardCatalog botActionCardCatalog;
    private final MarketCandidateProvider marketCandidateProvider;
    private final AutomaSimpleModeTurnService simpleModeTurnService;

    public BotTurnService(
            GameStateRepository repository,
            GameRulesEngine engine,
            LegalMoveBot legalMoveBot,
            List<ClassBotPlanner> classPlanners,
            List<BotCardPlanner> botCardPlanners,
            EnterpriseCardCatalog enterpriseCardCatalog,
            BotActionCardCatalog botActionCardCatalog,
            MarketCandidateProvider marketCandidateProvider,
            AutomaSimpleModeTurnService simpleModeTurnService
    ) {
        this.repository = repository;
        this.engine = engine;
        this.legalMoveBot = legalMoveBot;
        this.classPlanners = classPlanners == null ? List.of() : classPlanners;
        this.botCardPlanners = botCardPlanners == null ? List.of() : botCardPlanners;
        this.enterpriseCardCatalog = enterpriseCardCatalog;
        this.botActionCardCatalog = botActionCardCatalog;
        this.marketCandidateProvider = marketCandidateProvider;
        this.simpleModeTurnService = simpleModeTurnService;
    }

    public synchronized BotTurnExecutionResult playBotTurn() {
        GameState current = repository.get();
        refreshCardReadiness(current);

        PlayerState actor = current.currentPlayer();
        if (actor == null) {
            throw new IllegalStateException("Cannot play bot turn: no current player.");
        }
        if (actor.getControlMode() != PlayerControlMode.BOT) {
            throw new IllegalStateException("PLAY_BOT_TURN is allowed only when current actor is BOT-controlled.");
        }

        BotStep step = executeBotStep(current);
        repository.save(step.state());
        return new BotTurnExecutionResult(step.summary(), step.events(), step.state().copy());
    }

    public synchronized BotUntilHumanExecutionResult playBotUntilHuman() {
        GameState current = repository.get();
        refreshCardReadiness(current);

        List<BotTurnSummary> summaries = new ArrayList<>();
        int guard = 0;
        boolean stoppedAtHumanDecisionPoint = false;

        while (guard++ < 250) {
            if (current.isGameOver()) {
                break;
            }
            PlayerState actor = current.currentPlayer();
            if (actor == null) {
                break;
            }

            List<GameCommand> legalCommands = engine.generateLegalCommands(current);
            if (legalCommands.isEmpty()) {
                break;
            }

            if (actor.getControlMode() == PlayerControlMode.BOT) {
                BotStep step = executeBotStep(current);
                summaries.add(step.summary());
                current = step.state();
                continue;
            }

            if (legalCommands.size() == 1 && isNonDecisionLifecycle(legalCommands.getFirst().type())) {
                ApplyCommandResult auto = engine.apply(current, legalCommands.getFirst());
                if (!auto.validation().isValid()) {
                    break;
                }
                current = auto.resultingState();
                refreshCardReadiness(current);
                continue;
            }

            stoppedAtHumanDecisionPoint = true;
            break;
        }

        repository.save(current);
        return new BotUntilHumanExecutionResult(
                summaries,
                summaries.size(),
                stoppedAtHumanDecisionPoint,
                current.isGameOver(),
                current.copy()
        );
    }

    public List<MarketCandidate> currentMarketCandidates() {
        GameState state = repository.get();
        return marketCandidateProvider.listCandidates(state, ClassType.CAPITALIST);
    }

    private BotStep executeBotStep(GameState state) {
        PlayerState actor = state.currentPlayer();
        if (actor != null && actor.getClassType() == ClassType.CAPITALIST
                && actor.getBotStrategyMode() == BotStrategyMode.CARD_DRIVEN_SIMPLE_AUTOMA) {
            Optional<AutomaSimpleModeTurnService.ResolvedTurn> simpleTurn = simpleModeTurnService.resolveAndApply(state, actor);
            if (simpleTurn.isPresent()) {
                refreshCardReadiness(simpleTurn.get().state());
                return new BotStep(simpleTurn.get().state(), simpleTurn.get().summary(), simpleTurn.get().events());
            }
        }

        List<GameCommand> legalCommands = engine.generateLegalCommands(state);
        if (legalCommands.isEmpty()) {
            throw new IllegalStateException("No legal moves available for bot actor.");
        }

        PlannedBotMove planned = choosePlan(state, actor, legalCommands);
        GameCommand selected = planned.actionPlan().getMainAction();
        ApplyCommandResult applyResult = engine.apply(state, selected);
        if (!applyResult.validation().isValid()) {
            throw new IllegalStateException("Bot selected illegal move: " + applyResult.validation().getErrors());
        }

        GameState next = applyResult.resultingState();
        List<DomainEvent> producedEvents = new ArrayList<>(applyResult.producedEvents());

        if (shouldAutoEndActionsTurn(actor, selected, next)) {
            ApplyCommandResult endTurnResult = engine.apply(next, new EndTurnCommand());
            if (!endTurnResult.validation().isValid()) {
                throw new IllegalStateException("Bot failed to end turn after action: " + endTurnResult.validation().getErrors());
            }
            next = endTurnResult.resultingState();
            producedEvents.addAll(endTurnResult.producedEvents());
        }

        refreshCardReadiness(next);

        BotTurnSummary summary = new BotTurnSummary();
        summary.setActingClass(actor.getClassType());
        summary.setActingPlayerId(actor.getPlayerId());
        summary.setSelectedMoveId(selected.moveId());
        summary.setSelectedAction(selected.type());
        summary.setChosenTargets(extractTargets(selected));
        summary.setCardModifierPathUsed(planned.actionPlan().getOptionalCardReference() != null);
        summary.setPlannerId(planned.plannerId());
        summary.setRationale(planned.rationale());
        summary.setLegalOptionsConsidered(planned.legalOptionsConsidered());
        summary.setFallbackHeuristicMode(planned.fallbackHeuristicMode());
        summary.setStrategyModeUsed(actor.getBotStrategyMode());
        summary.setEventSummaries(producedEvents.stream().map(DomainEvent::description).toList());
        summary.setAutomaTrace(planned.debugTrace());
        next.setLastBotTurnSummary(summary);
        next.appendLog(
                "BOT_TURN",
                "[" + actor.getClassType() + "] bot chose " + selected.type()
                        + " from " + planned.legalOptionsConsidered() + " legal options. " + planned.rationale()
        );

        return new BotStep(next, summary, producedEvents);
    }

    private PlannedBotMove choosePlan(GameState state, PlayerState actor, List<GameCommand> legalCommands) {
        BotStrategyMode strategy = actor.getBotStrategyMode();
        if ((strategy == BotStrategyMode.CARD_DRIVEN_SIMPLE_AUTOMA || strategy == BotStrategyMode.CARD_DRIVEN_COMPLEX_AUTOMA)
                && isCardPlannerReady(actor.getClassType())) {
            Optional<PlannedBotMove> cardPlan = botCardPlanners.stream()
                    .filter(planner -> planner.isReady(actor.getClassType()))
                    .map(planner -> planner.plan(state, actor.getClassType(), legalCommands))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst();
            if (cardPlan.isPresent()) {
                return cardPlan.get();
            }
        }

        Optional<ClassBotPlanner> classPlanner = classPlanners.stream()
                .filter(planner -> planner.supportedClass() == actor.getClassType())
                .findFirst();
        if (classPlanner.isPresent()) {
            Optional<PlannedBotMove> planned = classPlanner.get().plan(state, legalCommands);
            if (planned.isPresent()) {
                return planned.get();
            }
        }

        var decision = legalMoveBot.chooseMove(state, engine);
        ActionPlan plan = new ActionPlan(decision.selectedCommand(), ActionPlanSupportStatus.FALLBACK_HEURISTIC,
                "CARD_DATA_NOT_INSTALLED: legal move heuristic fallback was used.");
        String rationale = decision.explanation();
        if ((strategy == BotStrategyMode.CARD_DRIVEN_SIMPLE_AUTOMA || strategy == BotStrategyMode.CARD_DRIVEN_COMPLEX_AUTOMA)
                && !isCardPlannerReady(actor.getClassType())) {
            rationale = "Card-driven automa mode is unavailable for class " + actor.getClassType()
                    + "; fallback heuristic selected legal action. " + rationale;
        }
        return new PlannedBotMove(
                plan,
                "legal-move-fallback",
                rationale,
                true,
                decision.legalActionCount(),
                Map.of(
                        "automa", actor.getClassType().name(),
                        "mode", "FALLBACK_LEGAL_HEURISTIC",
                        "selectedAction", decision.selectedCommand().type().name()
                )
        );
    }

    private Map<String, Object> extractTargets(GameCommand command) {
        return switch (command) {
            case ProposeBillCommand propose -> Map.of(
                    "policyId", propose.policyId().name(),
                    "targetCourse", propose.targetCourse().name()
            );
            case AssignWorkersCommand assign -> Map.of(
                    "assignmentsCount", assign.assignments().size(),
                    "workerIds", assign.assignments().stream().map(op -> op.workerId()).toList()
            );
            case BuyGoodsAndServicesCommand buy -> Map.of(
                    "resourceType", buy.resourceType(),
                    "purchasesCount", buy.purchases().size()
            );
            case DeclareVoteStanceCommand stance -> Map.of(
                    "policyId", stance.policyId().name(),
                    "stance", stance.stance()
            );
            case CommitVoteInfluenceCommand commit -> Map.of("influenceAmount", commit.influenceAmount());
            case CallExtraordinaryVoteCommand vote -> Map.of("policyId", vote.policyId().name());
            case RefreshBusinessDealsCommand refresh -> Map.of("actorPlayerId", refresh.actorPlayerId());
            default -> Map.of("actorPlayerId", actorPlayerIdFrom(command));
        };
    }

    private String actorPlayerIdFrom(GameCommand command) {
        return switch (command) {
            case ProposeBillCommand propose -> propose.actorPlayerId();
            case AssignWorkersCommand assign -> assign.actorPlayerId();
            case BuyGoodsAndServicesCommand buy -> buy.actorPlayerId();
            case DeclareVoteStanceCommand stance -> stance.actorPlayerId();
            case com.example.hegemony.domain.command.DrawVotingCubesCommand draw -> draw.actorPlayerId();
            case CommitVoteInfluenceCommand commit -> commit.actorPlayerId();
            case CallExtraordinaryVoteCommand vote -> vote.actorPlayerId();
            case RefreshBusinessDealsCommand refresh -> refresh.actorPlayerId();
            default -> "";
        };
    }

    private boolean isNonDecisionLifecycle(ActionType actionType) {
        return switch (actionType) {
            case ADVANCE_GAME_FLOW,
                    ADVANCE_TO_VOTING,
                    ADVANCE_TO_PRODUCTION,
                    RESOLVE_PRODUCTION_PHASE,
                    ADVANCE_TO_SCORING,
                    RESOLVE_SCORING_PHASE,
                    ADVANCE_TO_NEXT_ROUND,
                    RESOLVE_PREPARATION_PHASE,
                    ADVANCE_ROUND -> true;
            default -> false;
        };
    }

    private boolean isCardPlannerReady(ClassType classType) {
        return botCardPlanners.stream().anyMatch(planner -> planner.isReady(classType));
    }

    private boolean shouldAutoEndActionsTurn(PlayerState actor, GameCommand selected, GameState next) {
        if (actor == null || next == null || selected.type() == ActionType.END_TURN || next.isGameOver()) {
            return false;
        }
        if (next.getTurnOrder().getPhase() != RoundPhase.ACTIONS) {
            return false;
        }
        PlayerState current = next.currentPlayer();
        if (current == null || !Objects.equals(current.getPlayerId(), actor.getPlayerId())) {
            return false;
        }
        if (current.getControlMode() != PlayerControlMode.BOT) {
            return false;
        }
        return engine.generateLegalCommands(next).stream().anyMatch(command -> command.type() == ActionType.END_TURN);
    }

    private void refreshCardReadiness(GameState state) {
        CardReadinessState readiness = state.getCardReadiness();
        readiness.setEnterpriseCardDatasetInstalled(enterpriseCardCatalog.isDatasetInstalled());
        readiness.setActionModifierDatasetInstalled(false);

        Map<ClassType, Boolean> simpleAutomaInstalled = new EnumMap<>(ClassType.class);
        for (ClassType classType : ClassType.values()) {
            simpleAutomaInstalled.put(classType, botActionCardCatalog.isSimpleAutomaDatasetInstalled(classType));
        }
        readiness.setSimpleAutomaCardDatasetInstalledByClass(simpleAutomaInstalled);

        List<String> notes = new ArrayList<>();
        notes.add("enterprise_card_catalog=" + enterpriseCardCatalog.datasetStatus());
        for (ClassType classType : ClassType.values()) {
            notes.add("simple_automa_" + classType.name().toLowerCase() + "=" + botActionCardCatalog.datasetStatus(classType));
        }
        readiness.setNotes(notes);
        state.setCardReadiness(readiness);
    }

    private record BotStep(GameState state, BotTurnSummary summary, List<DomainEvent> events) {
    }
}
