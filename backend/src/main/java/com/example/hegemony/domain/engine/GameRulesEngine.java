package com.example.hegemony.domain.engine;

import com.example.hegemony.application.BusinessDealDeckManager;
import com.example.hegemony.application.ExportCardManager;
import com.example.hegemony.application.MigrationCardManager;
import com.example.hegemony.domain.card.CardCatalog;
import com.example.hegemony.domain.card.DeclarativeCardEffectProcessor;
import com.example.hegemony.domain.command.AdjustPolicyCommand;
import com.example.hegemony.domain.command.AdvanceGameFlowCommand;
import com.example.hegemony.domain.command.AdvanceRoundCommand;
import com.example.hegemony.domain.command.AdvanceToNextRoundCommand;
import com.example.hegemony.domain.command.AdvanceToProductionCommand;
import com.example.hegemony.domain.command.AdvanceToScoringCommand;
import com.example.hegemony.domain.command.AdvanceToVotingCommand;
import com.example.hegemony.domain.command.AddVotingCubesCommand;
import com.example.hegemony.domain.command.AssignWorkersCommand;
import com.example.hegemony.domain.command.AssignmentTargetType;
import com.example.hegemony.domain.command.BuyGoodsAndServicesCommand;
import com.example.hegemony.domain.command.CallExtraordinaryVoteCommand;
import com.example.hegemony.domain.command.CommitVoteInfluenceCommand;
import com.example.hegemony.domain.command.ConsumeEducationCommand;
import com.example.hegemony.domain.command.ConsumeHealthcareCommand;
import com.example.hegemony.domain.command.ConsumeLuxuryCommand;
import com.example.hegemony.domain.command.DeclareVoteStanceCommand;
import com.example.hegemony.domain.command.EndTurnCommand;
import com.example.hegemony.domain.command.GameCommand;
import com.example.hegemony.domain.command.HireWorkerCommand;
import com.example.hegemony.domain.command.PlayCardCommand;
import com.example.hegemony.domain.command.PlaceDemonstrationCommand;
import com.example.hegemony.domain.command.PlaceStrikesCommand;
import com.example.hegemony.domain.command.ProposeBillCommand;
import com.example.hegemony.domain.command.ProduceGoodsCommand;
import com.example.hegemony.domain.command.RefreshBusinessDealsCommand;
import com.example.hegemony.domain.command.ResolvePreparationPhaseCommand;
import com.example.hegemony.domain.command.ResolveProductionPhaseCommand;
import com.example.hegemony.domain.command.ResolveScoringPhaseCommand;
import com.example.hegemony.domain.command.SellGoodsCommand;
import com.example.hegemony.domain.command.StartTurnCommand;
import com.example.hegemony.domain.command.PurchaseItem;
import com.example.hegemony.domain.command.WorkerAssignmentOperation;
import com.example.hegemony.domain.event.BillProposedEvent;
import com.example.hegemony.domain.event.BusinessDealsRefreshedEvent;
import com.example.hegemony.domain.event.CardPlayedEvent;
import com.example.hegemony.domain.event.DomainEvent;
import com.example.hegemony.domain.event.ExportCardRefreshedEvent;
import com.example.hegemony.domain.event.GoodsProducedEvent;
import com.example.hegemony.domain.event.GoodsSoldEvent;
import com.example.hegemony.domain.event.MigrationResolvedEvent;
import com.example.hegemony.domain.event.PolicyAdjustedEvent;
import com.example.hegemony.domain.event.TurnEndedEvent;
import com.example.hegemony.domain.event.TurnStartedEvent;
import com.example.hegemony.domain.event.VoteInfluenceCommittedEvent;
import com.example.hegemony.domain.event.VoteStanceDeclaredEvent;
import com.example.hegemony.domain.event.VotingCubesDrawnEvent;
import com.example.hegemony.domain.event.VotingPhaseStartedEvent;
import com.example.hegemony.domain.event.WorkerAssignedEvent;
import com.example.hegemony.domain.event.WorkerHiredEvent;
import com.example.hegemony.domain.economy.ConsumerEconomyService;
import com.example.hegemony.domain.economy.ConsumerEconomyService.PurchaseEvaluation;
import com.example.hegemony.domain.economy.ConsumerEconomyService.SupplierOffer;
import com.example.hegemony.domain.model.ActionType;
import com.example.hegemony.domain.model.BotStrategyMode;
import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.CurrentVoteState;
import com.example.hegemony.domain.model.DrawnVotingCube;
import com.example.hegemony.domain.model.Enterprise;
import com.example.hegemony.domain.model.EnterpriseSlot;
import com.example.hegemony.domain.model.EventLogEntry;
import com.example.hegemony.domain.model.FinalResult;
import com.example.hegemony.domain.model.FinalStanding;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.GameStatus;
import com.example.hegemony.domain.model.InterpretedVote;
import com.example.hegemony.domain.model.MigrationCardEntry;
import com.example.hegemony.domain.model.MigrationCardState;
import com.example.hegemony.domain.model.OrderedCardDeckState;
import com.example.hegemony.domain.model.PlayerState;
import com.example.hegemony.domain.model.PlayerControlMode;
import com.example.hegemony.domain.model.PlayerScoringBreakdown;
import com.example.hegemony.domain.model.PlayerRole;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.PolicyState;
import com.example.hegemony.domain.model.PolicyTrack;
import com.example.hegemony.domain.model.PopulationScale;
import com.example.hegemony.domain.model.PreparationSummary;
import com.example.hegemony.domain.model.ProposalToken;
import com.example.hegemony.domain.model.ProductionPhaseState;
import com.example.hegemony.domain.model.ProductionSubPhase;
import com.example.hegemony.domain.model.RoundPhase;
import com.example.hegemony.domain.model.RoundSummary;
import com.example.hegemony.domain.model.ScoringSourceEntry;
import com.example.hegemony.domain.model.ScoringSummary;
import com.example.hegemony.domain.model.ResourceType;
import com.example.hegemony.domain.model.VoteResolutionResult;
import com.example.hegemony.domain.model.VoteStance;
import com.example.hegemony.domain.model.VotingCubeOwnerClass;
import com.example.hegemony.domain.model.VotingStage;
import com.example.hegemony.domain.model.Worker;
import com.example.hegemony.domain.model.WorkerLocation;
import com.example.hegemony.domain.model.WorkerQualification;
import com.example.hegemony.domain.model.WorkerSector;
import com.example.hegemony.domain.model.WorkerSlotColor;
import com.example.hegemony.domain.production.ProductionResolver;
import com.example.hegemony.domain.rules.ValidationReasonCode;
import com.example.hegemony.domain.rules.ValidationResult;
import com.example.hegemony.domain.voting.VotingBagRules;
import com.example.hegemony.domain.voting.VotingResolutionService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class GameRulesEngine {
    private final CardCatalog cardCatalog;
    private final DeclarativeCardEffectProcessor cardEffectProcessor;
    private final Map<PolicyCourse, Map<PolicyCourse, Integer>> workerTaxMatrix;
    private final BusinessDealDeckManager businessDealDeckManager;
    private final ExportCardManager exportCardManager;
    private final MigrationCardManager migrationCardManager;
    private final VotingBagRules votingBagRules = new VotingBagRules();
    private final VotingResolutionService votingResolutionService = new VotingResolutionService(votingBagRules);
    private final ProductionResolver productionResolver = new ProductionResolver();
    private final ConsumerEconomyService consumerEconomyService = new ConsumerEconomyService();
    private static final int ACTION_VOTING_CUBES_TO_ADD = 3;
    private static final int STATE_LOAN_AMOUNT = 50;
    private static final int STATE_LOAN_INTEREST = 5;
    private static final String WORKER_OPENING_MIGRATION_MARKER = "WORKER_OPENING_MIGRATION";

    public GameRulesEngine(
            CardCatalog cardCatalog,
            DeclarativeCardEffectProcessor cardEffectProcessor,
            BusinessDealDeckManager businessDealDeckManager,
            ExportCardManager exportCardManager,
            MigrationCardManager migrationCardManager
    ) {
        this(cardCatalog, cardEffectProcessor, defaultWorkerTaxMatrix(), businessDealDeckManager, exportCardManager, migrationCardManager);
    }

    public GameRulesEngine(
            CardCatalog cardCatalog,
            DeclarativeCardEffectProcessor cardEffectProcessor,
            Map<PolicyCourse, Map<PolicyCourse, Integer>> workerTaxMatrix,
            BusinessDealDeckManager businessDealDeckManager,
            ExportCardManager exportCardManager,
            MigrationCardManager migrationCardManager
    ) {
        this.cardCatalog = cardCatalog;
        this.cardEffectProcessor = cardEffectProcessor;
        this.workerTaxMatrix = sanitizeTaxMatrix(workerTaxMatrix);
        this.businessDealDeckManager = businessDealDeckManager;
        this.exportCardManager = exportCardManager;
        this.migrationCardManager = migrationCardManager;
    }

    public List<GameCommand> generateLegalCommands(GameState state) {
        if (state.isGameOver()) {
            return List.of();
        }
        return switch (state.getTurnOrder().getPhase()) {
            case PREPARATION -> generatePreparationPhaseCommands(state);
            case ACTIONS -> generateActionPhaseCommands(state);
            case VOTING -> generateVotingPhaseCommands(state);
            case PRODUCTION -> generateProductionPhaseCommands(state);
            case SCORING -> generateScoringPhaseCommands(state);
            case GAME_OVER -> List.of();
            default -> List.of();
        };
    }

    public List<LegalMove> generateLegalMoves(GameState state) {
        List<LegalMove> moves = new ArrayList<>();
        for (GameCommand command : generateLegalCommands(state)) {
            moves.add(new LegalMove(command.moveId(), command.type(), command.summary(), isLegacy(command.type()), Map.of()));
        }

        if (state.isGameOver()) {
            return moves;
        }

        if (state.getTurnOrder().getPhase() == RoundPhase.ACTIONS && state.currentPlayer() != null) {
            PlayerState actor = state.currentPlayer();
            if (actor != null && actor.getControlMode() == PlayerControlMode.HUMAN && votingBagRules.ownerForClass(actor.getClassType()) != null) {
                moves.add(new LegalMove(
                        "add-voting-cubes-template:" + actor.getPlayerId(),
                        ActionType.ADD_VOTING_CUBES,
                        "Add 3 voting cubes of the acting class color to the bag.",
                        false,
                        Map.of("actorPlayerId", actor.getPlayerId(), "amount", ACTION_VOTING_CUBES_TO_ADD)
                ));
            }
            moves.add(new LegalMove(
                    "assign-workers-template",
                    ActionType.ASSIGN_WORKERS,
                    "Assign up to 3 workers using assignment operations.",
                    false,
                    Map.of("maxWorkers", 3, "targetTypes", List.of("ENTERPRISE_SLOT", "UNION"), "targetIdFormat", "<enterpriseId>:<slotId>")
            ));
            if (actor != null && actor.getClassType() == ClassType.WORKER) {
                List<String> strikeTargets = legalStrikeEnterpriseIds(state);
                if (!strikeTargets.isEmpty()) {
                    moves.add(new LegalMove(
                            "place-strikes-template:" + actor.getPlayerId(),
                            ActionType.PLACE_STRIKES,
                            "Place up to 2 strike tokens on eligible enterprises employing worker-class workers.",
                            false,
                            Map.of("actorPlayerId", actor.getPlayerId(), "maxEnterprises", 2, "eligibleEnterpriseIds", strikeTargets)
                    ));
                }
                if (canPlaceDemonstration(state)) {
                    moves.add(new LegalMove(
                            "place-demonstration-template:" + actor.getPlayerId(),
                            ActionType.PLACE_DEMONSTRATION,
                            "Place a demonstration token on the unemployed area.",
                            false,
                            Map.of("actorPlayerId", actor.getPlayerId(), "penaltyPool", demonstrationPenaltyPool(state))
                    ));
                }
            }
            if (allActionTurnsCompleted(state)) {
                moves.add(new LegalMove(
                        "advance-to-production-template",
                        ActionType.ADVANCE_TO_PRODUCTION,
                        "Advance from ACTIONS to PRODUCTION phase.",
                        false,
                        Map.of("requiredActor", state.currentPlayer().getPlayerId())
                ));
            }

            if (actor != null && consumerEconomyService.isSupportedConsumerBuyer(actor.getClassType())) {
                for (ResourceType resourceType : List.of(ResourceType.FOOD, ResourceType.HEALTHCARE, ResourceType.EDUCATION, ResourceType.LUXURY)) {
                    List<SupplierOffer> offers = consumerEconomyService.listSupplierOffersFor(state, actor, resourceType);
                    if (offers.isEmpty()) {
                        continue;
                    }
                    List<Map<String, Object>> supplierTemplates = offers.stream()
                            .map(offer -> Map.<String, Object>of(
                                    "supplierType", offer.supplierType().name(),
                                    "supplierPlayerId", offer.supplierPlayerId() == null ? "" : offer.supplierPlayerId(),
                                    "maxQuantity", Math.max(0, offer.availableQuantity()),
                                    "unitPrice", offer.unitPrice()
                            ))
                            .toList();
                    moves.add(new LegalMove(
                            "buy-goods-template:" + actor.getPlayerId() + ":" + resourceType.name(),
                            ActionType.BUY_GOODS_AND_SERVICES,
                            "Buy " + resourceType.name() + " from supported suppliers.",
                            false,
                            Map.of(
                                    "actorPlayerId", actor.getPlayerId(),
                                    "resourceType", resourceType.name(),
                                    "templateOnly", true,
                                    "maxSuppliers", 2,
                                    "maxPerSupplier", Math.max(0, actor.getPopulation()),
                                    "eligibleSuppliers", supplierTemplates
                            )
                    ));
                }
            }
            if (actor != null && actor.getControlMode() == PlayerControlMode.HUMAN && actor.getInfluence() > 0) {
                for (PolicyState policy : state.pendingPoliciesInOrder()) {
                    ProposalToken token = policy.getOccupyingProposalToken();
                    if (token != null
                            && Objects.equals(proposalTokenOwnerPlayerId(token), actor.getPlayerId())
                            && isImmediateExtraordinaryVoteWindow(state, actor, policy)) {
                        moves.add(new LegalMove(
                                "call-extraordinary-vote-template:" + actor.getPlayerId() + ":" + policy.getId(),
                                ActionType.CALL_EXTRAORDINARY_VOTE,
                                "Spend 1 influence to resolve this pending proposal immediately with an extraordinary vote.",
                                false,
                                Map.of("actorPlayerId", actor.getPlayerId(), "policyId", policy.getId().name(), "influenceCost", 1)
                        ));
                    }
                }
            }
        }

        if ((state.getTurnOrder().getPhase() == RoundPhase.VOTING || state.getTurnOrder().getPhase() == RoundPhase.ACTIONS)
                && state.getCurrentVoteState() != null) {
            CurrentVoteState session = state.getCurrentVoteState();
            if (session.getVotingStage() == VotingStage.DECLARE_STANCES) {
                moves.add(new LegalMove("declare-stance-template", ActionType.DECLARE_VOTE_STANCE, "Declare FOR/AGAINST stance for current vote.", false,
                        Map.of("policyId", session.getActiveProposalPolicyId().name(), "stances", List.of("FOR", "AGAINST"))));
            }
            if (session.getVotingStage() == VotingStage.COMMIT_INFLUENCE) {
                moves.add(new LegalMove("commit-influence-template", ActionType.COMMIT_VOTE_INFLUENCE, "Commit influence for current vote.", false, Map.of("min", 0)));
            }
        }

        if (state.getTurnOrder().getPhase() == RoundPhase.PRODUCTION) {
            ProductionPhaseState production = state.getProductionPhaseState();
            if (production == null || !production.isProductionResolved()) {
                moves.add(new LegalMove(
                        "resolve-production-phase-template",
                        ActionType.RESOLVE_PRODUCTION_PHASE,
                        "Resolve wages and production for production phase.",
                        false,
                        Map.of("stage", production == null ? ProductionSubPhase.PRODUCE_GOODS_AND_SERVICES.name() : production.getStage().name())
                ));
            }
            if (production != null && production.isRoundAdvanceReady()) {
                moves.add(new LegalMove(
                        "advance-to-voting-template",
                        ActionType.ADVANCE_TO_VOTING,
                        "Advance from PRODUCTION to VOTING after production resolution.",
                        false,
                        Map.of()
                ));
            }
        }

        if (state.getTurnOrder().getPhase() == RoundPhase.VOTING && state.getCurrentVoteState() == null) {
            moves.add(new LegalMove(
                    "advance-to-scoring-template",
                    ActionType.ADVANCE_TO_SCORING,
                    "Advance from VOTING to SCORING when no pending votes remain.",
                    false,
                    Map.of()
            ));
        }

        if (state.getTurnOrder().getPhase() == RoundPhase.SCORING) {
            boolean resolved = isScoringResolvedForCurrentRound(state);
            if (!resolved) {
                moves.add(new LegalMove(
                        "resolve-scoring-phase-template",
                        ActionType.RESOLVE_SCORING_PHASE,
                        "Resolve supported scoring and produce scoring breakdown.",
                        false,
                        Map.of("round", state.getCurrentRound())
                ));
            } else if (state.getCurrentRound() < state.getMaxRounds()) {
                moves.add(new LegalMove(
                        "advance-to-next-round-template",
                        ActionType.ADVANCE_TO_NEXT_ROUND,
                        "Advance to the next round after scoring.",
                        false,
                        Map.of("nextRound", state.getCurrentRound() + 1)
                ));
            }
        }

        if (state.getTurnOrder().getPhase() == RoundPhase.PREPARATION) {
            moves.add(new LegalMove(
                    "resolve-preparation-phase-template",
                    ActionType.RESOLVE_PREPARATION_PHASE,
                    "Resolve supported preparation substeps and continue to ACTIONS.",
                    false,
                    Map.of("round", state.getCurrentRound())
            ));
        }

        PlayerState actor = state.currentPlayer();
        if (actor != null && state.getTurnOrder().getPhase() != RoundPhase.GAME_OVER) {
            if (state.getTurnOrder().getPhase() == RoundPhase.ACTIONS && consumerEconomyService.canConsumeResource(actor, ResourceType.HEALTHCARE)) {
                moves.add(new LegalMove(
                        "consume-healthcare-template:" + actor.getPlayerId(),
                        ActionType.CONSUME_HEALTHCARE,
                        "Consume healthcare for population and increase welfare.",
                        false,
                        Map.of(
                                "actorPlayerId", actor.getPlayerId(),
                                "requiredAmount", Math.max(0, actor.getPopulation()),
                                "availableAmount", actor.getGoodsAmount(ResourceType.HEALTHCARE.id())
                        )
                ));
            }
            if (state.getTurnOrder().getPhase() == RoundPhase.ACTIONS && consumerEconomyService.canConsumeResource(actor, ResourceType.EDUCATION)) {
                moves.add(new LegalMove(
                        "consume-education-template:" + actor.getPlayerId(),
                        ActionType.CONSUME_EDUCATION,
                        "Consume education for population and increase welfare.",
                        false,
                        Map.of(
                                "actorPlayerId", actor.getPlayerId(),
                                "requiredAmount", Math.max(0, actor.getPopulation()),
                                "availableAmount", actor.getGoodsAmount(ResourceType.EDUCATION.id())
                        )
                ));
            }
            if (state.getTurnOrder().getPhase() == RoundPhase.ACTIONS && consumerEconomyService.canConsumeResource(actor, ResourceType.LUXURY)) {
                moves.add(new LegalMove(
                        "consume-luxury-template:" + actor.getPlayerId(),
                        ActionType.CONSUME_LUXURY,
                        "Consume luxury for population and increase welfare.",
                        false,
                        Map.of(
                                "actorPlayerId", actor.getPlayerId(),
                                "requiredAmount", Math.max(0, actor.getPopulation()),
                                "availableAmount", actor.getGoodsAmount(ResourceType.LUXURY.id())
                        )
                ));
            }
            moves.add(new LegalMove(
                    "advance-game-flow-template",
                    ActionType.ADVANCE_GAME_FLOW,
                    "Execute one legal lifecycle step for the current phase.",
                    false,
                    Map.of("requiredActor", actor.getPlayerId())
            ));
        }

        return moves;
    }
    public ValidationResult validate(GameState state, GameCommand command) {
        if (state.isGameOver()) {
            return ValidationResult.invalid(
                    List.of("Command is not allowed because the game is already finished."),
                    List.of(
                            ValidationReasonCode.GAME_ALREADY_FINISHED,
                            ValidationReasonCode.COMMAND_NOT_ALLOWED_IN_GAME_OVER
                    )
            );
        }

        if (canUseHumanManualOverride(state, command)) {
            return ValidationResult.valid();
        }

        return switch (command) {
            case AdvanceGameFlowCommand advance -> validateAdvanceGameFlow(state, advance);
            case AdvanceToVotingCommand advance -> validateAdvanceToVoting(state, advance);
            case AdvanceToProductionCommand advance -> validateAdvanceToProduction(state, advance);
            case AdvanceToScoringCommand advance -> validateAdvanceToScoring(state, advance);
            case ResolveProductionPhaseCommand resolve -> validateResolveProductionPhase(state, resolve);
            case ResolveScoringPhaseCommand resolve -> validateResolveScoringPhase(state, resolve);
            case AdvanceToNextRoundCommand advance -> validateAdvanceToNextRound(state, advance);
            case ResolvePreparationPhaseCommand resolve -> validateResolvePreparationPhase(state, resolve);
            case AdvanceRoundCommand advance -> validateAdvanceToNextRound(state, new AdvanceToNextRoundCommand(advance.actorPlayerId()));
            case ProposeBillCommand propose -> validateProposeBill(state, propose);
            case AddVotingCubesCommand add -> validateAddVotingCubes(state, add);
            case CallExtraordinaryVoteCommand vote -> validateCallExtraordinaryVote(state, vote);
            case AssignWorkersCommand assign -> validateAssignWorkers(state, assign);
            case PlaceStrikesCommand strikes -> validatePlaceStrikes(state, strikes);
            case PlaceDemonstrationCommand demonstration -> validatePlaceDemonstration(state, demonstration);
            case BuyGoodsAndServicesCommand buy -> validateBuyGoodsAndServices(state, buy);
            case ConsumeHealthcareCommand consume -> validateConsumeCommand(state, consume.actorPlayerId(), ResourceType.HEALTHCARE);
            case ConsumeEducationCommand consume -> validateConsumeCommand(state, consume.actorPlayerId(), ResourceType.EDUCATION, consume.workerId(), consume.targetColor());
            case ConsumeLuxuryCommand consume -> validateConsumeCommand(state, consume.actorPlayerId(), ResourceType.LUXURY);
            case RefreshBusinessDealsCommand refresh -> validateRefreshBusinessDeals(state, refresh);
            case DeclareVoteStanceCommand stance -> validateDeclareVoteStance(state, stance);
            case CommitVoteInfluenceCommand commit -> validateCommitVoteInfluence(state, commit);
            case StartTurnCommand start -> validateLegacyAction(state, start.type());
            case EndTurnCommand end -> state.isDemoMode()
                    ? validateLegacyAction(state, end.type())
                    : validateEndTurn(state);
            case HireWorkerCommand hire -> validateLegacyAction(state, hire.type());
            case ProduceGoodsCommand produce -> validateLegacyAction(state, produce.type());
            case SellGoodsCommand sell -> validateLegacyAction(state, sell.type());
            case AdjustPolicyCommand adjust -> validateLegacyAction(state, adjust.type());
            case PlayCardCommand play -> validateLegacyAction(state, play.type());
            default -> {
                if (isLegacy(command.type()) && !state.isDemoMode()) {
                    yield ValidationResult.invalid(ValidationReasonCode.LEGACY_ACTION_DISABLED,
                            "Legacy demo actions are disabled in the Hegemony core slice.");
                }
                yield ValidationResult.invalid(ValidationReasonCode.UNSUPPORTED_ACTION, "Unsupported action in this slice: " + command.type());
            }
        };
    }

    public ApplyCommandResult apply(GameState currentState, GameCommand command) {
        ValidationResult validation = validate(currentState, command);
        if (!validation.isValid()) {
            return new ApplyCommandResult(validation, currentState.copy(), List.of());
        }

        GameState next = currentState.copy();
        for (PlayerState player : next.getPlayers()) {
            player.setLastWelfareDelta(0);
        }
        List<DomainEvent> events = new ArrayList<>();
        events.addAll(applyOpeningWorkerMigrationIfNeededInternal(next));

        events.addAll(switch (command) {
            case AdvanceGameFlowCommand advance -> applyAdvanceGameFlow(next, advance);
            case AdvanceToVotingCommand advance -> applyAdvanceToVoting(next, advance);
            case AdvanceToProductionCommand advance -> applyAdvanceToProduction(next, advance);
            case AdvanceToScoringCommand advance -> applyAdvanceToScoring(next, advance);
            case ResolveProductionPhaseCommand resolve -> applyResolveProductionPhase(next, resolve);
            case ResolveScoringPhaseCommand resolve -> applyResolveScoringPhase(next, resolve);
            case AdvanceToNextRoundCommand advance -> applyAdvanceToNextRound(next, advance);
            case ResolvePreparationPhaseCommand resolve -> applyResolvePreparationPhase(next, resolve);
            case AdvanceRoundCommand advance -> applyAdvanceToNextRound(next, new AdvanceToNextRoundCommand(advance.actorPlayerId()));
            case ProposeBillCommand propose -> applyProposeBill(next, propose);
            case AddVotingCubesCommand add -> applyAddVotingCubes(next, add);
            case CallExtraordinaryVoteCommand vote -> applyCallExtraordinaryVote(next, vote);
            case AssignWorkersCommand assign -> applyAssignWorkers(next, assign);
            case PlaceStrikesCommand strikes -> applyPlaceStrikes(next, strikes);
            case PlaceDemonstrationCommand demonstration -> applyPlaceDemonstration(next, demonstration);
            case BuyGoodsAndServicesCommand buy -> applyBuyGoodsAndServices(next, buy);
            case ConsumeHealthcareCommand consume -> applyConsumeCommand(next, consume.actorPlayerId(), ResourceType.HEALTHCARE);
            case ConsumeEducationCommand consume -> applyConsumeCommand(next, consume.actorPlayerId(), ResourceType.EDUCATION, consume.workerId(), consume.targetColor());
            case ConsumeLuxuryCommand consume -> applyConsumeCommand(next, consume.actorPlayerId(), ResourceType.LUXURY);
            case RefreshBusinessDealsCommand refresh -> applyRefreshBusinessDeals(next, refresh);
            case DeclareVoteStanceCommand stance -> applyDeclareVoteStance(next, stance);
            case CommitVoteInfluenceCommand commit -> applyCommitVoteInfluence(next, commit);
            case StartTurnCommand start -> applyLegacyStartTurn(next);
            case EndTurnCommand end -> next.isDemoMode()
                    ? applyLegacyEndTurn(next)
                    : applyEndTurn(next);
            case HireWorkerCommand hire -> applyLegacyHireWorker(next, hire);
            case ProduceGoodsCommand produce -> applyLegacyProduceGoods(next, produce);
            case SellGoodsCommand sell -> applyLegacySellGoods(next, sell);
            case AdjustPolicyCommand adjust -> applyLegacyAdjustPolicy(next, adjust);
            case PlayCardCommand play -> applyLegacyPlayCard(next, play);
            default -> List.of();
        });

        clearDemonstrationIfNoLongerValid(next);
        normalizeEconomyState(next);
        next.refreshLegacyPlayerSnapshots();
        events.forEach(event -> next.appendLog(event.type(), event.description()));
        return new ApplyCommandResult(validation, next, events);
    }

    public List<DomainEvent> applyOpeningWorkerMigrationIfNeeded(GameState state) {
        if (state == null) {
            return List.of();
        }
        List<DomainEvent> events = applyOpeningWorkerMigrationIfNeededInternal(state);
        if (events.isEmpty()) {
            return events;
        }
        normalizeEconomyState(state);
        state.refreshLegacyPlayerSnapshots();
        events.forEach(event -> state.appendLog(event.type(), event.description()));
        return events;
    }

    private List<GameCommand> generateActionPhaseCommands(GameState state) {
        List<GameCommand> commands = new ArrayList<>();
        PlayerState currentPlayer = state.currentPlayer();
        if (currentPlayer == null) {
            return commands;
        }
        if (state.getCurrentVoteState() != null) {
            return generateVotingPhaseCommands(state);
        }

        if (currentPlayer.getControlMode() == PlayerControlMode.HUMAN && votingBagRules.ownerForClass(currentPlayer.getClassType()) != null) {
            commands.add(new AddVotingCubesCommand(currentPlayer.getPlayerId()));
        }
        if (currentPlayer.getControlMode() == PlayerControlMode.HUMAN && currentPlayer.getInfluence() > 0) {
            for (PolicyState policy : state.pendingPoliciesInOrder()) {
                ProposalToken token = policy.getOccupyingProposalToken();
                if (token != null && Objects.equals(proposalTokenOwnerPlayerId(token), currentPlayer.getPlayerId())) {
                    commands.add(new CallExtraordinaryVoteCommand(currentPlayer.getPlayerId(), policy.getId()));
                }
            }
        }
        commands.addAll(generateProposeBillCommands(state, currentPlayer));
        maybeGenerateSimpleAssignWorkersCommand(state, currentPlayer).ifPresent(commands::add);
        maybeGenerateSimpleStrikeCommand(state, currentPlayer).ifPresent(commands::add);
        maybeGenerateSimpleDemonstrationCommand(state, currentPlayer).ifPresent(commands::add);
        maybeGenerateSimpleConsumerCommands(state, currentPlayer).forEach(commands::add);
        commands.add(new EndTurnCommand());
        if (allActionTurnsCompleted(state)) {
            commands.add(new AdvanceToProductionCommand(currentPlayer.getPlayerId()));
        }
        commands.add(new AdvanceGameFlowCommand(currentPlayer.getPlayerId()));

        if (state.isDemoMode()) {
            commands.add(new StartTurnCommand());
            commands.add(new EndTurnCommand());
            commands.add(new HireWorkerCommand(1));
            commands.add(new ProduceGoodsCommand(1));
            commands.add(new SellGoodsCommand(1));
            commands.add(new AdjustPolicyCommand(com.example.hegemony.domain.model.PolicyTrack.TAXATION, 1));
            commands.add(new PlayCardCommand(""));
        }

        return commands;
    }

    private List<GameCommand> generateVotingPhaseCommands(GameState state) {
        CurrentVoteState session = state.getCurrentVoteState();
        PlayerState actor = state.currentPlayer();
        if (session == null) {
            if (actor == null) {
                return List.of();
            }
            return List.of(
                    new AdvanceToScoringCommand(actor.getPlayerId()),
                    new AdvanceGameFlowCommand(actor.getPlayerId())
            );
        }

        List<GameCommand> commands = new ArrayList<>();
        List<PlayerState> activePlayers = activePlayers(state);
        if (session.getVotingStage() == VotingStage.DECLARE_STANCES) {
            for (PlayerState player : activePlayers) {
                if (session.getStanceByPlayer().containsKey(player.getPlayerId())) {
                    continue;
                }
                if (isAutomaControlled(player)) {
                    VoteStance stance = automaStanceFor(state, session, player);
                    commands.add(new DeclareVoteStanceCommand(player.getPlayerId(), session.getActiveProposalPolicyId(), stance.name()));
                    continue;
                }
                commands.add(new DeclareVoteStanceCommand(player.getPlayerId(), session.getActiveProposalPolicyId(), VoteStance.FOR.name()));
                if (!Objects.equals(player.getPlayerId(), session.getProposalAuthorPlayerId())) {
                    commands.add(new DeclareVoteStanceCommand(player.getPlayerId(), session.getActiveProposalPolicyId(), VoteStance.AGAINST.name()));
                }
            }
        }

        if (session.getVotingStage() == VotingStage.COMMIT_INFLUENCE) {
            for (PlayerState player : activePlayers) {
                if (session.getInfluenceCommitments().containsKey(player.getPlayerId())) {
                    continue;
                }
                if (isAutomaControlled(player)) {
                    commands.add(new CommitVoteInfluenceCommand(player.getPlayerId(), automaInfluenceCommitment(state, session, player)));
                    continue;
                }
                int available = Math.max(0, player.getInfluence());
                for (int amount = 0; amount <= available; amount++) {
                    commands.add(new CommitVoteInfluenceCommand(player.getPlayerId(), amount));
                }
            }
        }
        if (actor != null) {
            commands.add(new AdvanceGameFlowCommand(actor.getPlayerId()));
        }
        return commands;
    }

    private List<GameCommand> generateProductionPhaseCommands(GameState state) {
        PlayerState actor = state.currentPlayer();
        if (actor == null) {
            return List.of();
        }

        List<GameCommand> commands = new ArrayList<>();
        ProductionPhaseState production = state.getProductionPhaseState();
        if (production == null || !production.isProductionResolved()) {
            commands.add(new ResolveProductionPhaseCommand(actor.getPlayerId()));
        } else if (production.isRoundAdvanceReady()) {
            commands.add(new AdvanceToVotingCommand(actor.getPlayerId()));
        }
        commands.add(new AdvanceGameFlowCommand(actor.getPlayerId()));
        return commands;
    }

    private List<GameCommand> generatePreparationPhaseCommands(GameState state) {
        PlayerState actor = state.currentPlayer();
        if (actor == null || state.getCurrentRound() == 1) {
            return List.of();
        }
        return List.of(
                new ResolvePreparationPhaseCommand(actor.getPlayerId()),
                new AdvanceGameFlowCommand(actor.getPlayerId())
        );
    }

    private List<GameCommand> generateScoringPhaseCommands(GameState state) {
        PlayerState actor = state.currentPlayer();
        if (actor == null) {
            return List.of();
        }

        List<GameCommand> commands = new ArrayList<>();
        if (!isScoringResolvedForCurrentRound(state)) {
            commands.add(new ResolveScoringPhaseCommand(actor.getPlayerId()));
        } else if (state.getCurrentRound() < state.getMaxRounds() && state.getGameStatus() == GameStatus.IN_PROGRESS) {
            commands.add(new AdvanceToNextRoundCommand(actor.getPlayerId()));
        }
        commands.add(new AdvanceGameFlowCommand(actor.getPlayerId()));
        return commands;
    }

    private ValidationResult validateAdvanceToVoting(GameState state, AdvanceToVotingCommand command) {
        List<String> errors = new ArrayList<>();
        List<ValidationReasonCode> codes = new ArrayList<>();
        if (state.getTurnOrder().getPhase() != RoundPhase.PRODUCTION) {
            addError(errors, codes, ValidationReasonCode.NOT_IN_PRODUCTION_PHASE, "ADVANCE_TO_VOTING is only allowed in PRODUCTION phase.");
        }
        PlayerState current = state.currentPlayer();
        if (current == null || !Objects.equals(current.getPlayerId(), command.actorPlayerId())) {
            addError(errors, codes, ValidationReasonCode.NOT_CURRENT_PLAYER, "Only current player can advance to voting.");
        }
        ProductionPhaseState production = state.getProductionPhaseState();
        if (production == null || !production.isRoundAdvanceReady()) {
            addError(errors, codes, ValidationReasonCode.INVALID_PHASE_TRANSITION,
                    "PRODUCTION must be resolved before advancing to voting.");
        }
        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors, codes);
    }

    private ValidationResult validateAdvanceToProduction(GameState state, AdvanceToProductionCommand command) {
        List<String> errors = new ArrayList<>();
        List<ValidationReasonCode> codes = new ArrayList<>();

        if (state.getTurnOrder().getPhase() != RoundPhase.ACTIONS) {
            addError(errors, codes, ValidationReasonCode.NOT_IN_ACTIONS_PHASE, "ADVANCE_TO_PRODUCTION is only allowed in ACTIONS phase.");
        }

        PlayerState current = state.currentPlayer();
        if (current == null || !Objects.equals(current.getPlayerId(), command.actorPlayerId())) {
            addError(errors, codes, ValidationReasonCode.NOT_CURRENT_PLAYER, "Only current player can advance to production.");
        }

        if (!allActionTurnsCompleted(state)) {
            addError(errors, codes, ValidationReasonCode.INVALID_PHASE_TRANSITION,
                    "ACTIONS phase can end only after every player completes 5 turns.");
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors, codes);
    }

    private ValidationResult validateEndTurn(GameState state) {
        List<String> errors = new ArrayList<>();
        List<ValidationReasonCode> codes = new ArrayList<>();

        if (state.getTurnOrder().getPhase() != RoundPhase.ACTIONS) {
            addError(errors, codes, ValidationReasonCode.NOT_IN_ACTIONS_PHASE, "END_TURN is only allowed in ACTIONS phase.");
        }

        if (state.currentPlayer() == null) {
            addError(errors, codes, ValidationReasonCode.NOT_CURRENT_PLAYER, "No current player is available to end the turn.");
        }
        if (state.getCurrentVoteState() != null) {
            addError(errors, codes, ValidationReasonCode.NOT_CURRENT_VOTING_STAGE, "Resolve the active extraordinary vote before ending the action turn.");
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors, codes);
    }

    private ValidationResult validateResolveProductionPhase(GameState state, ResolveProductionPhaseCommand command) {
        List<String> errors = new ArrayList<>();
        List<ValidationReasonCode> codes = new ArrayList<>();

        if (state.getTurnOrder().getPhase() != RoundPhase.PRODUCTION) {
            addError(errors, codes, ValidationReasonCode.NOT_IN_PRODUCTION_PHASE, "RESOLVE_PRODUCTION_PHASE is only allowed in PRODUCTION phase.");
        }

        PlayerState current = state.currentPlayer();
        if (current == null || !Objects.equals(current.getPlayerId(), command.actorPlayerId())) {
            addError(errors, codes, ValidationReasonCode.NOT_CURRENT_PLAYER, "Only current player can resolve production phase.");
        }

        if (state.getProductionPhaseState() != null && state.getProductionPhaseState().isProductionResolved()) {
            addError(errors, codes, ValidationReasonCode.PRODUCTION_ALREADY_RESOLVED, "Production phase is already resolved.");
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors, codes);
    }

    private ValidationResult validateAdvanceToScoring(GameState state, AdvanceToScoringCommand command) {
        List<String> errors = new ArrayList<>();
        List<ValidationReasonCode> codes = new ArrayList<>();

        if (state.getTurnOrder().getPhase() != RoundPhase.VOTING) {
            addError(errors, codes, ValidationReasonCode.NOT_IN_VOTING_PHASE, "ADVANCE_TO_SCORING is only allowed in VOTING phase.");
        }

        PlayerState current = state.currentPlayer();
        if (current == null || !Objects.equals(current.getPlayerId(), command.actorPlayerId())) {
            addError(errors, codes, ValidationReasonCode.NOT_CURRENT_PLAYER, "Only current player can advance to scoring.");
        }

        if (state.getCurrentVoteState() != null) {
            addError(errors, codes, ValidationReasonCode.CANNOT_ADVANCE_ROUND_BEFORE_SCORING,
                    "Cannot advance to scoring while vote session is still active.");
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors, codes);
    }

    private ValidationResult validateResolveScoringPhase(GameState state, ResolveScoringPhaseCommand command) {
        List<String> errors = new ArrayList<>();
        List<ValidationReasonCode> codes = new ArrayList<>();

        if (state.getTurnOrder().getPhase() != RoundPhase.SCORING) {
            addError(errors, codes, ValidationReasonCode.NOT_IN_SCORING_PHASE, "RESOLVE_SCORING_PHASE is only allowed in SCORING phase.");
        }

        PlayerState current = state.currentPlayer();
        if (current == null || !Objects.equals(current.getPlayerId(), command.actorPlayerId())) {
            addError(errors, codes, ValidationReasonCode.NOT_CURRENT_PLAYER, "Only current player can resolve scoring phase.");
        }

        if (isScoringResolvedForCurrentRound(state)) {
            addError(errors, codes, ValidationReasonCode.ROUND_ALREADY_COMPLETE, "Scoring is already resolved for current round.");
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors, codes);
    }

    private ValidationResult validateAdvanceToNextRound(GameState state, AdvanceToNextRoundCommand command) {
        List<String> errors = new ArrayList<>();
        List<ValidationReasonCode> codes = new ArrayList<>();

        if (state.getTurnOrder().getPhase() != RoundPhase.SCORING) {
            addError(errors, codes, ValidationReasonCode.NOT_IN_SCORING_PHASE, "ADVANCE_TO_NEXT_ROUND is only allowed in SCORING phase.");
        }

        PlayerState current = state.currentPlayer();
        if (current == null || !Objects.equals(current.getPlayerId(), command.actorPlayerId())) {
            addError(errors, codes, ValidationReasonCode.NOT_CURRENT_PLAYER, "Only current player can advance to next round.");
        }

        if (!isScoringResolvedForCurrentRound(state)) {
            addError(errors, codes, ValidationReasonCode.CANNOT_ADVANCE_ROUND_BEFORE_SCORING,
                    "Cannot advance round before scoring is resolved.");
        }

        if (state.getCurrentRound() >= state.getMaxRounds()) {
            addError(errors, codes, ValidationReasonCode.ROUND_ALREADY_COMPLETE,
                    "Current round is final. Resolve game end in scoring instead of advancing.");
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors, codes);
    }

    private ValidationResult validateResolvePreparationPhase(GameState state, ResolvePreparationPhaseCommand command) {
        List<String> errors = new ArrayList<>();
        List<ValidationReasonCode> codes = new ArrayList<>();

        if (state.getTurnOrder().getPhase() != RoundPhase.PREPARATION) {
            addError(errors, codes, ValidationReasonCode.NOT_IN_PREPARATION_PHASE,
                    "RESOLVE_PREPARATION_PHASE is only allowed in PREPARATION phase.");
        }

        PlayerState current = state.currentPlayer();
        if (current == null || !Objects.equals(current.getPlayerId(), command.actorPlayerId())) {
            addError(errors, codes, ValidationReasonCode.NOT_CURRENT_PLAYER, "Only current player can resolve preparation phase.");
        }

        if (state.getCurrentRound() != 1 && isPreparationResolvedForCurrentRound(state)) {
            addError(errors, codes, ValidationReasonCode.ROUND_ALREADY_COMPLETE, "Preparation is already resolved for current round.");
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors, codes);
    }

    private ValidationResult validateRefreshBusinessDeals(GameState state, RefreshBusinessDealsCommand command) {
        return ValidationResult.invalid(
                ValidationReasonCode.UNSUPPORTED_ACTION,
                "Business deals refresh only during preparation; manual refresh is not a legal gameplay action."
        );
    }

    private ValidationResult validateAdvanceGameFlow(GameState state, AdvanceGameFlowCommand command) {
        List<String> errors = new ArrayList<>();
        List<ValidationReasonCode> codes = new ArrayList<>();

        PlayerState current = state.currentPlayer();
        if (current == null || !Objects.equals(current.getPlayerId(), command.actorPlayerId())) {
            addError(errors, codes, ValidationReasonCode.NOT_CURRENT_PLAYER, "Only current player can advance game flow.");
            return ValidationResult.invalid(errors, codes);
        }

        GameCommand nextFlowCommand = nextFlowCommand(state, command.actorPlayerId());
        if (nextFlowCommand == null) {
            addError(errors, codes, ValidationReasonCode.INVALID_PHASE_TRANSITION,
                    "No lifecycle step can be advanced from current state.");
            return ValidationResult.invalid(errors, codes);
        }

        return validate(state, nextFlowCommand);
    }

    private ValidationResult validateProposeBill(GameState state, ProposeBillCommand command) {
        List<String> errors = new ArrayList<>();
        List<ValidationReasonCode> codes = new ArrayList<>();

        if (state.getTurnOrder().getPhase() != RoundPhase.ACTIONS) {
            addError(errors, codes, ValidationReasonCode.NOT_IN_ACTIONS_PHASE, "PROPOSE_BILL is only allowed in ACTIONS phase.");
        }

        PlayerState actor = state.findPlayerById(command.actorPlayerId()).orElse(null);
        if (actor == null || state.currentPlayer() == null || !Objects.equals(state.currentPlayer().getPlayerId(), command.actorPlayerId())) {
            addError(errors, codes, ValidationReasonCode.NOT_CURRENT_PLAYER, "Only current player can propose bill.");
        }
        if (actor != null && actor.availableProposalTokens() <= 0) {
            addError(errors, codes, ValidationReasonCode.NO_AVAILABLE_PROPOSAL_TOKEN, "No available proposal token.");
        }
        if (state.getCurrentVoteState() != null) {
            addError(errors, codes, ValidationReasonCode.NOT_CURRENT_VOTING_STAGE, "Cannot propose a bill while a vote is active.");
        }
        PolicyState policy = state.findPolicy(command.policyId()).orElse(null);
        if (policy == null) {
            addError(errors, codes, ValidationReasonCode.INVALID_POLICY, "Policy does not exist.");
        }
        if (command.targetCourse() == null) {
            addError(errors, codes, ValidationReasonCode.INVALID_TARGET_COURSE, "Target course is required.");
        }
        if (policy != null && policy.getOccupyingProposalToken() != null) {
            addError(errors, codes, ValidationReasonCode.POLICY_ALREADY_HAS_PROPOSAL, "Policy already has pending proposal token.");
        }
        if (policy != null && command.targetCourse() != null && !policy.getCurrentCourse().isAdjacentTo(command.targetCourse())) {
            addError(errors, codes, ValidationReasonCode.TARGET_COURSE_NOT_ADJACENT, "Target course must be adjacent to current course.");
        }
        if (actor != null
                && isAutomaControlled(actor)
                && state.getCurrentRound() >= state.getMaxRounds()
                && policy != null
                && policy.getId() == PolicyId.POLICY_6_FOREIGN_TRADE
                && command.targetCourse() != null
                && !wouldAutomaCallExtraordinaryVote(state, actor, policy, command.targetCourse())) {
            addError(errors, codes, ValidationReasonCode.UNSUPPORTED_ACTION,
                    "In round 5, automa proposes foreign trade only when it can call an extraordinary vote.");
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors, codes);
    }

    private ValidationResult validateAddVotingCubes(GameState state, AddVotingCubesCommand command) {
        List<String> errors = new ArrayList<>();
        List<ValidationReasonCode> codes = new ArrayList<>();

        if (state.getTurnOrder().getPhase() != RoundPhase.ACTIONS) {
            addError(errors, codes, ValidationReasonCode.NOT_IN_ACTIONS_PHASE, "ADD_VOTING_CUBES is only allowed in ACTIONS phase.");
        }

        PlayerState actor = state.findPlayerById(command.actorPlayerId()).orElse(null);
        if (actor == null || state.currentPlayer() == null || !Objects.equals(state.currentPlayer().getPlayerId(), command.actorPlayerId())) {
            addError(errors, codes, ValidationReasonCode.NOT_CURRENT_PLAYER, "Only current player can add voting cubes.");
        }
        if (actor != null && actor.getControlMode() != PlayerControlMode.HUMAN) {
            addError(errors, codes, ValidationReasonCode.UNSUPPORTED_ACTION, "Only human players can use ADD_VOTING_CUBES.");
        }
        if (actor != null && votingBagRules.ownerForClass(actor.getClassType()) == null) {
            addError(errors, codes, ValidationReasonCode.UNSUPPORTED_ACTION, "Actor class has no voting cube color in this slice.");
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors, codes);
    }

    private ValidationResult validateCallExtraordinaryVote(GameState state, CallExtraordinaryVoteCommand command) {
        List<String> errors = new ArrayList<>();
        List<ValidationReasonCode> codes = new ArrayList<>();

        if (state.getTurnOrder().getPhase() != RoundPhase.ACTIONS) {
            addError(errors, codes, ValidationReasonCode.NOT_IN_ACTIONS_PHASE, "CALL_EXTRAORDINARY_VOTE is only allowed in ACTIONS phase.");
        }

        PlayerState actor = state.findPlayerById(command.actorPlayerId()).orElse(null);
        if (actor == null || state.currentPlayer() == null || !Objects.equals(state.currentPlayer().getPlayerId(), command.actorPlayerId())) {
            addError(errors, codes, ValidationReasonCode.NOT_CURRENT_PLAYER, "Only current player can call an extraordinary vote.");
        }
        if (actor != null && votingBagRules.ownerForClass(actor.getClassType()) == null) {
            addError(errors, codes, ValidationReasonCode.UNSUPPORTED_ACTION, "Actor class has no voting cube color in this slice.");
        }
        if (actor != null && actor.getInfluence() < 1) {
            addError(errors, codes, ValidationReasonCode.INFLUENCE_EXCEEDS_AVAILABLE, "Extraordinary vote requires 1 influence.");
        }
        if (state.getCurrentVoteState() != null) {
            addError(errors, codes, ValidationReasonCode.NOT_CURRENT_VOTING_STAGE, "Another vote is already active.");
        }

        PolicyState policy = state.findPolicy(command.policyId()).orElse(null);
        if (policy == null) {
            addError(errors, codes, ValidationReasonCode.INVALID_POLICY, "Policy does not exist.");
        } else {
            ProposalToken token = policy.getOccupyingProposalToken();
            if (token == null) {
                addError(errors, codes, ValidationReasonCode.NO_PENDING_PROPOSAL_FOR_POLICY, "Policy has no pending proposal.");
            } else if (actor != null && !Objects.equals(proposalTokenOwnerPlayerId(token), actor.getPlayerId())) {
                addError(errors, codes, ValidationReasonCode.NO_PENDING_PROPOSAL_FOR_POLICY, "Only the proposal author can call an extraordinary vote.");
            } else if (actor != null && !isImmediateExtraordinaryVoteWindow(state, actor, policy)) {
                addError(errors, codes, ValidationReasonCode.UNSUPPORTED_ACTION, "Extraordinary vote can only be called immediately after proposing this bill.");
            }
        }
        if (actor != null
                && isAutomaControlled(actor)
                && policy != null
                && !shouldAutomaCallExtraordinaryVote(state, actor, policy)) {
            addError(errors, codes, ValidationReasonCode.UNSUPPORTED_ACTION, "Automa calls an extraordinary vote only when its influence side is ahead.");
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors, codes);
    }

    private boolean isImmediateExtraordinaryVoteWindow(GameState state, PlayerState actor, PolicyState policy) {
        if (state == null || actor == null || policy == null || policy.getOccupyingProposalToken() == null) {
            return false;
        }
        ProposalToken token = policy.getOccupyingProposalToken();
        if (!Objects.equals(proposalTokenOwnerPlayerId(token), actor.getPlayerId())) {
            return false;
        }
        List<EventLogEntry> log = state.getEventLog();
        if (log == null || log.isEmpty()) {
            return false;
        }
        for (int i = log.size() - 1; i >= 0; i--) {
            EventLogEntry candidate = log.get(i);
            if ("ACTION_REJECTED".equals(candidate.getType()) || "ACTION_FAILED".equals(candidate.getType())) {
                continue;
            }
            return "BILL_PROPOSED".equals(candidate.getType())
                    && candidate.getMessage() != null
                    && candidate.getMessage().contains(policy.getId().name());
        }
        return false;
    }

    private ValidationResult validateAssignWorkers(GameState state, AssignWorkersCommand command) {
        List<String> errors = new ArrayList<>();
        List<ValidationReasonCode> codes = new ArrayList<>();

        if (state.getTurnOrder().getPhase() != RoundPhase.ACTIONS) {
            addError(errors, codes, ValidationReasonCode.NOT_IN_ACTIONS_PHASE, "ASSIGN_WORKERS is only allowed in ACTIONS phase.");
        }

        PlayerState actor = state.findPlayerById(command.actorPlayerId()).orElse(null);
        if (actor == null || state.currentPlayer() == null || !Objects.equals(state.currentPlayer().getPlayerId(), command.actorPlayerId())) {
            addError(errors, codes, ValidationReasonCode.NOT_CURRENT_PLAYER, "Only current player can assign workers.");
        }

        if (command.assignments() == null || command.assignments().isEmpty()) {
            addError(errors, codes, ValidationReasonCode.INVALID_TARGET, "Assignments list cannot be empty.");
            return ValidationResult.invalid(errors, codes);
        }

        if (command.assignments().size() > 3) {
            addError(errors, codes, ValidationReasonCode.TOO_MANY_WORKERS_IN_ONE_ASSIGN_ACTION, "At most 3 workers can be assigned in one action.");
        }

        Set<String> seenWorkers = new HashSet<>();
        Set<String> seenSlots = new HashSet<>();
        boolean hasEnterpriseTargets = false;

        for (WorkerAssignmentOperation op : command.assignments()) {
            if (op.targetType() == AssignmentTargetType.UNION) {
                addError(errors, codes, ValidationReasonCode.UNSUPPORTED_UNION_ASSIGNMENT, "Union assignment is unsupported in current slice.");
                continue;
            }

            Worker worker = state.findWorker(op.workerId()).orElse(null);
            if (worker == null) {
                addError(errors, codes, ValidationReasonCode.INVALID_TARGET, "Worker not found: " + op.workerId());
                continue;
            }
            if (actor != null && worker.getClassType() != actor.getClassType()) {
                addError(errors, codes, ValidationReasonCode.WORKER_NOT_OWNED_BY_ACTOR, "Worker does not belong to actor class: " + worker.getId());
            }
            if (worker.getLocation() == WorkerLocation.UNION) {
                addError(errors, codes, ValidationReasonCode.UNSUPPORTED_UNION_ASSIGNMENT, "Union worker reassignment is unsupported in current slice.");
            }
            if (worker.isTiedContract()) {
                addError(errors, codes, ValidationReasonCode.WORKER_TIED_BY_CONTRACT, "Worker is tied by contract and cannot be reassigned: " + worker.getId());
            }
            if (!seenWorkers.add(worker.getId())) {
                addError(errors, codes, ValidationReasonCode.INVALID_TARGET, "Duplicate worker in one action: " + worker.getId());
            }

            if (op.targetType() == AssignmentTargetType.UNEMPLOYED) {
                if (worker.getLocation() != WorkerLocation.ENTERPRISE_SLOT) {
                    addError(errors, codes, ValidationReasonCode.INVALID_TARGET, "Only employed workers can be returned to unemployed: " + worker.getId());
                }
                continue;
            }

            hasEnterpriseTargets = true;
            SlotRef target = parseSlotRef(op.targetId());
            if (target == null) {
                addError(errors, codes, ValidationReasonCode.INVALID_TARGET, "Invalid ENTERPRISE_SLOT targetId format: " + op.targetId());
                continue;
            }
            Enterprise enterprise = state.findEnterprise(target.enterpriseId()).orElse(null);
            if (enterprise == null) {
                addError(errors, codes, ValidationReasonCode.INVALID_TARGET, "Target enterprise not found: " + target.enterpriseId());
                continue;
            }
            EnterpriseSlot slot = enterprise.getSlots().stream().filter(s -> s.getId().equals(target.slotId())).findFirst().orElse(null);
            if (slot == null) {
                addError(errors, codes, ValidationReasonCode.INVALID_TARGET, "Target slot not found: " + target.slotId());
                continue;
            }
            if (slot.isOccupied() && !Objects.equals(slot.getOccupiedWorkerId(), worker.getId())) {
                addError(errors, codes, ValidationReasonCode.INVALID_TARGET, "Target slot is already occupied: " + slot.getId());
            }
            if (!seenSlots.add(target.enterpriseId() + ":" + target.slotId())) {
                addError(errors, codes, ValidationReasonCode.INVALID_TARGET, "Duplicate target slot in one action: " + target.slotId());
            }
            if (!slotMatchesWorker(slot, worker)) {
                addError(errors, codes, ValidationReasonCode.SLOT_QUALIFICATION_MISMATCH, "Worker " + worker.getId() + " does not match slot " + slot.getId());
            }
        }

        if (errors.isEmpty() && hasEnterpriseTargets && !enterprisesRemainBinaryAfterSimulation(state, command.assignments())) {
            addError(errors, codes, ValidationReasonCode.ENTERPRISE_CANNOT_BE_PARTIALLY_FILLED,
                    "Enterprises must end fully staffed or fully empty after whole assignment action.");
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors, codes);
    }

    private ValidationResult validatePlaceStrikes(GameState state, PlaceStrikesCommand command) {
        List<String> errors = new ArrayList<>();
        List<ValidationReasonCode> codes = new ArrayList<>();

        if (state.getTurnOrder().getPhase() != RoundPhase.ACTIONS) {
            addError(errors, codes, ValidationReasonCode.NOT_IN_ACTIONS_PHASE, "PLACE_STRIKES is only allowed in ACTIONS phase.");
        }

        PlayerState actor = state.findPlayerById(command.actorPlayerId()).orElse(null);
        if (actor == null || state.currentPlayer() == null || !Objects.equals(state.currentPlayer().getPlayerId(), command.actorPlayerId())) {
            addError(errors, codes, ValidationReasonCode.NOT_CURRENT_PLAYER, "Only current player can place strikes.");
        }
        if (actor != null && actor.getClassType() != ClassType.WORKER) {
            addError(errors, codes, ValidationReasonCode.UNSUPPORTED_ACTION, "Only the worker class can place strikes.");
        }

        List<String> enterpriseIds = command.enterpriseIds() == null ? List.of() : command.enterpriseIds();
        if (enterpriseIds.isEmpty()) {
            addError(errors, codes, ValidationReasonCode.INVALID_TARGET, "At least one enterprise is required for PLACE_STRIKES.");
        }
        if (enterpriseIds.size() > 2) {
            addError(errors, codes, ValidationReasonCode.TOO_MANY_STRIKES_IN_ONE_ACTION, "At most 2 strike tokens can be placed in one action.");
        }

        Set<String> seen = new HashSet<>();
        for (String enterpriseId : enterpriseIds) {
            if (!seen.add(enterpriseId)) {
                addError(errors, codes, ValidationReasonCode.INVALID_TARGET, "Duplicate enterprise target: " + enterpriseId);
                continue;
            }
            Enterprise enterprise = state.findEnterprise(enterpriseId).orElse(null);
            if (enterprise == null) {
                addError(errors, codes, ValidationReasonCode.INVALID_TARGET, "Enterprise not found: " + enterpriseId);
                continue;
            }
            validateStrikeTarget(state, enterprise, errors, codes);
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors, codes);
    }

    private ValidationResult validatePlaceDemonstration(GameState state, PlaceDemonstrationCommand command) {
        List<String> errors = new ArrayList<>();
        List<ValidationReasonCode> codes = new ArrayList<>();

        if (state.getTurnOrder().getPhase() != RoundPhase.ACTIONS) {
            addError(errors, codes, ValidationReasonCode.NOT_IN_ACTIONS_PHASE, "PLACE_DEMONSTRATION is only allowed in ACTIONS phase.");
        }

        PlayerState actor = state.findPlayerById(command.actorPlayerId()).orElse(null);
        if (actor == null || state.currentPlayer() == null || !Objects.equals(state.currentPlayer().getPlayerId(), command.actorPlayerId())) {
            addError(errors, codes, ValidationReasonCode.NOT_CURRENT_PLAYER, "Only current player can place a demonstration.");
        }
        if (actor != null && actor.getClassType() != ClassType.WORKER) {
            addError(errors, codes, ValidationReasonCode.UNSUPPORTED_ACTION, "Only the worker class can place a demonstration.");
        }
        if (state.isDemonstrationToken()) {
            addError(errors, codes, ValidationReasonCode.DEMONSTRATION_ALREADY_PRESENT, "A demonstration token is already present.");
        }
        if (!canPlaceDemonstration(state)) {
            addError(errors, codes, ValidationReasonCode.DEMONSTRATION_REQUIREMENT_NOT_MET,
                    "Worker unemployment must exceed available vacancies by at least 2.");
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors, codes);
    }

    private ValidationResult validateBuyGoodsAndServices(GameState state, BuyGoodsAndServicesCommand command) {
        List<String> errors = new ArrayList<>();
        List<ValidationReasonCode> codes = new ArrayList<>();

        if (state.getTurnOrder().getPhase() != RoundPhase.ACTIONS) {
            addError(errors, codes, ValidationReasonCode.NOT_IN_ACTIONS_PHASE, "BUY_GOODS_AND_SERVICES is only allowed in ACTIONS phase.");
        }

        PlayerState actor = state.findPlayerById(command.actorPlayerId()).orElse(null);
        if (actor == null || state.currentPlayer() == null || !Objects.equals(state.currentPlayer().getPlayerId(), command.actorPlayerId())) {
            addError(errors, codes, ValidationReasonCode.NOT_CURRENT_PLAYER, "Only current player can buy goods and services.");
        }

        if (actor != null && !consumerEconomyService.isSupportedConsumerBuyer(actor.getClassType())) {
            addError(errors, codes, ValidationReasonCode.NOT_SUPPORTED_BUYER_CLASS,
                    "Buyer class is unsupported in current consumer-economy slice: " + actor.getClassType());
        }

        ResourceType resourceType = ResourceType.fromRaw(command.resourceType());
        if (resourceType == null || !consumerEconomyService.supportedBuyResource(resourceType)) {
            addError(errors, codes, ValidationReasonCode.INVALID_RESOURCE_TYPE, "Unsupported resourceType for BUY_GOODS_AND_SERVICES.");
        }

        List<PurchaseItem> purchases = command.purchases() == null ? List.of() : command.purchases();
        if (purchases.isEmpty()) {
            addError(errors, codes, ValidationReasonCode.INVALID_TARGET, "At least one purchase item is required.");
        }
        long uniqueSuppliers = purchases.stream()
                .filter(Objects::nonNull)
                .map(item -> String.valueOf(item.supplierType()) + ":" + String.valueOf(item.supplierPlayerId()))
                .distinct()
                .count();
        if (uniqueSuppliers > 2) {
            addError(errors, codes, ValidationReasonCode.TOO_MANY_SUPPLIERS, "At most 2 suppliers are allowed for one purchase action.");
        }

        int totalQuantity = purchases.stream().mapToInt(item -> Math.max(0, item.quantity())).sum();
        if (totalQuantity <= 0) {
            addError(errors, codes, ValidationReasonCode.INVALID_TARGET, "Total purchase quantity must be > 0.");
        }

        if (actor != null && resourceType != null) {
            PurchaseEvaluation evaluation = consumerEconomyService.evaluatePurchasePlan(state, actor, resourceType, purchases, false);
            mergeEvaluationErrors(evaluation, errors, codes);
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors, codes);
    }

    private ValidationResult validateConsumeCommand(GameState state, String actorPlayerId, ResourceType resourceType) {
        return validateConsumeCommand(state, actorPlayerId, resourceType, "", WorkerSlotColor.WHITE);
    }

    private ValidationResult validateConsumeCommand(
            GameState state,
            String actorPlayerId,
            ResourceType resourceType,
            String workerId,
            WorkerSlotColor targetColor
    ) {
        List<String> errors = new ArrayList<>();
        List<ValidationReasonCode> codes = new ArrayList<>();

        if (state.getTurnOrder().getPhase() != RoundPhase.ACTIONS) {
            addError(errors, codes, ValidationReasonCode.NOT_IN_ACTIONS_PHASE, resourceType.name() + " consumption is only allowed in ACTIONS phase.");
        }

        PlayerState actor = state.findPlayerById(actorPlayerId).orElse(null);
        if (actor == null || state.currentPlayer() == null || !Objects.equals(state.currentPlayer().getPlayerId(), actorPlayerId)) {
            addError(errors, codes, ValidationReasonCode.NOT_CURRENT_PLAYER, "Only current player can run consumption action.");
            return ValidationResult.invalid(errors, codes);
        }

        if (!consumerEconomyService.isSupportedConsumerBuyer(actor.getClassType())) {
            addError(errors, codes, ValidationReasonCode.NOT_SUPPORTED_BUYER_CLASS,
                    "Consumption action is unsupported for class " + actor.getClassType() + ".");
        }

        if (!consumerEconomyService.supportedConsumptionResource(resourceType)) {
            addError(errors, codes, ValidationReasonCode.INVALID_CONSUMPTION_ACTION, "Unsupported consumption action for resource " + resourceType.name() + ".");
        }

        int required = Math.max(0, actor.getPopulation());
        int available = actor.getGoodsAmount(resourceType.id());
        if (available < required) {
            addError(errors, codes, ValidationReasonCode.INSUFFICIENT_RESOURCE_FOR_CONSUMPTION,
                    "Need " + required + " " + resourceType.id() + " to consume, available " + available + ".");
        }
        if (actor != null && resourceType == ResourceType.EDUCATION) {
            Worker trainable = findSelectedTrainableWorker(state, actor.getClassType(), workerId);
            if (trainable == null) {
                addError(errors, codes, ValidationReasonCode.INVALID_TARGET,
                        "Education consumption requires one selected unskilled worker owned by the acting class.");
            }
            if (targetColor == null || targetColor == WorkerSlotColor.GRAY || targetColor.toWorkerSector() == null) {
                addError(errors, codes, ValidationReasonCode.INVALID_TARGET,
                        "Education consumption requires a skilled target color.");
            }
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors, codes);
    }

    private ValidationResult validateDeclareVoteStance(GameState state, DeclareVoteStanceCommand command) {
        List<String> errors = new ArrayList<>();
        List<ValidationReasonCode> codes = new ArrayList<>();
        CurrentVoteState session = state.getCurrentVoteState();

        boolean extraordinaryActionVote = session != null
                && session.isExtraordinary()
                && state.getTurnOrder().getPhase() == RoundPhase.ACTIONS;
        if (state.getTurnOrder().getPhase() != RoundPhase.VOTING && !extraordinaryActionVote) {
            addError(errors, codes, ValidationReasonCode.NOT_IN_VOTING_PHASE, "DECLARE_VOTE_STANCE is only allowed in VOTING phase.");
        }
        if (session == null || session.getActiveProposalPolicyId() == null) {
            addError(errors, codes, ValidationReasonCode.NO_PENDING_PROPOSAL_FOR_POLICY, "No active proposal is currently being voted.");
            return ValidationResult.invalid(errors, codes);
        }
        if (session.getVotingStage() != VotingStage.DECLARE_STANCES) {
            addError(errors, codes, ValidationReasonCode.NOT_CURRENT_VOTING_STAGE, "Current voting stage does not accept stances.");
        }
        if (command.policyId() != session.getActiveProposalPolicyId()) {
            addError(errors, codes, ValidationReasonCode.NO_PENDING_PROPOSAL_FOR_POLICY, "Stance must target currently active policy proposal.");
        }

        PlayerState actor = state.findPlayerById(command.actorPlayerId()).orElse(null);
        if (actor == null || !state.getTurnOrder().getActiveClasses().contains(actor.getClassType())) {
            addError(errors, codes, ValidationReasonCode.NOT_CURRENT_PLAYER, "Actor is not an active class in current vote.");
        }
        if (session.getStanceByPlayer().containsKey(command.actorPlayerId())) {
            addError(errors, codes, ValidationReasonCode.STANCE_ALREADY_SUBMITTED, "Stance is already submitted for actor.");
        }

        VoteStance stance = parseStance(command.stance());
        if (stance == null) {
            addError(errors, codes, ValidationReasonCode.INVALID_STANCE, "Stance must be FOR or AGAINST.");
        }
        if (stance == VoteStance.AGAINST && Objects.equals(command.actorPlayerId(), session.getProposalAuthorPlayerId())) {
            addError(errors, codes, ValidationReasonCode.PROPOSER_CANNOT_VOTE_AGAINST, "Proposal author cannot vote AGAINST own proposal.");
        }
        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors, codes);
    }

    private ValidationResult validateCommitVoteInfluence(GameState state, CommitVoteInfluenceCommand command) {
        List<String> errors = new ArrayList<>();
        List<ValidationReasonCode> codes = new ArrayList<>();
        CurrentVoteState session = state.getCurrentVoteState();

        boolean extraordinaryActionVote = session != null
                && session.isExtraordinary()
                && state.getTurnOrder().getPhase() == RoundPhase.ACTIONS;
        if (state.getTurnOrder().getPhase() != RoundPhase.VOTING && !extraordinaryActionVote) {
            addError(errors, codes, ValidationReasonCode.NOT_IN_VOTING_PHASE, "COMMIT_VOTE_INFLUENCE is only allowed in VOTING phase.");
        }
        if (session == null || session.getActiveProposalPolicyId() == null) {
            addError(errors, codes, ValidationReasonCode.NO_PENDING_PROPOSAL_FOR_POLICY, "No active proposal to commit influence for.");
            return ValidationResult.invalid(errors, codes);
        }

        if (session.getVotingStage() == VotingStage.DECLARE_STANCES) {
            addError(errors, codes, ValidationReasonCode.NOT_CURRENT_VOTING_STAGE, "Influence cannot be committed before cube draw stage.");
            addError(errors, codes, ValidationReasonCode.CANNOT_RESOLVE_BEFORE_ALL_STANCES, "Cannot resolve vote before all stances are submitted.");
        } else if (session.getVotingStage() != VotingStage.COMMIT_INFLUENCE) {
            addError(errors, codes, ValidationReasonCode.NOT_CURRENT_VOTING_STAGE, "Current voting stage does not accept influence commits.");
            addError(errors, codes, ValidationReasonCode.CANNOT_RESOLVE_BEFORE_ALL_INFLUENCE_COMMITS,
                    "Cannot resolve vote before all influence commitments are submitted.");
        }

        PlayerState actor = state.findPlayerById(command.actorPlayerId()).orElse(null);
        if (actor == null || !state.getTurnOrder().getActiveClasses().contains(actor.getClassType())) {
            addError(errors, codes, ValidationReasonCode.NOT_CURRENT_PLAYER, "Actor is not an active class in current vote.");
        }
        if (session.getInfluenceCommitments().containsKey(command.actorPlayerId())) {
            addError(errors, codes, ValidationReasonCode.INFLUENCE_ALREADY_COMMITTED, "Influence already committed for actor.");
        }
        if (command.influenceAmount() < 0) {
            addError(errors, codes, ValidationReasonCode.INVALID_TARGET, "Influence commitment cannot be negative.");
        }
        if (actor != null && command.influenceAmount() > actor.getInfluence()) {
            addError(errors, codes, ValidationReasonCode.INFLUENCE_EXCEEDS_AVAILABLE, "Influence commitment exceeds available influence.");
        }
        if (!session.getStanceByPlayer().containsKey(command.actorPlayerId())) {
            addError(errors, codes, ValidationReasonCode.CANNOT_RESOLVE_BEFORE_ALL_STANCES, "Actor must declare stance before committing influence.");
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors, codes);
    }
    private List<DomainEvent> applyAdvanceGameFlow(GameState state, AdvanceGameFlowCommand command) {
        GameCommand next = nextFlowCommand(state, command.actorPlayerId());
        if (next == null) {
            return List.of();
        }
        return switch (next) {
            case AdvanceToVotingCommand advance -> applyAdvanceToVoting(state, advance);
            case AdvanceToProductionCommand advance -> applyAdvanceToProduction(state, advance);
            case ResolveProductionPhaseCommand resolve -> applyResolveProductionPhase(state, resolve);
            case AdvanceToScoringCommand advance -> applyAdvanceToScoring(state, advance);
            case ResolveScoringPhaseCommand resolve -> applyResolveScoringPhase(state, resolve);
            case AdvanceToNextRoundCommand advance -> applyAdvanceToNextRound(state, advance);
            case ResolvePreparationPhaseCommand resolve -> applyResolvePreparationPhase(state, resolve);
            case DeclareVoteStanceCommand stance -> applyDeclareVoteStance(state, stance);
            case CommitVoteInfluenceCommand commit -> applyCommitVoteInfluence(state, commit);
            default -> List.of();
        };
    }

    private List<DomainEvent> applyAdvanceToVoting(GameState state, AdvanceToVotingCommand command) {
        List<DomainEvent> events = new ArrayList<>();
        state.getTurnOrder().setPhase(RoundPhase.VOTING);
        state.setCurrentPhase(RoundPhase.VOTING);
        state.getTurnOrder().setCurrentPlayerIndex(0);

        List<PolicyState> pending = state.pendingPoliciesInOrder();
        PolicyId firstPolicy = pending.isEmpty() ? null : pending.get(0).getId();
        events.add(new VotingPhaseStartedEvent(state.getTurnOrder().getRound(), pending.size(), firstPolicy));

        if (pending.isEmpty()) {
            applyAdvanceToScoring(state, new AdvanceToScoringCommand(command.actorPlayerId()));
            state.appendLog("VOTING_SKIPPED", "No pending proposals, moving directly to scoring phase.");
        } else {
            votingBagRules.refill(state, 1, true);
            applyStateVotingInfluenceGain(state);
            startNextPendingVoteSession(state);
        }
        return events;
    }

    private List<DomainEvent> applyEndTurn(GameState state) {
        PlayerState current = state.currentPlayer();
        if (current == null) {
            return List.of();
        }

        state.getTurnOrder().recordCompletedActionForCurrentPlayer();
        PlayerRole endedBy = current.getRole();
        if (allActionTurnsCompleted(state)) {
            List<DomainEvent> events = new ArrayList<>();
            state.getTurnOrder().setCurrentPlayerIndex(0);
            PlayerState nextPlayer = state.currentPlayer();
            events.add(new TurnEndedEvent(
                    endedBy,
                    nextPlayer == null ? endedBy : nextPlayer.getRole(),
                    state.getTurnOrder().getCurrentPlayerIndex(),
                    state.getTurnOrder().getRound()
            ));
            events.addAll(applyAdvanceToProduction(state, new AdvanceToProductionCommand(current.getPlayerId())));
            return events;
        }

        state.getTurnOrder().moveToNextPlayer();
        PlayerState nextPlayer = state.currentPlayer();
        return List.of(new TurnEndedEvent(
                endedBy,
                nextPlayer == null ? endedBy : nextPlayer.getRole(),
                state.getTurnOrder().getCurrentPlayerIndex(),
                state.getTurnOrder().getRound()
        ));
    }

    private List<DomainEvent> applyAdvanceToProduction(GameState state, AdvanceToProductionCommand command) {
        enterProductionPhase(state);
        state.appendLog("PHASE_ADVANCED", "Advanced to production phase.");
        return List.of();
    }

    private List<DomainEvent> applyAdvanceToScoring(GameState state, AdvanceToScoringCommand command) {
        if (state.getProductionPhaseState() != null && state.getProductionPhaseState().isProductionResolved()) {
            state.getProductionPhaseState().setStage(ProductionSubPhase.COMPLETED);
            state.setLastProductionSummary(state.getProductionPhaseState().copy());
        }
        state.setProductionPhaseState(null);
        state.getTurnOrder().setPhase(RoundPhase.SCORING);
        state.setCurrentPhase(RoundPhase.SCORING);
        state.getTurnOrder().setCurrentPlayerIndex(0);
        state.appendLog("PHASE_ADVANCED", "Advanced to scoring phase.");
        return List.of();
    }

    private List<DomainEvent> applyProposeBill(GameState state, ProposeBillCommand command) {
        PlayerState actor = state.findPlayerById(command.actorPlayerId()).orElseThrow();
        PolicyState policy = state.findPolicy(command.policyId()).orElseThrow();

        ProposalToken token = actor.takeFirstAvailableProposalToken();
        if (token == null) {
            throw new IllegalStateException("No proposal token available after validation");
        }
        token.setOwnerPlayerId(actor.getPlayerId());
        token.setPolicyId(policy.getId());
        token.setTargetCourse(command.targetCourse());
        policy.setOccupyingProposalToken(token.copy());

        List<DomainEvent> events = new ArrayList<>();
        events.add(new BillProposedEvent(actor.getClassType(), policy.getId(), policy.getCurrentCourse(), command.targetCourse(), token.getId()));
        if (shouldAutomaCallExtraordinaryVote(state, actor, policy)) {
            events.addAll(startExtraordinaryVoteSession(state, actor, policy));
        }
        return events;
    }

    private List<DomainEvent> applyAddVotingCubes(GameState state, AddVotingCubesCommand command) {
        PlayerState actor = state.findPlayerById(command.actorPlayerId()).orElseThrow();
        VotingCubeOwnerClass ownerClass = votingBagRules.ownerForClass(actor.getClassType());
        if (ownerClass == null) {
            return List.of();
        }
        state.getVotingBag().add(ownerClass, ACTION_VOTING_CUBES_TO_ADD);
        state.appendLog(
                "VOTING_CUBES_ADDED",
                actor.getPlayerId() + " added " + ACTION_VOTING_CUBES_TO_ADD + " " + ownerClass.name() + " voting cubes to the bag."
        );
        return List.of();
    }

    private List<DomainEvent> applyCallExtraordinaryVote(GameState state, CallExtraordinaryVoteCommand command) {
        PlayerState actor = state.findPlayerById(command.actorPlayerId()).orElseThrow();
        PolicyState policy = state.findPolicy(command.policyId()).orElseThrow();
        return startExtraordinaryVoteSession(state, actor, policy);
    }

    private List<DomainEvent> startExtraordinaryVoteSession(GameState state, PlayerState actor, PolicyState policy) {
        ProposalToken token = policy.getOccupyingProposalToken();
        if (token == null) {
            return List.of();
        }

        actor.setInfluence(Math.max(0, actor.getInfluence() - 1));

        CurrentVoteState session = new CurrentVoteState();
        session.setActiveProposalPolicyId(policy.getId());
        session.setProposalAuthorPlayerId(actor.getPlayerId());
        session.setCurrentCourseBeforeVote(policy.getCurrentCourse());
        session.setTargetCourse(token.getTargetCourse());
        session.setVotingStage(VotingStage.DRAW_BAG_CUBES);
        session.setResult(VoteResolutionResult.PENDING);
        session.setExtraordinary(true);
        for (PlayerState player : activePlayers(state)) {
            VoteStance stance = Objects.equals(player.getPlayerId(), actor.getPlayerId())
                    ? VoteStance.FOR
                    : VoteStance.AGAINST;
            session.getStanceByPlayer().put(player.getPlayerId(), stance);
        }
        drawAndInterpretCubes(state, session);
        session.setVotingStage(VotingStage.COMMIT_INFLUENCE);
        state.setCurrentVoteState(session);
        state.appendLog(
                "EXTRAORDINARY_VOTE_STARTED",
                actor.getPlayerId() + " spent 1 influence and started an extraordinary vote on " + policy.getId() + "."
        );
        state.appendLog(
                "VOTING_CUBES_DRAWN",
                "Drawn " + session.getDrawnVotingCubes().size() + " existing cubes for extraordinary vote on " + policy.getId() + "."
        );
        appendPreliminaryVoteResultLog(state, session);
        autoCommitAutomaInfluence(state, session);
        if (allInfluenceCommitted(state, session)) {
            return resolveCurrentVoteAndAdvance(state, session);
        }
        return List.of();
    }

    private List<DomainEvent> applyAssignWorkers(GameState state, AssignWorkersCommand command) {
        List<DomainEvent> events = new ArrayList<>();
        Map<String, Worker> workerMap = new HashMap<>();
        for (Worker worker : state.getWorkers()) {
            workerMap.put(worker.getId(), worker);
        }
        Set<String> enterprisesWithExistingContracts = new HashSet<>();
        for (WorkerAssignmentOperation op : command.assignments()) {
            SlotRef target = parseSlotRef(op.targetId());
            if (target == null) {
                continue;
            }
            Enterprise enterprise = state.findEnterprise(target.enterpriseId()).orElse(null);
            if (enterprise != null && enterprise.getOwnerClass() != ClassType.MIDDLE_CLASS && enterpriseHasTiedWorkers(state, enterprise)) {
                enterprisesWithExistingContracts.add(enterprise.getId());
            }
        }

        for (WorkerAssignmentOperation op : command.assignments()) {
            Worker worker = workerMap.get(op.workerId());
            if (worker.getLocation() == WorkerLocation.ENTERPRISE_SLOT && worker.getEnterpriseId() != null && worker.getSlotId() != null) {
                state.findEnterprise(worker.getEnterpriseId()).ifPresent(previous -> previous.getSlots().stream()
                        .filter(s -> s.getId().equals(worker.getSlotId()))
                        .findFirst()
                        .ifPresent(slot -> slot.setOccupiedWorkerId(null)));
            }

            if (op.targetType() == AssignmentTargetType.UNEMPLOYED) {
                worker.setLocation(WorkerLocation.UNEMPLOYED);
                worker.setEnterpriseId(null);
                worker.setSlotId(null);
                worker.setTiedContract(false);
                state.appendLog("WORKER_RETURNED_TO_UNEMPLOYED", worker.getId() + " was returned to unemployed by " + command.actorPlayerId() + ".");
                continue;
            }

            SlotRef target = parseSlotRef(op.targetId());
            Enterprise enterprise = state.findEnterprise(target.enterpriseId()).orElseThrow();
            EnterpriseSlot slot = enterprise.getSlots().stream().filter(s -> s.getId().equals(target.slotId())).findFirst().orElseThrow();
            slot.setOccupiedWorkerId(worker.getId());

            worker.setLocation(WorkerLocation.ENTERPRISE_SLOT);
            worker.setEnterpriseId(enterprise.getId());
            worker.setSlotId(slot.getId());
            worker.setTiedContract(true);

            events.add(new WorkerAssignedEvent(worker.getId(), enterprise.getId(), slot.getId()));
        }

        for (String enterpriseId : enterprisesWithExistingContracts) {
            Enterprise enterprise = state.findEnterprise(enterpriseId).orElse(null);
            if (enterprise == null) {
                continue;
            }
            clearEnterpriseWorkerContracts(state, enterprise);
            state.appendLog(
                    "LABOR_CONTRACT_RESET",
                    enterprise.getId() + " received a new worker; all labor contracts on this enterprise were reset."
            );
        }

        return events;
    }

    private List<DomainEvent> applyPlaceStrikes(GameState state, PlaceStrikesCommand command) {
        for (String enterpriseId : command.enterpriseIds()) {
            Enterprise enterprise = state.findEnterprise(enterpriseId).orElseThrow();
            enterprise.setStrikeToken(true);
            state.appendLog("STRIKE_PLACED", command.actorPlayerId() + " placed a strike token on " + enterprise.getId() + ".");
        }
        if (command.enterpriseIds().stream()
                .map(state::findEnterprise)
                .flatMap(Optional::stream)
                .anyMatch(enterprise -> enterprise.getOwnerClass() == ClassType.CAPITALIST)) {
            state.appendLog("AUTOMA_STRIKE_RESPONSE_PENDING",
                    "Capitalist automa strike-response instruction is not modeled yet; legal future moves remain engine-generated.");
        }
        return List.of();
    }

    private List<DomainEvent> applyPlaceDemonstration(GameState state, PlaceDemonstrationCommand command) {
        state.setDemonstrationToken(true);
        state.setDemonstrationPenaltyAllocation(command.penaltyAllocation());
        state.appendLog("DEMONSTRATION_PLACED", command.actorPlayerId() + " placed a demonstration token on the unemployed area.");
        return List.of();
    }

    private List<DomainEvent> applyBuyGoodsAndServices(GameState state, BuyGoodsAndServicesCommand command) {
        PlayerState buyer = state.findPlayerById(command.actorPlayerId()).orElseThrow();
        ResourceType resourceType = ResourceType.fromRaw(command.resourceType());
        if (resourceType == null) {
            return List.of();
        }

        PurchaseEvaluation evaluation = consumerEconomyService.evaluatePurchasePlan(state, buyer, resourceType, command.purchases(), false);
        if (!evaluation.errors().isEmpty()) {
            throw new IllegalStateException("Purchase plan became invalid after validation: " + evaluation.errors());
        }

        consumerEconomyService.applyPurchase(state, buyer, resourceType, evaluation);
        return List.of();
    }

    private List<DomainEvent> applyConsumeCommand(GameState state, String actorPlayerId, ResourceType resourceType) {
        return applyConsumeCommand(state, actorPlayerId, resourceType, "", WorkerSlotColor.WHITE);
    }

    private List<DomainEvent> applyConsumeCommand(
            GameState state,
            String actorPlayerId,
            ResourceType resourceType,
            String workerId,
            WorkerSlotColor targetColor
    ) {
        PlayerState actor = state.findPlayerById(actorPlayerId).orElseThrow();
        consumerEconomyService.applyConsumption(state, actor, resourceType);
        if (resourceType == ResourceType.HEALTHCARE) {
            actor.setVictoryPoints(actor.getVictoryPoints() + 2);
            Worker worker = new Worker();
            worker.setId(nextWorkerIdForClass(state, actor.getClassType()));
            worker.setClassType(actor.getClassType());
            worker.setQualificationType(WorkerQualification.UNSKILLED);
            worker.setSector(WorkerSector.GENERAL);
            worker.setLocation(WorkerLocation.UNEMPLOYED);
            worker.setTiedContract(false);
            state.getWorkers().add(worker);
            actor.setPopulation(actor.getPopulation() + 1);
            state.appendLog("HEALTHCARE_CONSUMED", actor.getPlayerId() + " gained +2 VP and new unskilled worker " + worker.getId() + ".");
        }
        if (resourceType == ResourceType.EDUCATION) {
            Worker trained = findSelectedTrainableWorker(state, actor.getClassType(), workerId);
            if (trained != null) {
                trained.setQualificationType(WorkerQualification.SKILLED);
                WorkerSector targetSector = targetColor == null ? WorkerSector.WHITE : targetColor.toWorkerSector();
                trained.setSector(targetSector == null ? WorkerSector.WHITE : targetSector);
                state.appendLog("EDUCATION_CONSUMED", actor.getPlayerId() + " trained worker " + trained.getId() + " into SKILLED/" + trained.getSector() + ".");
            }
        }
        return List.of();
    }

    private List<DomainEvent> applyDeclareVoteStance(GameState state, DeclareVoteStanceCommand command) {
        List<DomainEvent> events = new ArrayList<>();
        CurrentVoteState session = state.getCurrentVoteState();
        if (session == null || session.getActiveProposalPolicyId() == null) {
            state.appendLog("VOTE_STANCE_SKIPPED", "Stance command ignored because no active voting session exists.");
            return events;
        }
        VoteStance stance = parseStance(command.stance());
        if (stance == null) {
            state.appendLog("VOTE_STANCE_SKIPPED", "Stance command ignored because stance is invalid: " + command.stance());
            return events;
        }
        session.getStanceByPlayer().put(command.actorPlayerId(), stance);
        events.add(new VoteStanceDeclaredEvent(command.actorPlayerId(), command.policyId(), stance));

        if (allStancesSubmitted(state, session)) {
            session.setVotingStage(VotingStage.DRAW_BAG_CUBES);
            drawAndInterpretCubes(state, session);
            events.add(new VotingCubesDrawnEvent(session.getActiveProposalPolicyId(), session.getDrawnVotingCubes().size()));
            session.setVotingStage(VotingStage.COMMIT_INFLUENCE);
            appendPreliminaryVoteResultLog(state, session);
            autoCommitAutomaInfluence(state, session);
            if (allInfluenceCommitted(state, session)) {
                events.addAll(resolveCurrentVoteAndAdvance(state, session));
            }
        }
        return events;
    }

    private List<DomainEvent> applyCommitVoteInfluence(GameState state, CommitVoteInfluenceCommand command) {
        List<DomainEvent> events = new ArrayList<>();
        CurrentVoteState session = state.getCurrentVoteState();
        if (session == null || session.getActiveProposalPolicyId() == null) {
            state.appendLog("VOTE_INFLUENCE_SKIPPED", "Influence command ignored because no active voting session exists.");
            return events;
        }

        session.getInfluenceCommitments().put(command.actorPlayerId(), command.influenceAmount());
        events.add(new VoteInfluenceCommittedEvent(command.actorPlayerId(), session.getActiveProposalPolicyId(), command.influenceAmount()));

        if (allInfluenceCommitted(state, session)) {
            events.addAll(resolveCurrentVoteAndAdvance(state, session));
        }
        return events;
    }

    private List<DomainEvent> applyResolveProductionPhase(GameState state, ResolveProductionPhaseCommand command) {
        List<DomainEvent> events = new ArrayList<>();
        ProductionPhaseState production = state.getProductionPhaseState();
        if (production == null) {
            production = new ProductionPhaseState();
            state.setProductionPhaseState(production);
        }

        production.setStage(ProductionSubPhase.PRODUCE_GOODS_AND_SERVICES);
        productionResolver.resolveGoodsAndServices(state, production);

        production.setStage(ProductionSubPhase.ROUND_CLOSE);
        production.setProductionResolved(true);
        production.setRoundAdvanceReady(true);
        state.setLastProductionSummary(production.copy());

        state.appendLog("PRODUCTION_RESOLVED", "Production phase resolved in one pass: wages paid and goods produced.");
        return events;
    }

    private List<DomainEvent> applyResolveScoringPhase(GameState state, ResolveScoringPhaseCommand command) {
        int round = state.getCurrentRound();

        ScoringSummary summary = new ScoringSummary();
        summary.setRound(round);
        summary.setResolved(true);

        List<String> unsupported = List.of(
                "UNSUPPORTED_SCORING_SOURCE: union scoring is unavailable because union subsystem is not modeled.",
                "UNSUPPORTED_SCORING_SOURCE: state agenda/event scoring is unavailable in current slice.",
                "UNSUPPORTED_SCORING_SOURCE: middle class welfare/enterprise final scoring is unavailable in current slice."
        );
        summary.setUnsupportedSources(unsupported);
        summary.setNotes(List.of("Scoring applies supported per-round VP sources and reports missing subsystems."));

        List<PlayerScoringBreakdown> byPlayer = new ArrayList<>();
        for (PlayerState player : state.getPlayers()) {
            PlayerScoringBreakdown row = new PlayerScoringBreakdown();
            row.setPlayerId(player.getPlayerId());
            row.setClassType(player.getClassType());
            row.setAccumulatedBeforePhase(player.getVictoryPoints());
            List<ScoringSourceEntry> sources = new ArrayList<>();
            int gained = 0;
            if (player.getClassType() == ClassType.CAPITALIST) {
                gained += applyCapitalistWealthScoring(player, sources);
            } else {
                sources.add(new ScoringSourceEntry(
                        "persisted_vp_from_supported_actions",
                        0,
                        true,
                        "No extra VP added for this class in the current per-round scoring slice."
                ));
            }
            player.setVictoryPoints(player.getVictoryPoints() + gained);
            row.setGainedThisPhase(gained);
            row.setTotalAfterPhase(player.getVictoryPoints());
            row.setSources(sources);
            row.setUnsupportedSources(unsupported);
            byPlayer.add(row);
        }

        summary.setPlayers(byPlayer);
        state.setScoringBreakdown(byPlayer);
        state.setLastScoringSummary(summary);
        mergeRoundSummaryWithScoring(state, summary);
        mergeLifecycleUnsupportedNotes(state, unsupported);
        state.appendLog("SCORING_RESOLVED", "Scoring resolved for round " + round + ".");

        if (round >= state.getMaxRounds()) {
            finalizeGame(state, summary);
        }
        return List.of();
    }

    private int applyCapitalistWealthScoring(PlayerState capitalist, List<ScoringSourceEntry> sources) {
        int revenueMoved = Math.max(0, capitalist.getRevenue());
        if (revenueMoved > 0) {
            capitalist.setCapital(Math.max(0, capitalist.getCapital()) + revenueMoved);
            capitalist.setRevenue(0);
        }

        int wealthLevel = wealthTrackLevelForCapital(capitalist.getCapital());
        int baseVp = wealthLevel;
        int previousBest = Math.max(0, capitalist.getWealthTrackLevel());
        int growthSteps = Math.max(0, wealthLevel - previousBest);
        int growthVp = growthSteps * 3;
        capitalist.setWealthTrackLevel(Math.max(previousBest, wealthLevel));

        sources.add(new ScoringSourceEntry(
                "capitalist_wealth_track_base",
                baseVp,
                true,
                "Capital " + capitalist.getCapital() + " gives " + baseVp + " VP on the wealth track."
        ));
        if (growthVp > 0) {
            sources.add(new ScoringSourceEntry(
                    "capitalist_wealth_track_growth",
                    growthVp,
                    true,
                    "Wealth marker advanced " + growthSteps + " step(s) beyond previous maximum."
            ));
        }
        if (revenueMoved > 0) {
            sources.add(new ScoringSourceEntry(
                    "capitalist_revenue_to_capital",
                    0,
                    true,
                    "Moved " + revenueMoved + " money from revenue to capital before wealth scoring."
            ));
        }
        return baseVp + growthVp;
    }

    private int wealthTrackLevelForCapital(int capital) {
        int safeCapital = Math.max(0, capital);
        int[] thresholds = {10, 25, 50, 75, 100, 125, 150, 175, 200, 250, 300, 350, 400, 450, 500};
        int level = 0;
        for (int threshold : thresholds) {
            if (safeCapital >= threshold) {
                level++;
            }
        }
        return level;
    }

    private List<DomainEvent> applyAdvanceToNextRound(GameState state, AdvanceToNextRoundCommand command) {
        int nextRound = state.getCurrentRound() + 1;
        state.setGameStatus(GameStatus.IN_PROGRESS);
        state.setCurrentRound(nextRound);
        state.getTurnOrder().setRound(nextRound);
        state.setRoundMarker(nextRound);
        state.getTurnOrder().setCurrentPlayerIndex(0);
        state.setCurrentVoteState(null);
        state.setProductionPhaseState(null);

        if (nextRound == 1) {
            PreparationSummary skipped = new PreparationSummary();
            skipped.setRound(nextRound);
            skipped.setSkipped(true);
            skipped.setResolved(true);
            skipped.setNotes(List.of("Round 1 preparation is skipped by rules."));
            state.setLastPreparationSummary(skipped);
            mergeRoundSummaryWithPreparation(state, skipped);
            state.getTurnOrder().setPhase(RoundPhase.ACTIONS);
            state.setCurrentPhase(RoundPhase.ACTIONS);
        } else {
            state.getTurnOrder().setPhase(RoundPhase.PREPARATION);
            state.setCurrentPhase(RoundPhase.PREPARATION);
        }

        state.appendLog("ROUND_ADVANCED", "Advanced to round " + nextRound + " (" + state.getCurrentPhase() + ").");
        return List.of();
    }

    private List<DomainEvent> applyResolvePreparationPhase(GameState state, ResolvePreparationPhaseCommand command) {
        state.setGameStatus(GameStatus.IN_PROGRESS);
        if (state.getCurrentRound() == 1) {
            PreparationSummary skipped = new PreparationSummary();
            skipped.setRound(1);
            skipped.setResolved(true);
            skipped.setSkipped(true);
            skipped.setNotes(List.of("Round 1 preparation is skipped by rules."));
            state.setLastPreparationSummary(skipped);
            mergeRoundSummaryWithPreparation(state, skipped);
            resetActionsPhaseTracker(state);
            state.getTurnOrder().setPhase(RoundPhase.ACTIONS);
            state.setCurrentPhase(RoundPhase.ACTIONS);
            state.getTurnOrder().setCurrentPlayerIndex(0);
            state.appendLog("PREPARATION_SKIPPED", "Round 1 preparation skipped by rules.");
            return List.of();
        }
        PreparationSummary summary = new PreparationSummary();
        summary.setRound(state.getCurrentRound());
        summary.setResolved(true);
        summary.setSkipped(false);
        int stateLoanInterestPaid = payStateLoanInterest(state);
        summary.setExecutedSubsteps(List.of(
                "round_marker_confirmed",
                "state_loan_interest_paid",
                "business_deals_refreshed",
                "export_card_refreshed",
                "worker_welfare_decreased",
                "worker_base_workers_added",
                "migration_resolved"
        ));

        List<String> visibleDeals = businessDealDeckManager.refreshVisibleDealsForPreparation(state, state.getCurrentRound()).stream()
                .map(card -> card.getId() + "(pay=" + card.getPayout() + ")")
                .toList();
        var exportCard = exportCardManager.refreshForRound(state, state.getCurrentRound());
        int welfareBefore = decreaseWorkerWelfareForPreparation(state);
        List<String> baseWorkers = addWorkerBasePreparationWorkers(state);
        randomizeMigrationDeckOrder(state, "ROUND_PREPARATION");
        var migrationResolution = migrationCardManager.resolveForPreparation(state);
        List<String> migrationNotes = new ArrayList<>();
        if (migrationResolution.drawnCardIdsByClass().isEmpty()) {
            migrationNotes.add("Migration resolved: no additional workers under current immigration policy.");
        } else {
            migrationResolution.drawnCardIdsByClass().forEach((classType, cardIds) -> {
                List<String> arrivals = migrationResolution.arrivalsByClass().getOrDefault(classType, List.of());
                migrationNotes.add(classType.name() + " migration cards: " + String.join(", ", cardIds) + " -> " + String.join(", ", arrivals));
            });
        }

        List<String> unsupported = List.of(
                "UNSUPPORTED_PREPARATION_SUBSTEP: player loan interest is unavailable because per-player loans are not modeled.",
                "UNSUPPORTED_PREPARATION_SUBSTEP: card refill requires deck/hand refill subsystem.",
                "UNSUPPORTED_PREPARATION_SUBSTEP: event/agenda refresh requires corresponding deck subsystems."
        );
        summary.setUnsupportedSubsteps(unsupported);
        List<String> notes = new ArrayList<>();
        notes.add("Preparation resolves only supported MVP substeps and explicitly reports missing subsystems.");
        if (stateLoanInterestPaid > 0) {
            notes.add("State loan interest paid from treasury: " + stateLoanInterestPaid + ".");
        }
        notes.add("Business deals refreshed: " + (visibleDeals.isEmpty() ? "none visible under current foreign trade policy" : String.join(", ", visibleDeals)));
        notes.add("Export card refreshed: " + exportCard.getCardId() + " (" + exportCard.getAvailableOperations() + " operations).");
        notes.add("Worker welfare decreased from " + welfareBefore + " to "
                + findPlayerByClass(state, ClassType.WORKER).map(PlayerState::getWelfare).orElse(0) + ".");
        notes.add("Worker class received 2 base unskilled workers: " + String.join(", ", baseWorkers) + ".");
        notes.addAll(migrationNotes);
        notes.add("Migration added " + migrationResolution.totalAddedPopulation() + " workers; population was recalculated from worker count.");
        summary.setNotes(notes);
        state.setLastPreparationSummary(summary);
        mergeRoundSummaryWithPreparation(state, summary);
        mergeLifecycleUnsupportedNotes(state, unsupported);

        resetActionsPhaseTracker(state);
        state.getTurnOrder().setPhase(RoundPhase.ACTIONS);
        state.setCurrentPhase(RoundPhase.ACTIONS);
        state.getTurnOrder().setCurrentPlayerIndex(0);
        state.appendLog("PREPARATION_RESOLVED", "Preparation resolved for round " + state.getCurrentRound() + ".");
        List<DomainEvent> events = new ArrayList<>();
        events.add(new BusinessDealsRefreshedEvent("ROUND_PREPARATION", state.getCurrentRound(), visibleDeals));
        events.add(new ExportCardRefreshedEvent(exportCard.getCardId(), state.getCurrentRound(), exportCard.getAvailableOperations()));
        events.add(new MigrationResolvedEvent(state.getCurrentRound(), migrationNotes));
        return events;
    }

    private List<DomainEvent> applyRefreshBusinessDeals(GameState state, RefreshBusinessDealsCommand command) {
        return List.of();
    }

    private int decreaseWorkerWelfareForPreparation(GameState state) {
        PlayerState worker = findPlayerByClass(state, ClassType.WORKER).orElse(null);
        if (worker == null) {
            return 0;
        }
        int before = Math.max(0, worker.getWelfare());
        worker.setWelfare(Math.max(0, before - 1));
        worker.setLastWelfareDelta(worker.getWelfare() - before);
        state.appendLog("WORKER_WELFARE_DECREASED", "Worker welfare decreased from " + before + " to " + worker.getWelfare() + " during preparation.");
        return before;
    }

    private List<String> addWorkerBasePreparationWorkers(GameState state) {
        PlayerState workerPlayer = findPlayerByClass(state, ClassType.WORKER).orElse(null);
        if (workerPlayer == null) {
            return List.of();
        }
        List<String> added = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Worker worker = createUnskilledUnemployedWorker(state, ClassType.WORKER);
            state.getWorkers().add(worker);
            added.add(worker.getId());
        }
        workerPlayer.setPopulation(populationForClass(state, ClassType.WORKER));
        state.refreshLegacyPlayerSnapshots();
        state.appendLog("WORKER_BASE_WORKERS_ADDED", "Worker class received 2 base unskilled workers during preparation: " + String.join(", ", added) + ".");
        return added;
    }

    private List<DomainEvent> applyAdvanceRound(GameState state, AdvanceRoundCommand command) {
        return applyAdvanceToNextRound(state, new AdvanceToNextRoundCommand(command.actorPlayerId()));
    }

    private void applyFinalScoring(GameState state, List<PlayerScoringBreakdown> rows) {
        for (PlayerState player : state.getPlayers()) {
            List<ScoringSourceEntry> sources = new ArrayList<>();
            int gained = switch (player.getClassType()) {
                case WORKER -> applyWorkerFinalScoring(state, player, sources);
                case CAPITALIST -> applyCapitalistFinalScoring(state, player, sources);
                default -> 0;
            };
            if (gained <= 0 && sources.isEmpty()) {
                continue;
            }

            player.setVictoryPoints(player.getVictoryPoints() + gained);
            PlayerScoringBreakdown row = scoringRowFor(rows, player);
            row.setGainedThisPhase(row.getGainedThisPhase() + gained);
            row.setTotalAfterPhase(player.getVictoryPoints());
            List<ScoringSourceEntry> mergedSources = new ArrayList<>(row.getSources());
            mergedSources.addAll(sources);
            row.setSources(mergedSources);
        }
    }

    private int applyWorkerFinalScoring(GameState state, PlayerState worker, List<ScoringSourceEntry> sources) {
        int socialistPolicies = finalPolicyTiebreakScore(state, ClassType.WORKER);
        int policyVp = finalPolicyVpForCount(socialistPolicies);
        int cashVp = Math.min(15, Math.max(0, worker.getMoney()) / 10);
        sources.add(new ScoringSourceEntry(
                "worker_final_socialist_policies",
                policyVp,
                true,
                socialistPolicies + " policy/policies 1-5 on course A."
        ));
        sources.add(new ScoringSourceEntry(
                "worker_final_cash",
                cashVp,
                true,
                "Remaining money " + worker.getMoney() + " gives 1 VP per 10, max 15."
        ));
        state.appendLog(
                "FINAL_WORKER_SCORING",
                worker.getPlayerId() + " gained " + (policyVp + cashVp) + " final VP: policies " + policyVp + ", cash " + cashVp + "."
        );
        return policyVp + cashVp;
    }

    private int applyCapitalistFinalScoring(GameState state, PlayerState capitalist, List<ScoringSourceEntry> sources) {
        int neoliberalPolicies = finalPolicyTiebreakScore(state, ClassType.CAPITALIST);
        int policyVp = finalPolicyVpForCount(neoliberalPolicies);
        int resourceVp = capitalistFinalResourceVp(capitalist);
        sources.add(new ScoringSourceEntry(
                "capitalist_final_neoliberal_policies",
                policyVp,
                true,
                neoliberalPolicies + " policy/policies 1-5 on course C."
        ));
        sources.add(new ScoringSourceEntry(
                "capitalist_final_resources",
                resourceVp,
                true,
                "Stored food scores 1 VP per 2; luxury, healthcare and education score 1 VP per 3."
        ));
        state.appendLog(
                "FINAL_CAPITALIST_SCORING",
                capitalist.getPlayerId() + " gained " + (policyVp + resourceVp) + " final VP: policies " + policyVp + ", resources " + resourceVp + "."
        );
        return policyVp + resourceVp;
    }

    private PlayerScoringBreakdown scoringRowFor(List<PlayerScoringBreakdown> rows, PlayerState player) {
        for (PlayerScoringBreakdown row : rows) {
            if (Objects.equals(row.getPlayerId(), player.getPlayerId())) {
                return row;
            }
        }
        PlayerScoringBreakdown row = new PlayerScoringBreakdown();
        row.setPlayerId(player.getPlayerId());
        row.setClassType(player.getClassType());
        row.setAccumulatedBeforePhase(player.getVictoryPoints());
        row.setTotalAfterPhase(player.getVictoryPoints());
        rows.add(row);
        return row;
    }

    private int capitalistFinalResourceVp(PlayerState capitalist) {
        Map<String, Integer> totals = new HashMap<>();
        for (Map.Entry<String, Integer> entry : capitalist.getProducedResourceStorage().entrySet()) {
            String key = normalizeFinalResourceKey(entry.getKey());
            totals.merge(key, Math.max(0, entry.getValue()), Integer::sum);
        }
        int foodVp = totals.getOrDefault(ResourceType.FOOD.id(), 0) / 2;
        int luxuryVp = totals.getOrDefault(ResourceType.LUXURY.id(), 0) / 3;
        int healthcareVp = totals.getOrDefault(ResourceType.HEALTHCARE.id(), 0) / 3;
        int educationVp = totals.getOrDefault(ResourceType.EDUCATION.id(), 0) / 3;
        return foodVp + luxuryVp + healthcareVp + educationVp;
    }

    private String normalizeFinalResourceKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String normalized = raw.toLowerCase();
        if (normalized.startsWith("ftz_")) {
            normalized = normalized.substring(4);
        }
        ResourceType type = ResourceType.fromRaw(normalized);
        return type == null ? normalized : type.id();
    }

    private int finalPolicyVpForCount(int count) {
        return switch (Math.max(0, count)) {
            case 0 -> 0;
            case 1 -> 4;
            case 2 -> 8;
            case 3 -> 12;
            case 4 -> 18;
            default -> 25;
        };
    }

    private int finalPolicyTiebreakScore(GameState state, ClassType classType) {
        PolicyCourse preferred = switch (classType) {
            case WORKER -> PolicyCourse.A;
            case CAPITALIST -> PolicyCourse.C;
            default -> PolicyCourse.B;
        };
        int count = 0;
        for (PolicyId policyId : List.of(
                PolicyId.POLICY_1_FISCAL,
                PolicyId.POLICY_2_LABOR_MARKET,
                PolicyId.POLICY_3_TAXATION,
                PolicyId.POLICY_4_HEALTHCARE_AND_BENEFITS,
                PolicyId.POLICY_5_EDUCATION
        )) {
            if (state.findPolicy(policyId).map(PolicyState::getCurrentCourse).orElse(PolicyCourse.B) == preferred) {
                count++;
            }
        }
        return count;
    }

    private int votingCubeTiebreakScore(GameState state, ClassType classType) {
        return votingBagRules.countForOwner(state, votingBagRules.ownerForClass(classType));
    }

    private List<String> resolveFinalWinnersByTiebreaks(GameState state, List<FinalStanding> tiedOnVp) {
        if (tiedOnVp.size() <= 1) {
            return tiedOnVp.stream().map(FinalStanding::getPlayerId).toList();
        }
        int bestPolicyScore = tiedOnVp.stream()
                .mapToInt(standing -> finalPolicyTiebreakScore(state, standing.getClassType()))
                .max()
                .orElse(0);
        List<FinalStanding> tiedOnPolicies = tiedOnVp.stream()
                .filter(standing -> finalPolicyTiebreakScore(state, standing.getClassType()) == bestPolicyScore)
                .toList();
        if (tiedOnPolicies.size() <= 1) {
            return tiedOnPolicies.stream().map(FinalStanding::getPlayerId).toList();
        }

        int bestCubeScore = tiedOnPolicies.stream()
                .mapToInt(standing -> votingCubeTiebreakScore(state, standing.getClassType()))
                .max()
                .orElse(0);
        return tiedOnPolicies.stream()
                .filter(standing -> votingCubeTiebreakScore(state, standing.getClassType()) == bestCubeScore)
                .map(FinalStanding::getPlayerId)
                .toList();
    }

    private void finalizeGame(GameState state, ScoringSummary summary) {
        List<PlayerScoringBreakdown> rows = new ArrayList<>(summary.getPlayers().stream()
                .map(PlayerScoringBreakdown::copy)
                .toList());
        applyFinalScoring(state, rows);

        List<PlayerState> sorted = new ArrayList<>(state.getPlayers());
        sorted.sort(Comparator
                .comparingInt(PlayerState::getVictoryPoints).reversed()
                .thenComparing((PlayerState player) -> finalPolicyTiebreakScore(state, player.getClassType()), Comparator.reverseOrder())
                .thenComparing((PlayerState player) -> votingCubeTiebreakScore(state, player.getClassType()), Comparator.reverseOrder())
                .thenComparing(PlayerState::getPlayerId));

        List<FinalStanding> standings = new ArrayList<>();
        int rank = 0;
        int prevVp = Integer.MIN_VALUE;
        int prevPolicyScore = Integer.MIN_VALUE;
        int prevCubeScore = Integer.MIN_VALUE;
        for (int i = 0; i < sorted.size(); i++) {
            PlayerState player = sorted.get(i);
            int policyScore = finalPolicyTiebreakScore(state, player.getClassType());
            int cubeScore = votingCubeTiebreakScore(state, player.getClassType());
            if (player.getVictoryPoints() != prevVp || policyScore != prevPolicyScore || cubeScore != prevCubeScore) {
                rank = i + 1;
                prevVp = player.getVictoryPoints();
                prevPolicyScore = policyScore;
                prevCubeScore = cubeScore;
            }
            standings.add(new FinalStanding(player.getPlayerId(), player.getClassType(), player.getVictoryPoints(), rank));
        }

        int topVp = standings.isEmpty() ? 0 : standings.get(0).getTotalVp();
        List<FinalStanding> tiedOnVp = standings.stream()
                .filter(s -> s.getTotalVp() == topVp)
                .toList();
        List<String> winners = resolveFinalWinnersByTiebreaks(state, tiedOnVp);

        FinalResult finalResult = new FinalResult();
        finalResult.setCompletedRound(state.getCurrentRound());
        finalResult.setStandings(standings);
        finalResult.setScoringBreakdown(rows);
        finalResult.setWinnerPlayerIds(winners);
        finalResult.setTie(winners.size() > 1);
        finalResult.setTiebreakApplied(tiedOnVp.size() > 1 && winners.size() == 1);
        finalResult.setUnresolvedTie(winners.size() > 1);
        summary.setPlayers(rows);
        state.setScoringBreakdown(rows);
        if (winners.size() > 1) {
            finalResult.setUnsupportedNotes(List.of("Final tiebreak remained unresolved after policy-interest and voting-bag cube checks."));
        } else {
            finalResult.setUnsupportedNotes(List.of());
        }

        state.setFinalResult(finalResult);
        state.setGameStatus(GameStatus.FINISHED);
        state.getTurnOrder().setPhase(RoundPhase.GAME_OVER);
        state.setCurrentPhase(RoundPhase.GAME_OVER);
        state.appendLog("GAME_OVER", "Game finished after round " + state.getCurrentRound() + ".");
    }

    private GameCommand nextFlowCommand(GameState state, String actorPlayerId) {
        if (state.isGameOver()) {
            return null;
        }

        return switch (state.getTurnOrder().getPhase()) {
            case PREPARATION -> new ResolvePreparationPhaseCommand(actorPlayerId);
            case ACTIONS -> {
                if (state.getCurrentVoteState() != null) {
                    yield nextVotingFlowCommand(state);
                }
                yield allActionTurnsCompleted(state)
                        ? new AdvanceToProductionCommand(actorPlayerId)
                        : new EndTurnCommand();
            }
            case VOTING -> nextVotingFlowCommand(state);
            case PRODUCTION -> {
                ProductionPhaseState production = state.getProductionPhaseState();
                if (production == null || !production.isProductionResolved()) {
                    yield new ResolveProductionPhaseCommand(actorPlayerId);
                }
                if (production.isRoundAdvanceReady()) {
                    yield new AdvanceToVotingCommand(actorPlayerId);
                }
                yield null;
            }
            case SCORING -> {
                if (!isScoringResolvedForCurrentRound(state)) {
                    yield new ResolveScoringPhaseCommand(actorPlayerId);
                }
                if (state.getCurrentRound() < state.getMaxRounds()) {
                    yield new AdvanceToNextRoundCommand(actorPlayerId);
                }
                yield null;
            }
            case GAME_OVER -> null;
        };
    }

    private GameCommand nextVotingFlowCommand(GameState state) {
        CurrentVoteState session = state.getCurrentVoteState();
        if (session == null) {
            PlayerState actor = state.currentPlayer();
            return actor == null ? null : new AdvanceToScoringCommand(actor.getPlayerId());
        }

        if (session.getVotingStage() == VotingStage.DECLARE_STANCES) {
            for (PlayerState player : activePlayers(state)) {
                if (session.getStanceByPlayer().containsKey(player.getPlayerId())) {
                    continue;
                }
                String stance = stanceForVotingFlow(state, session, player).name();
                return new DeclareVoteStanceCommand(player.getPlayerId(), session.getActiveProposalPolicyId(), stance);
            }
            return null;
        }

        if (session.getVotingStage() == VotingStage.COMMIT_INFLUENCE) {
            for (PlayerState player : activePlayers(state)) {
                if (session.getInfluenceCommitments().containsKey(player.getPlayerId())) {
                    continue;
                }
                int influence = isAutomaControlled(player)
                        ? automaInfluenceCommitment(state, session, player)
                        : 0;
                return new CommitVoteInfluenceCommand(player.getPlayerId(), influence);
            }
        }
        return null;
    }

    private boolean isScoringResolvedForCurrentRound(GameState state) {
        ScoringSummary summary = state.getLastScoringSummary();
        return summary != null && summary.isResolved() && summary.getRound() == state.getCurrentRound();
    }

    private boolean isPreparationResolvedForCurrentRound(GameState state) {
        PreparationSummary summary = state.getLastPreparationSummary();
        return summary != null && summary.isResolved() && summary.getRound() == state.getCurrentRound();
    }

    private void mergeRoundSummaryWithPreparation(GameState state, PreparationSummary preparationSummary) {
        RoundSummary summary = state.getLastRoundSummary();
        if (summary == null || summary.getRound() != state.getCurrentRound()) {
            summary = new RoundSummary();
            summary.setRound(state.getCurrentRound());
        }
        summary.setPreparationSummary(preparationSummary == null ? null : preparationSummary.copy());
        state.setLastRoundSummary(summary);
    }

    private void mergeRoundSummaryWithScoring(GameState state, ScoringSummary scoringSummary) {
        RoundSummary summary = state.getLastRoundSummary();
        if (summary == null || summary.getRound() != state.getCurrentRound()) {
            summary = new RoundSummary();
            summary.setRound(state.getCurrentRound());
        }
        summary.setScoringSummary(scoringSummary == null ? null : scoringSummary.copy());
        state.setLastRoundSummary(summary);
    }

    private void mergeLifecycleUnsupportedNotes(GameState state, List<String> notes) {
        if (notes == null || notes.isEmpty()) {
            return;
        }
        List<String> merged = state.getLifecycleUnsupportedNotes() == null
                ? new ArrayList<>()
                : new ArrayList<>(state.getLifecycleUnsupportedNotes());
        for (String note : notes) {
            if (note == null || note.isBlank() || merged.contains(note)) {
                continue;
            }
            merged.add(note);
        }
        state.setLifecycleUnsupportedNotes(merged);
    }

    private List<DomainEvent> resolveCurrentVoteAndAdvance(GameState state, CurrentVoteState session) {
        PolicyId resolvedPolicyId = session.getActiveProposalPolicyId();
        List<DomainEvent> events = new ArrayList<>(votingResolutionService.resolveOrdinaryVote(state, session));
        if (resolvedPolicyId == PolicyId.POLICY_2_LABOR_MARKET && state.getLastProposalResolution() != null
                && state.getLastProposalResolution().getResult() == VoteResolutionResult.PASSED) {
            enforceStateEnterpriseMinimumWages(state);
        }
        if (resolvedPolicyId == PolicyId.POLICY_1_FISCAL && state.getLastProposalResolution() != null
                && state.getLastProposalResolution().getResult() == VoteResolutionResult.PASSED) {
            ensureStateEnterpriseRowsForFiscalPolicy(state);
        }
        if (session.isExtraordinary()) {
            state.getTurnOrder().setPhase(RoundPhase.ACTIONS);
            state.setCurrentPhase(RoundPhase.ACTIONS);
            state.appendLog("EXTRAORDINARY_VOTE_FINISHED", "Extraordinary vote finished; action phase continues.");
            return events;
        }
        Optional<PolicyState> nextPolicy = state.pendingPoliciesInOrder().stream()
                .filter(policy -> policy.getId().ordinal() > resolvedPolicyId.ordinal())
                .findFirst();
        if (nextPolicy.isEmpty()) {
            applyAdvanceToScoring(state, new AdvanceToScoringCommand(session.getProposalAuthorPlayerId()));
        } else {
            startVoteSessionForPolicy(state, nextPolicy.get());
        }
        return events;
    }

    private int payStateLoanInterest(GameState state) {
        int loans = Math.max(0, state.getStateLoans());
        if (loans <= 0) {
            return 0;
        }
        int interest = loans * STATE_LOAN_INTEREST;
        ensureTreasuryCanPay(state, interest, "state loan interest");
        state.setTreasury(Math.max(0, state.getTreasury() - interest));
        state.appendLog("STATE_LOAN_INTEREST_PAID", "State paid " + interest + " interest for " + loans + " loan(s).");
        return interest;
    }

    private void ensureTreasuryCanPay(GameState state, int amount, String reason) {
        while (state.getTreasury() < amount) {
            state.setTreasury(state.getTreasury() + STATE_LOAN_AMOUNT);
            state.setStateLoans(state.getStateLoans() + 1);
            state.appendLog(
                    "STATE_LOAN_TAKEN",
                    "State took a 50 loan for " + reason + "; active state loans: " + state.getStateLoans() + "."
            );
        }
    }

    private void enforceStateEnterpriseMinimumWages(GameState state) {
        int minimum = minimumStateWageLevel(state);
        for (Enterprise enterprise : state.getEnterprises()) {
            if (enterprise.getOwnerClass() != ClassType.STATE || enterprise.getWageLevel() >= minimum) {
                continue;
            }
            int before = enterprise.getWageLevel();
            enterprise.setWageLevel(minimum);
            enterprise.setWageTrack(Map.of("low", 15, "medium", 20, "high", 25));
            state.appendLog(
                    "STATE_WAGE_ADJUSTED",
                    enterprise.getId() + " wage level adjusted from L" + before + " to L" + minimum + " after labor policy changed."
            );
        }
    }

    private int minimumStateWageLevel(GameState state) {
        PolicyCourse labor = state.findPolicy(PolicyId.POLICY_2_LABOR_MARKET)
                .map(PolicyState::getCurrentCourse)
                .orElse(PolicyCourse.B);
        return switch (labor) {
            case A -> 3;
            case B -> 2;
            case C -> 1;
        };
    }

    private void ensureStateEnterpriseRowsForFiscalPolicy(GameState state) {
        int desiredRows = state.findPolicy(PolicyId.POLICY_1_FISCAL)
                .map(PolicyState::getCurrentCourse)
                .map(this::stateEnterpriseRowsForFiscalCourse)
                .orElse(1);
        for (String baseId : List.of("state_hospital", "state_university", "state_media")) {
            Optional<Enterprise> base = state.findEnterprise(baseId);
            if (base.isEmpty()) {
                continue;
            }
            int added = 0;
            for (int row = 2; row <= desiredRows; row++) {
                String cloneId = baseId + "_" + row;
                if (state.findEnterprise(cloneId).isPresent()) {
                    continue;
                }
                state.getEnterprises().add(cloneStateEnterpriseRow(base.get(), cloneId, row));
                added++;
            }
            if (added > 0) {
                state.appendLog(
                        "STATE_ENTERPRISE_ROWS_SYNCED",
                        baseId + " gained " + added + " public sector row(s) after fiscal policy changed."
                );
            }
        }
    }

    private int stateEnterpriseRowsForFiscalCourse(PolicyCourse course) {
        return switch (course) {
            case A -> 3;
            case B -> 2;
            case C -> 1;
        };
    }

    private Enterprise cloneStateEnterpriseRow(Enterprise base, String cloneId, int row) {
        Enterprise clone = base.copy();
        clone.setId(cloneId);
        clone.setName((base.getName() == null || base.getName().isBlank() ? base.getId() : base.getName()) + " " + row);
        clone.setOwnerClass(ClassType.STATE);
        List<EnterpriseSlot> slots = new ArrayList<>();
        List<EnterpriseSlot> baseSlots = base.getSlots() == null ? List.of() : base.getSlots();
        for (int idx = 0; idx < baseSlots.size(); idx++) {
            EnterpriseSlot slot = baseSlots.get(idx).copy();
            slot.setId(cloneId + "-slot-" + (idx + 1));
            slot.setOccupiedWorkerId(null);
            slots.add(slot);
        }
        clone.setSlots(slots);
        return clone;
    }

    private String proposalTokenOwnerPlayerId(ProposalToken token) {
        if (token == null) {
            return null;
        }
        if (token.getOwnerPlayerId() != null && !token.getOwnerPlayerId().isBlank()) {
            return token.getOwnerPlayerId();
        }
        return token.getOwnerClass() == null ? null : token.getOwnerClass().playerId();
    }

    private void enterProductionPhase(GameState state) {
        state.setCurrentVoteState(null);
        state.getTurnOrder().setPhase(RoundPhase.PRODUCTION);
        state.setCurrentPhase(RoundPhase.PRODUCTION);
        state.getTurnOrder().setCurrentPlayerIndex(0);
        ProductionPhaseState phaseState = new ProductionPhaseState();
        phaseState.setStage(ProductionSubPhase.PRODUCE_GOODS_AND_SERVICES);
        state.setProductionPhaseState(phaseState);
        state.setGameStatus(GameStatus.IN_PROGRESS);
    }

    private boolean allActionTurnsCompleted(GameState state) {
        return state != null
                && state.getTurnOrder() != null
                && state.getTurnOrder().allPlayersCompletedActions();
    }

    private void resetActionsPhaseTracker(GameState state) {
        if (state == null || state.getTurnOrder() == null) {
            return;
        }
        state.getTurnOrder().resetActionTracking();
    }

    private void startNextPendingVoteSession(GameState state) {
        List<PolicyState> pending = state.pendingPoliciesInOrder();
        if (pending.isEmpty()) {
            state.setCurrentVoteState(null);
            return;
        }

        startVoteSessionForPolicy(state, pending.get(0));
    }

    private void startVoteSessionForPolicy(GameState state, PolicyState policy) {
        ProposalToken token = policy.getOccupyingProposalToken();
        if (token == null) {
            state.setCurrentVoteState(null);
            return;
        }

        CurrentVoteState session = new CurrentVoteState();
        session.setActiveProposalPolicyId(policy.getId());
        String authorPlayerId = token.getOwnerPlayerId() == null || token.getOwnerPlayerId().isBlank()
                ? token.getOwnerClass().playerId()
                : token.getOwnerPlayerId();
        session.setProposalAuthorPlayerId(authorPlayerId);
        session.setCurrentCourseBeforeVote(policy.getCurrentCourse());
        session.setTargetCourse(token.getTargetCourse());
        session.setVotingStage(VotingStage.DECLARE_STANCES);
        session.setResult(VoteResolutionResult.PENDING);

        session.getStanceByPlayer().put(authorPlayerId, VoteStance.FOR);
        state.setCurrentVoteState(session);
    }

    private void drawAndInterpretCubes(GameState state, CurrentVoteState session) {
        List<DrawnVotingCube> drawn = new ArrayList<>();
        Map<String, Integer> interpreted = new HashMap<>();
        interpreted.put(InterpretedVote.FOR.name(), 0);
        interpreted.put(InterpretedVote.AGAINST.name(), 0);
        interpreted.put(InterpretedVote.NEUTRAL.name(), 0);

        List<VotingCubeOwnerClass> drawnOwners = session.isExtraordinary()
                ? votingBagRules.drawExistingCubes(state, 5)
                : votingBagRules.drawCubes(state, 5);
        for (VotingCubeOwnerClass ownerClass : drawnOwners) {
            InterpretedVote vote = interpretCubeVote(state, session, ownerClass);
            drawn.add(new DrawnVotingCube(ownerClass, vote));
            interpreted.compute(vote.name(), (k, v) -> v == null ? 1 : v + 1);

            String playerId = votingBagRules.playerIdForCubeOwner(state, ownerClass);
            if (vote != InterpretedVote.NEUTRAL && playerId != null) {
                interpreted.compute("PLAYER:" + playerId, (k, v) -> v == null ? 1 : v + 1);
            }
        }

        session.setDrawnVotingCubes(drawn);
        session.setInterpretedVotes(interpreted);
    }

    private void appendPreliminaryVoteResultLog(GameState state, CurrentVoteState session) {
        int preliminaryFor = session.getInterpretedVotes().getOrDefault(InterpretedVote.FOR.name(), 0);
        int preliminaryAgainst = session.getInterpretedVotes().getOrDefault(InterpretedVote.AGAINST.name(), 0);
        String leadingSide = preliminaryFor >= preliminaryAgainst ? "FOR" : "AGAINST";
        state.appendLog(
                "VOTE_PRELIMINARY_RESULT",
                "Preliminary vote result before influence: FOR " + preliminaryFor
                        + " / AGAINST " + preliminaryAgainst + "; leading side " + leadingSide
                        + ". Influence is committed after this result is known."
        );
    }

    private InterpretedVote interpretCubeVote(GameState state, CurrentVoteState session, VotingCubeOwnerClass ownerClass) {
        if (isTwoPlayerMode(state) && ownerClass == VotingCubeOwnerClass.MIDDLE_CLASS) {
            return InterpretedVote.NEUTRAL;
        }

        String playerId = votingBagRules.playerIdForCubeOwner(state, ownerClass);
        if (playerId == null) {
            return InterpretedVote.NEUTRAL;
        }

        VoteStance stance = session.getStanceByPlayer().get(playerId);
        if (stance == null) {
            return InterpretedVote.NEUTRAL;
        }

        return stance == VoteStance.FOR ? InterpretedVote.FOR : InterpretedVote.AGAINST;
    }

    private void applyStateVotingInfluenceGain(GameState state) {
        Optional<PlayerState> statePlayerOpt = findPlayerByClass(state, ClassType.STATE);
        if (statePlayerOpt.isEmpty()) {
            return;
        }
        PlayerState statePlayer = statePlayerOpt.get();
        int gain = Math.min(statePlayer.getLegitimacyWorker(), Math.min(statePlayer.getLegitimacyMiddleClass(), statePlayer.getLegitimacyCapitalist()));
        if (gain > 0) {
            statePlayer.setInfluence(statePlayer.getInfluence() + gain);
            state.appendLog("STATE_INFLUENCE_GAIN", "State gains " + gain + " influence for voting phase.");
        }
    }

    private VoteStance stanceForVotingFlow(GameState state, CurrentVoteState session, PlayerState player) {
        if (isAutomaControlled(player)) {
            return automaStanceFor(state, session, player);
        }
        return Objects.equals(player.getPlayerId(), session.getProposalAuthorPlayerId())
                ? VoteStance.FOR
                : VoteStance.AGAINST;
    }

    private VoteStance automaStanceFor(GameState state, CurrentVoteState session, PlayerState player) {
        if (Objects.equals(player.getPlayerId(), session.getProposalAuthorPlayerId())) {
            return VoteStance.FOR;
        }
        if (player.getClassType() == ClassType.CAPITALIST) {
            VoteStance workerStance = stanceForClass(state, session, ClassType.WORKER);
            if (workerStance != null) {
                return opposite(workerStance);
            }
        }
        PolicyCourse desired = desiredPolicyCourse(player.getClassType(), session.getActiveProposalPolicyId());
        PolicyCourse current = session.getCurrentCourseBeforeVote();
        PolicyCourse target = session.getTargetCourse();
        if (desired == null || current == null || target == null) {
            return VoteStance.AGAINST;
        }
        int currentDistance = Math.abs(current.ordinal() - desired.ordinal());
        int targetDistance = Math.abs(target.ordinal() - desired.ordinal());
        return targetDistance < currentDistance ? VoteStance.FOR : VoteStance.AGAINST;
    }

    private VoteStance stanceForClass(GameState state, CurrentVoteState session, ClassType classType) {
        return activePlayers(state).stream()
                .filter(player -> player.getClassType() == classType)
                .map(player -> session.getStanceByPlayer().get(player.getPlayerId()))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private PolicyCourse desiredPolicyCourse(ClassType classType, PolicyId policyId) {
        if (classType == null || policyId == null) {
            return PolicyCourse.B;
        }
        return switch (classType) {
            case WORKER -> PolicyCourse.A;
            case CAPITALIST -> PolicyCourse.C;
            case MIDDLE_CLASS, STATE -> PolicyCourse.B;
        };
    }

    private int automaInfluenceCommitment(GameState state, CurrentVoteState session, PlayerState player) {
        int available = Math.max(0, player.getInfluence());
        if (available <= 0) {
            return 0;
        }
        VoteStance side = session.getStanceByPlayer().get(player.getPlayerId());
        if (side == null) {
            return 0;
        }
        if (isFinalRoundLastVoting(state)) {
            return available;
        }

        int forVotes = votesForSide(session, VoteStance.FOR);
        int againstVotes = votesForSide(session, VoteStance.AGAINST);
        boolean winning = isSideWinning(side, forVotes, againstVotes);
        int opposingRemaining = remainingInfluenceForSide(state, session, opposite(side));

        if (winning) {
            if (isComplexAutoma(player) && opposingRemaining > 0 && guaranteesWinAfterOpposition(side, forVotes + (side == VoteStance.FOR ? 1 : 0), againstVotes + (side == VoteStance.AGAINST ? 1 : 0), opposingRemaining)) {
                return 1;
            }
            return 0;
        }

        int required = influenceRequiredToWin(side, forVotes, againstVotes);
        int sideRemaining = remainingInfluenceForSide(state, session, side);
        if (required <= 0 || sideRemaining < required) {
            return 0;
        }

        int supportersWithInfluence = supportersWithRemainingInfluence(state, session, side);
        if (supportersWithInfluence <= 1) {
            return Math.min(available, required);
        }
        if (sideRemaining <= opposingRemaining) {
            return 0;
        }
        return Math.min(available, required);
    }

    private void autoCommitAutomaInfluence(GameState state, CurrentVoteState session) {
        for (PlayerState player : activePlayers(state)) {
            if (!isAutomaControlled(player) || session.getInfluenceCommitments().containsKey(player.getPlayerId())) {
                continue;
            }
            int amount = automaInfluenceCommitment(state, session, player);
            session.getInfluenceCommitments().put(player.getPlayerId(), amount);
            state.appendLog(
                    "AUTOMA_VOTE_INFLUENCE",
                    player.getPlayerId() + " committed " + amount + " influence for "
                            + session.getStanceByPlayer().getOrDefault(player.getPlayerId(), VoteStance.AGAINST)
                            + " after seeing the 5 drawn cubes."
            );
        }
    }

    private int votesForSide(CurrentVoteState session, VoteStance side) {
        InterpretedVote interpretedVote = side == VoteStance.FOR ? InterpretedVote.FOR : InterpretedVote.AGAINST;
        int total = session.getInterpretedVotes().getOrDefault(interpretedVote.name(), 0);
        for (Map.Entry<String, Integer> entry : session.getInfluenceCommitments().entrySet()) {
            if (session.getStanceByPlayer().get(entry.getKey()) == side) {
                total += entry.getValue();
            }
        }
        return total;
    }

    private boolean isSideWinning(VoteStance side, int forVotes, int againstVotes) {
        return side == VoteStance.FOR ? forVotes >= againstVotes : againstVotes > forVotes;
    }

    private int influenceRequiredToWin(VoteStance side, int forVotes, int againstVotes) {
        return side == VoteStance.FOR
                ? Math.max(0, againstVotes - forVotes)
                : Math.max(0, forVotes - againstVotes + 1);
    }

    private boolean guaranteesWinAfterOpposition(VoteStance side, int forVotes, int againstVotes, int opposingRemaining) {
        return side == VoteStance.FOR
                ? forVotes >= againstVotes + opposingRemaining
                : againstVotes > forVotes + opposingRemaining;
    }

    private int remainingInfluenceForSide(GameState state, CurrentVoteState session, VoteStance side) {
        int total = 0;
        for (PlayerState player : activePlayers(state)) {
            if (session.getInfluenceCommitments().containsKey(player.getPlayerId())) {
                continue;
            }
            if (session.getStanceByPlayer().get(player.getPlayerId()) == side) {
                total += Math.max(0, player.getInfluence());
            }
        }
        return total;
    }

    private int supportersWithRemainingInfluence(GameState state, CurrentVoteState session, VoteStance side) {
        int total = 0;
        for (PlayerState player : activePlayers(state)) {
            if (session.getInfluenceCommitments().containsKey(player.getPlayerId())) {
                continue;
            }
            if (session.getStanceByPlayer().get(player.getPlayerId()) == side && player.getInfluence() > 0) {
                total++;
            }
        }
        return total;
    }

    private VoteStance opposite(VoteStance side) {
        return side == VoteStance.FOR ? VoteStance.AGAINST : VoteStance.FOR;
    }

    private boolean isFinalRoundLastVoting(GameState state) {
        return state.getCurrentRound() >= state.getMaxRounds() && state.pendingPoliciesInOrder().size() <= 1;
    }

    private boolean isAutomaControlled(PlayerState player) {
        if (player == null) {
            return false;
        }
        return player.getControlMode() == PlayerControlMode.BOT
                || player.getControlMode() == PlayerControlMode.AUTOMA_SIMPLE
                || player.getControlMode() == PlayerControlMode.AUTOMA_COMPLEX
                || player.getBotStrategyMode() == BotStrategyMode.CARD_DRIVEN_SIMPLE_AUTOMA
                || player.getBotStrategyMode() == BotStrategyMode.CARD_DRIVEN_COMPLEX_AUTOMA;
    }

    private boolean isComplexAutoma(PlayerState player) {
        return player != null
                && (player.getControlMode() == PlayerControlMode.AUTOMA_COMPLEX
                || player.getBotStrategyMode() == BotStrategyMode.CARD_DRIVEN_COMPLEX_AUTOMA);
    }

    private boolean shouldAutomaCallExtraordinaryVote(GameState state, PlayerState actor, PolicyState policy) {
        if (!isAutomaControlled(actor) || actor.getInfluence() < 1 || policy == null || policy.getOccupyingProposalToken() == null) {
            return false;
        }
        ProposalToken token = policy.getOccupyingProposalToken();
        if (!Objects.equals(proposalTokenOwnerPlayerId(token), actor.getPlayerId())) {
            return false;
        }
        return wouldAutomaCallExtraordinaryVote(state, actor, policy, token.getTargetCourse());
    }

    private boolean wouldAutomaCallExtraordinaryVote(GameState state, PlayerState actor, PolicyState policy, PolicyCourse targetCourse) {
        if (!isAutomaControlled(actor) || actor.getInfluence() < 1 || policy == null || targetCourse == null) {
            return false;
        }
        CurrentVoteState preview = new CurrentVoteState();
        preview.setActiveProposalPolicyId(policy.getId());
        preview.setProposalAuthorPlayerId(actor.getPlayerId());
        preview.setCurrentCourseBeforeVote(policy.getCurrentCourse());
        preview.setTargetCourse(targetCourse);
        preview.getStanceByPlayer().put(actor.getPlayerId(), VoteStance.FOR);

        int supporters = Math.max(0, actor.getInfluence() - 1);
        int opponents = 0;
        int alliedClasses = 0;
        for (PlayerState player : activePlayers(state)) {
            if (Objects.equals(player.getPlayerId(), actor.getPlayerId())) {
                continue;
            }
            VoteStance stance = automaStanceFor(state, preview, player);
            if (stance == VoteStance.FOR) {
                supporters += Math.max(0, player.getInfluence());
                alliedClasses++;
            } else {
                opponents += Math.max(0, player.getInfluence());
            }
        }

        if (isTwoPlayerMode(state)) {
            VotingCubeOwnerClass actorOwner = votingBagRules.ownerForClass(actor.getClassType());
            int actorCubes = votingBagRules.countForOwner(state, actorOwner);
            int opponentCubes = activePlayers(state).stream()
                    .filter(player -> !Objects.equals(player.getPlayerId(), actor.getPlayerId()))
                    .map(player -> votingBagRules.ownerForClass(player.getClassType()))
                    .mapToInt(owner -> votingBagRules.countForOwner(state, owner))
                    .sum();
            return supporters > opponents && actorCubes >= opponentCubes;
        }
        return alliedClasses > 0 && supporters > opponents;
    }

    private boolean allStancesSubmitted(GameState state, CurrentVoteState session) {
        for (PlayerState player : activePlayers(state)) {
            if (!session.getStanceByPlayer().containsKey(player.getPlayerId())) {
                return false;
            }
        }
        return true;
    }

    private boolean allInfluenceCommitted(GameState state, CurrentVoteState session) {
        for (PlayerState player : activePlayers(state)) {
            if (!session.getInfluenceCommitments().containsKey(player.getPlayerId())) {
                return false;
            }
        }
        return true;
    }

    private List<PlayerState> activePlayers(GameState state) {
        List<PlayerState> ordered = new ArrayList<>();
        for (ClassType classType : state.getTurnOrder().getActiveClasses()) {
            findPlayerByClass(state, classType).ifPresent(ordered::add);
        }
        return ordered;
    }

    private Optional<PlayerState> findPlayerByClass(GameState state, ClassType classType) {
        return state.getPlayers().stream().filter(player -> player.getClassType() == classType).findFirst();
    }

    private int functioningEnterprisesByOwner(GameState state, ClassType classType) {
        return (int) state.getEnterprises().stream().filter(e -> e.getOwnerClass() == classType).filter(Enterprise::isFunctioning).count();
    }

    private boolean isTwoPlayerMode(GameState state) {
        return state.getTurnOrder().getActiveClasses().size() == 2;
    }

    private List<GameCommand> maybeGenerateSimpleConsumerCommands(GameState state, PlayerState actor) {
        if (actor == null || !consumerEconomyService.isSupportedConsumerBuyer(actor.getClassType())) {
            return List.of();
        }
        List<GameCommand> commands = new ArrayList<>();
        if (consumerEconomyService.canConsumeResource(actor, ResourceType.HEALTHCARE)) {
            commands.add(new ConsumeHealthcareCommand(actor.getPlayerId()));
        }
        if (consumerEconomyService.canConsumeResource(actor, ResourceType.EDUCATION)) {
            commands.add(new ConsumeEducationCommand(actor.getPlayerId()));
        }
        if (consumerEconomyService.canConsumeResource(actor, ResourceType.LUXURY)) {
            commands.add(new ConsumeLuxuryCommand(actor.getPlayerId()));
        }

        for (ResourceType resourceType : List.of(ResourceType.FOOD, ResourceType.HEALTHCARE, ResourceType.EDUCATION, ResourceType.LUXURY)) {
            List<SupplierOffer> offers = consumerEconomyService.listSupplierOffersFor(state, actor, resourceType);
            SupplierOffer best = offers.stream()
                    .sorted(Comparator.comparingInt(offer -> switch (offer.supplierType()) {
                        case CAPITALIST -> 0;
                        case MIDDLE_CLASS -> 1;
                        case STATE -> 2;
                        case EXTERNAL_MARKET -> 3;
                    }))
                    .findFirst()
                    .orElse(null);
            if (best == null) {
                continue;
            }
            int affordable = best.unitPrice() <= 0 ? 0 : Math.max(0, actor.getMoney()) / best.unitPrice();
            int quantity = Math.min(Math.max(0, actor.getPopulation()), Math.min(best.availableQuantity(), affordable));
            if (quantity <= 0) {
                continue;
            }
            commands.add(new BuyGoodsAndServicesCommand(
                    actor.getPlayerId(),
                    resourceType.name(),
                    List.of(new PurchaseItem(best.supplierType(), best.supplierPlayerId(), quantity))
            ));
        }
        return commands;
    }

    private void mergeEvaluationErrors(PurchaseEvaluation evaluation, List<String> errors, List<ValidationReasonCode> codes) {
        for (int i = 0; i < evaluation.errors().size(); i++) {
            String error = evaluation.errors().get(i);
            ValidationReasonCode code = evaluation.reasonCodes().size() > i
                    ? evaluation.reasonCodes().get(i)
                    : ValidationReasonCode.UNSUPPORTED_ACTION;
            addError(errors, codes, code, error);
        }
    }

    private void normalizeEconomyState(GameState state) {
        if (state.getPublicServicesStorage() == null || state.getPublicServicesStorage().isEmpty()) {
            state.setPublicServices(state.getPublicServices());
        }
        for (PlayerState player : state.getPlayers()) {
            if (player.getGoodsAndServicesArea() == null) {
                player.setGoodsAndServicesArea(Map.of());
            }
            if (player.getProducedResourceStorage() == null) {
                player.setProducedResourceStorage(Map.of());
            }
            if (player.getPrices() == null) {
                player.setPrices(Map.of());
            }
            player.getResources();
        }
    }

    private List<GameCommand> generateProposeBillCommands(GameState state, PlayerState player) {
        List<GameCommand> commands = new ArrayList<>();
        if (player.availableProposalTokens() <= 0) {
            return commands;
        }
        for (PolicyState policy : state.getPolicies()) {
            if (policy.isLocked() || policy.getOccupyingProposalToken() != null) {
                continue;
            }
            for (PolicyCourse adjacent : adjacentCourses(policy.getCurrentCourse())) {
                if (isAutomaControlled(player) && !isBeneficialPolicyCourseFor(player.getClassType(), policy, adjacent)) {
                    continue;
                }
                if (isAutomaControlled(player)
                        && state.getCurrentRound() >= state.getMaxRounds()
                        && policy.getId() == PolicyId.POLICY_6_FOREIGN_TRADE
                        && !wouldAutomaCallExtraordinaryVote(state, player, policy, adjacent)) {
                    continue;
                }
                commands.add(new ProposeBillCommand(player.getPlayerId(), policy.getId(), adjacent));
            }
        }
        return commands;
    }

    private boolean isBeneficialPolicyCourseFor(ClassType classType, PolicyState policy, PolicyCourse targetCourse) {
        if (policy == null || targetCourse == null) {
            return false;
        }
        PolicyCourse desired = desiredPolicyCourse(classType, policy.getId());
        PolicyCourse current = policy.getCurrentCourse();
        if (desired == null || current == null) {
            return false;
        }
        return Math.abs(targetCourse.ordinal() - desired.ordinal()) < Math.abs(current.ordinal() - desired.ordinal());
    }

    private Optional<GameCommand> maybeGenerateSimpleAssignWorkersCommand(GameState state, PlayerState player) {
        ClassType actorClass = player.getClassType();
        List<Worker> freeWorkers = state.getWorkers().stream()
                .filter(w -> w.getClassType() == actorClass)
                .filter(w -> w.getLocation() == WorkerLocation.UNEMPLOYED)
                .filter(w -> !w.isTiedContract())
                .toList();

        if (freeWorkers.isEmpty()) {
            return Optional.empty();
        }

        for (Enterprise enterprise : state.getEnterprises()) {
            if (enterprise.getOwnerClass() != actorClass || !enterprise.isFullyEmpty()) {
                continue;
            }

            List<EnterpriseSlot> targetSlots = enterprise.getSlots();
            if (targetSlots.size() > 3 || targetSlots.isEmpty()) {
                continue;
            }

            List<WorkerAssignmentOperation> operations = new ArrayList<>();
            Set<String> usedWorkerIds = new HashSet<>();
            boolean possible = true;
            for (EnterpriseSlot slot : targetSlots) {
                Worker found = pickMatchingWorker(freeWorkers, usedWorkerIds, slot);
                if (found == null) {
                    possible = false;
                    break;
                }
                usedWorkerIds.add(found.getId());
                operations.add(new WorkerAssignmentOperation(found.getId(), AssignmentTargetType.ENTERPRISE_SLOT, enterprise.getId() + ":" + slot.getId()));
            }
            if (possible && !operations.isEmpty()) {
                return Optional.of(new AssignWorkersCommand(player.getPlayerId(), operations));
            }
        }
        return Optional.empty();
    }

    private Optional<GameCommand> maybeGenerateSimpleStrikeCommand(GameState state, PlayerState player) {
        if (player.getClassType() != ClassType.WORKER) {
            return Optional.empty();
        }
        List<String> targets = legalStrikeEnterpriseIds(state).stream().limit(2).toList();
        if (targets.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PlaceStrikesCommand(player.getPlayerId(), targets));
    }

    private Optional<GameCommand> maybeGenerateSimpleDemonstrationCommand(GameState state, PlayerState player) {
        if (player.getClassType() != ClassType.WORKER || !canPlaceDemonstration(state)) {
            return Optional.empty();
        }
        return Optional.of(new PlaceDemonstrationCommand(player.getPlayerId(), Map.of()));
    }

    private List<String> legalStrikeEnterpriseIds(GameState state) {
        List<String> ids = new ArrayList<>();
        for (Enterprise enterprise : state.getEnterprises()) {
            List<String> errors = new ArrayList<>();
            List<ValidationReasonCode> codes = new ArrayList<>();
            validateStrikeTarget(state, enterprise, errors, codes);
            if (errors.isEmpty()) {
                ids.add(enterprise.getId());
            }
        }
        return ids;
    }

    private void validateStrikeTarget(GameState state, Enterprise enterprise, List<String> errors, List<ValidationReasonCode> codes) {
        if (enterprise.isStrikeToken()) {
            addError(errors, codes, ValidationReasonCode.STRIKE_ALREADY_PRESENT, "Strike token already present on " + enterprise.getId() + ".");
        }
        if (enterprise.getWageLevel() >= 3) {
            addError(errors, codes, ValidationReasonCode.STRIKE_WAGE_TOO_HIGH, "Cannot strike at wage level 3 enterprise: " + enterprise.getId() + ".");
        }
        if (enterprise.getOwnerClass() == ClassType.STATE && findPlayerByClass(state, ClassType.STATE).isEmpty()) {
            addError(errors, codes, ValidationReasonCode.INVALID_TARGET, "State enterprise strikes require an active State player.");
        }
        List<Worker> workerClassWorkers = workersAtEnterprise(state, enterprise).stream()
                .filter(worker -> worker.getClassType() == ClassType.WORKER)
                .toList();
        if (workerClassWorkers.isEmpty()) {
            addError(errors, codes, ValidationReasonCode.INVALID_TARGET,
                    "Strike target must employ at least one worker-class worker: " + enterprise.getId() + ".");
        }
        if (workerClassWorkers.stream().anyMatch(Worker::isTiedContract)) {
            addError(errors, codes, ValidationReasonCode.WORKER_TIED_BY_CONTRACT,
                    "Worker-class workers on strike target must not be tied by labor contract: " + enterprise.getId() + ".");
        }
    }

    private boolean canPlaceDemonstration(GameState state) {
        if (state.isDemonstrationToken()) {
            return false;
        }
        return workerUnemploymentSurplus(state) >= 2;
    }

    private void clearDemonstrationIfNoLongerValid(GameState state) {
        if (!state.isDemonstrationToken() || state.getTurnOrder().getPhase() != RoundPhase.ACTIONS) {
            return;
        }
        if (workerUnemploymentSurplus(state) >= 2) {
            return;
        }
        state.setDemonstrationToken(false);
        state.setDemonstrationPenaltyAllocation(Map.of());
        state.appendLog("DEMONSTRATION_CLEARED", "Demonstration token removed because worker unemployment surplus dropped below 2.");
    }

    private int workerUnemploymentSurplus(GameState state) {
        int unemployed = unemployedWorkerClassCount(state);
        return unemployed - availableWorkerClassVacancies(state);
    }

    private int unemployedWorkerClassCount(GameState state) {
        return (int) state.getWorkers().stream()
                .filter(worker -> worker.getClassType() == ClassType.WORKER)
                .filter(worker -> worker.getLocation() == WorkerLocation.UNEMPLOYED)
                .count();
    }

    private int availableWorkerClassVacancies(GameState state) {
        List<Worker> unemployedWorkers = state.getWorkers().stream()
                .filter(worker -> worker.getClassType() == ClassType.WORKER)
                .filter(worker -> worker.getLocation() == WorkerLocation.UNEMPLOYED)
                .filter(worker -> !worker.isTiedContract())
                .toList();
        if (unemployedWorkers.isEmpty()) {
            return 0;
        }
        int vacancies = 0;
        for (Enterprise enterprise : state.getEnterprises()) {
            if (enterprise.getOwnerClass() == ClassType.WORKER) {
                continue;
            }
            for (EnterpriseSlot slot : enterprise.getSlots()) {
                if (slot.isOccupied()) {
                    continue;
                }
                if (unemployedWorkers.stream().anyMatch(worker -> slotMatchesWorker(slot, worker))) {
                    vacancies++;
                }
            }
        }
        return vacancies;
    }

    private int demonstrationPenaltyPool(GameState state) {
        int unionWorkers = (int) state.getWorkers().stream()
                .filter(worker -> worker.getClassType() == ClassType.WORKER)
                .filter(worker -> worker.getLocation() == WorkerLocation.UNION)
                .count();
        return unemployedWorkerClassCount(state) + unionWorkers;
    }

    private List<Worker> workersAtEnterprise(GameState state, Enterprise enterprise) {
        return state.getWorkers().stream()
                .filter(worker -> worker.getLocation() == WorkerLocation.ENTERPRISE_SLOT)
                .filter(worker -> Objects.equals(worker.getEnterpriseId(), enterprise.getId()))
                .toList();
    }

    private Worker pickMatchingWorker(List<Worker> workers, Set<String> used, EnterpriseSlot slot) {
        for (Worker worker : workers) {
            if (used.contains(worker.getId())) {
                continue;
            }
            if (slotMatchesWorker(slot, worker)) {
                return worker;
            }
        }
        return null;
    }

    private boolean enterprisesRemainBinaryAfterSimulation(GameState state, List<WorkerAssignmentOperation> assignments) {
        GameState copy = state.copy();

        for (WorkerAssignmentOperation op : assignments) {
            if (op.targetType() != AssignmentTargetType.ENTERPRISE_SLOT) {
                continue;
            }

            Worker worker = copy.findWorker(op.workerId()).orElse(null);
            if (worker == null) {
                return false;
            }

            if (worker.getLocation() == WorkerLocation.ENTERPRISE_SLOT && worker.getEnterpriseId() != null && worker.getSlotId() != null) {
                Enterprise previous = copy.findEnterprise(worker.getEnterpriseId()).orElse(null);
                if (previous != null) {
                    previous.getSlots().stream()
                            .filter(s -> s.getId().equals(worker.getSlotId()))
                            .findFirst()
                            .ifPresent(slot -> slot.setOccupiedWorkerId(null));
                }
            }

            if (op.targetType() == AssignmentTargetType.UNEMPLOYED) {
                worker.setLocation(WorkerLocation.UNEMPLOYED);
                worker.setEnterpriseId(null);
                worker.setSlotId(null);
                worker.setTiedContract(false);
                continue;
            }

            SlotRef slotRef = parseSlotRef(op.targetId());
            if (slotRef == null) {
                return false;
            }

            Enterprise targetEnterprise = copy.findEnterprise(slotRef.enterpriseId()).orElse(null);
            if (targetEnterprise == null) {
                return false;
            }
            EnterpriseSlot targetSlot = targetEnterprise.getSlots().stream()
                    .filter(s -> s.getId().equals(slotRef.slotId()))
                    .findFirst()
                    .orElse(null);
            if (targetSlot == null) {
                return false;
            }
            targetSlot.setOccupiedWorkerId(worker.getId());
            worker.setLocation(WorkerLocation.ENTERPRISE_SLOT);
            worker.setEnterpriseId(targetEnterprise.getId());
            worker.setSlotId(targetSlot.getId());
        }

        return copy.getEnterprises().stream().noneMatch(Enterprise::isPartiallyFilled);
    }

    private boolean enterpriseHasTiedWorkers(GameState state, Enterprise enterprise) {
        for (EnterpriseSlot slot : enterprise.getSlots()) {
            if (!slot.isOccupied()) {
                continue;
            }
            Worker worker = state.findWorker(slot.getOccupiedWorkerId()).orElse(null);
            if (worker != null && worker.isTiedContract()) {
                return true;
            }
        }
        return false;
    }

    private void clearEnterpriseWorkerContracts(GameState state, Enterprise enterprise) {
        for (EnterpriseSlot slot : enterprise.getSlots()) {
            if (!slot.isOccupied()) {
                continue;
            }
            state.findWorker(slot.getOccupiedWorkerId()).ifPresent(worker -> worker.setTiedContract(false));
        }
    }

    private List<PolicyCourse> adjacentCourses(PolicyCourse currentCourse) {
        return switch (currentCourse) {
            case A -> List.of(PolicyCourse.B);
            case B -> List.of(PolicyCourse.A, PolicyCourse.C);
            case C -> List.of(PolicyCourse.B);
        };
    }

    private VoteStance parseStance(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return VoteStance.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
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

    private boolean isLegacy(ActionType actionType) {
        return switch (actionType) {
            case START_TURN, HIRE_WORKER, PRODUCE_GOODS, SELL_GOODS, ADJUST_POLICY, PLAY_CARD -> true;
            default -> false;
        };
    }

    private boolean canUseHumanManualOverride(GameState state, GameCommand command) {
        if (!(command instanceof PlayCardCommand)) {
            return false;
        }
        PlayerState current = state.currentPlayer();
        if (current == null || current.getControlMode() != PlayerControlMode.HUMAN) {
            return false;
        }
        String actorPlayerId = actorPlayerId(command);
        if (actorPlayerId == null || actorPlayerId.isBlank()) {
            return true;
        }
        return state.findPlayerById(actorPlayerId).isPresent();
    }

    private String actorPlayerId(GameCommand command) {
        return switch (command) {
            case AdvanceGameFlowCommand advance -> advance.actorPlayerId();
            case AdvanceToVotingCommand advance -> advance.actorPlayerId();
            case AdvanceToProductionCommand advance -> advance.actorPlayerId();
            case ResolveProductionPhaseCommand resolve -> resolve.actorPlayerId();
            case AdvanceToScoringCommand advance -> advance.actorPlayerId();
            case ResolveScoringPhaseCommand resolve -> resolve.actorPlayerId();
            case AdvanceToNextRoundCommand advance -> advance.actorPlayerId();
            case ResolvePreparationPhaseCommand resolve -> resolve.actorPlayerId();
            case AdvanceRoundCommand advance -> advance.actorPlayerId();
            case ProposeBillCommand propose -> propose.actorPlayerId();
            case AddVotingCubesCommand add -> add.actorPlayerId();
            case CallExtraordinaryVoteCommand vote -> vote.actorPlayerId();
            case AssignWorkersCommand assign -> assign.actorPlayerId();
            case BuyGoodsAndServicesCommand buy -> buy.actorPlayerId();
            case ConsumeHealthcareCommand consume -> consume.actorPlayerId();
            case ConsumeEducationCommand consume -> consume.actorPlayerId();
            case ConsumeLuxuryCommand consume -> consume.actorPlayerId();
            case RefreshBusinessDealsCommand refresh -> refresh.actorPlayerId();
            case DeclareVoteStanceCommand stance -> stance.actorPlayerId();
            case CommitVoteInfluenceCommand commit -> commit.actorPlayerId();
            default -> "";
        };
    }

    private List<DomainEvent> applyOpeningWorkerMigrationIfNeededInternal(GameState state) {
        if (state == null || state.getCurrentRound() != 1) {
            return List.of();
        }
        if (state.getTurnOrder().getPhase() != RoundPhase.ACTIONS) {
            return List.of();
        }
        PlayerState current = state.currentPlayer();
        if (current == null || current.getClassType() != ClassType.WORKER) {
            return List.of();
        }
        boolean alreadyApplied = state.getEventLog().stream()
                .anyMatch(entry -> WORKER_OPENING_MIGRATION_MARKER.equals(entry.getType()));
        if (alreadyApplied) {
            return List.of();
        }

        migrationCardManager.ensureInitialized(state);
        randomizeMigrationDeckOrder(state, "WORKER_OPENING_TURN");
        MigrationCardState card = drawNextMigrationCardForOpening(state);
        if (card == null || card.getCardId() == null || card.getCardId().isBlank()
                || card.getWorkerEntry() == null || card.getWorkerEntry().getQualificationType() == null) {
            String note = "Opening migration before worker turn: no valid card was available.";
            state.appendLog(WORKER_OPENING_MIGRATION_MARKER, note);
            return List.of(new MigrationResolvedEvent(state.getCurrentRound(), List.of(note)));
        }

        Worker migratedWorker = createMigratedWorkerFromEntry(state, ClassType.WORKER, card.getWorkerEntry());
        state.getWorkers().add(migratedWorker);
        current.setPopulation(populationForClass(state, ClassType.WORKER));

        String summary = "Opening migration before worker turn: " + card.getCardId()
                + " -> " + describeMigrationEntry(card.getWorkerEntry())
                + " (" + migratedWorker.getId() + ")";
        state.appendLog(WORKER_OPENING_MIGRATION_MARKER, summary);
        return List.of(new MigrationResolvedEvent(state.getCurrentRound(), List.of(summary)));
    }

    private MigrationCardState drawNextMigrationCardForOpening(GameState state) {
        OrderedCardDeckState deck = state.getMigrationDeck();
        List<String> ordered = deck.getOrderedCardIds();
        if (ordered == null || ordered.isEmpty()) {
            return null;
        }

        int currentIndex = Math.floorMod(deck.getNextCardIndex(), ordered.size());
        String cardId = ordered.get(currentIndex);
        deck.setNextCardIndex((currentIndex + 1) % ordered.size());

        List<String> visible = new ArrayList<>(deck.getVisibleCardIds() == null ? List.of() : deck.getVisibleCardIds());
        visible.add(cardId);
        deck.setVisibleCardIds(visible);
        deck.setLastRefreshedRound(state.getCurrentRound());
        deck.setLastRefreshReason("WORKER_OPENING_TURN");
        deck.setRefreshCount(deck.getRefreshCount() + 1);
        state.setMigrationDeck(deck);

        return state.getMigrationCards().stream()
                .filter(card -> cardId.equals(card.getCardId()))
                .findFirst()
                .map(MigrationCardState::copy)
                .orElse(null);
    }

    private void randomizeMigrationDeckOrder(GameState state, String reason) {
        OrderedCardDeckState deck = state.getMigrationDeck();
        if (deck == null || deck.getOrderedCardIds() == null || deck.getOrderedCardIds().size() < 2) {
            return;
        }
        List<String> shuffled = new ArrayList<>(deck.getOrderedCardIds());
        Collections.shuffle(shuffled, ThreadLocalRandom.current());
        deck.setOrderedCardIds(shuffled);
        deck.setNextCardIndex(0);
        deck.setVisibleCardIds(List.of());
        deck.setLastRefreshedRound(state.getCurrentRound());
        deck.setLastRefreshReason(reason);
        state.setMigrationDeck(deck);
    }

    private Worker createMigratedWorkerFromEntry(GameState state, ClassType classType, MigrationCardEntry entry) {
        Worker worker = new Worker();
        worker.setId(nextWorkerIdForClass(state, classType));
        worker.setClassType(classType);
        worker.setQualificationType(entry.getQualificationType());
        worker.setSector(entry.getSector());
        worker.setLocation(WorkerLocation.UNEMPLOYED);
        worker.setTiedContract(false);
        worker.setEnterpriseId(null);
        worker.setSlotId(null);
        return worker;
    }

    private Worker createUnskilledUnemployedWorker(GameState state, ClassType classType) {
        Worker worker = new Worker();
        worker.setId(nextWorkerIdForClass(state, classType));
        worker.setClassType(classType);
        worker.setQualificationType(WorkerQualification.UNSKILLED);
        worker.setSector(WorkerSector.GENERAL);
        worker.setLocation(WorkerLocation.UNEMPLOYED);
        worker.setTiedContract(false);
        worker.setEnterpriseId(null);
        worker.setSlotId(null);
        return worker;
    }

    private int populationForClass(GameState state, ClassType classType) {
        int workerCount = (int) state.getWorkers().stream()
                .filter(worker -> worker.getClassType() == classType)
                .count();
        return PopulationScale.fromWorkerCount(workerCount);
    }

    private String nextWorkerIdForClass(GameState state, ClassType classType) {
        String prefix = classType.playerId() + "-worker-";
        int nextIndex = 1;
        for (Worker existing : state.getWorkers()) {
            String workerId = existing.getId();
            if (workerId == null || !workerId.startsWith(prefix)) {
                continue;
            }
            String suffix = workerId.substring(prefix.length());
            try {
                nextIndex = Math.max(nextIndex, Integer.parseInt(suffix) + 1);
            } catch (NumberFormatException ignored) {
                // Ignore non-standard ids in manual/debug setups.
            }
        }
        return prefix + nextIndex;
    }

    private Worker findTrainableWorker(GameState state, ClassType classType) {
        return state.getWorkers().stream()
                .filter(worker -> worker.getClassType() == classType)
                .filter(worker -> worker.getQualificationType() == WorkerQualification.UNSKILLED)
                .findFirst()
                .orElse(null);
    }

    private Worker findSelectedTrainableWorker(GameState state, ClassType classType, String workerId) {
        if (workerId == null || workerId.isBlank()) {
            return findTrainableWorker(state, classType);
        }
        return state.getWorkers().stream()
                .filter(worker -> Objects.equals(worker.getId(), workerId))
                .filter(worker -> worker.getClassType() == classType)
                .filter(worker -> worker.getQualificationType() == WorkerQualification.UNSKILLED)
                .findFirst()
                .orElse(null);
    }

    private String describeMigrationEntry(MigrationCardEntry entry) {
        if (entry == null || entry.getQualificationType() == null) {
            return "UNKNOWN";
        }
        if (entry.getQualificationType() == WorkerQualification.UNSKILLED) {
            return "UNSKILLED/GRAY";
        }
        return "SKILLED/" + (entry.getSector() == null ? "GENERAL" : entry.getSector().name());
    }

    private SlotRef parseSlotRef(String targetId) {
        if (targetId == null || !targetId.contains(":")) {
            return null;
        }
        String[] parts = targetId.split(":", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            return null;
        }
        return new SlotRef(parts[0], parts[1]);
    }

    private void addError(List<String> errors, List<ValidationReasonCode> codes, ValidationReasonCode code, String message) {
        errors.add(message);
        if (!codes.contains(code)) {
            codes.add(code);
        }
    }

    private static Map<PolicyCourse, Map<PolicyCourse, Integer>> sanitizeTaxMatrix(Map<PolicyCourse, Map<PolicyCourse, Integer>> raw) {
        Map<PolicyCourse, Map<PolicyCourse, Integer>> base = new EnumMap<>(PolicyCourse.class);
        Map<PolicyCourse, Map<PolicyCourse, Integer>> source = raw == null || raw.isEmpty() ? defaultWorkerTaxMatrix() : raw;
        for (PolicyCourse labor : PolicyCourse.values()) {
            Map<PolicyCourse, Integer> row = new EnumMap<>(PolicyCourse.class);
            Map<PolicyCourse, Integer> inputRow = source.getOrDefault(labor, Map.of());
            for (PolicyCourse tax : PolicyCourse.values()) {
                row.put(tax, Math.max(0, inputRow.getOrDefault(tax, defaultWorkerTaxMatrix().get(labor).get(tax))));
            }
            base.put(labor, row);
        }
        return base;
    }

    private static Map<PolicyCourse, Map<PolicyCourse, Integer>> defaultWorkerTaxMatrix() {
        Map<PolicyCourse, Map<PolicyCourse, Integer>> matrix = new EnumMap<>(PolicyCourse.class);

        Map<PolicyCourse, Integer> rowA = new EnumMap<>(PolicyCourse.class);
        rowA.put(PolicyCourse.A, 0);
        rowA.put(PolicyCourse.B, 1);
        rowA.put(PolicyCourse.C, 2);
        matrix.put(PolicyCourse.A, rowA);

        Map<PolicyCourse, Integer> rowB = new EnumMap<>(PolicyCourse.class);
        rowB.put(PolicyCourse.A, 1);
        rowB.put(PolicyCourse.B, 2);
        rowB.put(PolicyCourse.C, 3);
        matrix.put(PolicyCourse.B, rowB);

        Map<PolicyCourse, Integer> rowC = new EnumMap<>(PolicyCourse.class);
        rowC.put(PolicyCourse.A, 2);
        rowC.put(PolicyCourse.B, 3);
        rowC.put(PolicyCourse.C, 4);
        matrix.put(PolicyCourse.C, rowC);

        return matrix;
    }

    private record SlotRef(String enterpriseId, String slotId) {
    }

    private ValidationResult validateLegacyAction(GameState state, ActionType actionType) {
        if (!state.isDemoMode()) {
            return ValidationResult.invalid(
                    ValidationReasonCode.LEGACY_ACTION_DISABLED,
                    "Legacy demo action is disabled outside demo mode: " + actionType
            );
        }
        return ValidationResult.valid();
    }

    private List<DomainEvent> applyLegacyStartTurn(GameState state) {
        PlayerState current = state.currentPlayer();
        if (current == null) {
            return List.of();
        }
        return List.of(new TurnStartedEvent(current.getRole(), state.getTurnOrder().getRound()));
    }

    private List<DomainEvent> applyLegacyEndTurn(GameState state) {
        PlayerState current = state.currentPlayer();
        if (current == null) {
            return List.of();
        }
        PlayerRole endedBy = current.getRole();

        int beforeIndex = state.getTurnOrder().getCurrentPlayerIndex();
        state.getTurnOrder().moveToNextPlayer();
        int afterIndex = state.getTurnOrder().getCurrentPlayerIndex();
        if (afterIndex <= beforeIndex) {
            state.getTurnOrder().setRound(state.getTurnOrder().getRound() + 1);
        }

        PlayerState nextPlayer = state.currentPlayer();
        return List.of(new TurnEndedEvent(
                endedBy,
                nextPlayer == null ? endedBy : nextPlayer.getRole(),
                afterIndex,
                state.getTurnOrder().getRound()
        ));
    }

    private List<DomainEvent> applyLegacyHireWorker(GameState state, HireWorkerCommand command) {
        PlayerState player = state.currentPlayer();
        if (player == null) {
            return List.of();
        }
        int requested = Math.max(0, command.count());
        int affordable = state.getMarket().getWorkerHireCost() <= 0
                ? requested
                : Math.max(0, player.getMoney() / state.getMarket().getWorkerHireCost());
        int hired = Math.min(requested, affordable);
        int totalCost = hired * state.getMarket().getWorkerHireCost();

        player.setMoney(player.getMoney() - totalCost);
        player.setEmployedWorkers(player.getEmployedWorkers() + hired);
        player.setAvailableWorkers(Math.max(0, player.getAvailableWorkers() - hired));
        return List.of(new WorkerHiredEvent(player.getRole(), hired, totalCost));
    }

    private List<DomainEvent> applyLegacyProduceGoods(GameState state, ProduceGoodsCommand command) {
        PlayerState player = state.currentPlayer();
        if (player == null) {
            return List.of();
        }
        int amount = Math.max(0, command.amount());
        player.setGoods(player.getGoods() + amount);
        return List.of(new GoodsProducedEvent(player.getRole(), amount));
    }

    private List<DomainEvent> applyLegacySellGoods(GameState state, SellGoodsCommand command) {
        PlayerState player = state.currentPlayer();
        if (player == null) {
            return List.of();
        }
        int requested = Math.max(0, command.amount());
        int sold = Math.min(requested, Math.max(0, player.getGoods()));
        int revenue = sold * Math.max(0, state.getMarket().getGoodsPrice());

        player.setGoods(player.getGoods() - sold);
        player.setMoney(player.getMoney() + revenue);
        return List.of(new GoodsSoldEvent(player.getRole(), sold, revenue));
    }

    private List<DomainEvent> applyLegacyAdjustPolicy(GameState state, AdjustPolicyCommand command) {
        if (command.track() != PolicyTrack.TAXATION) {
            return List.of();
        }
        int before = state.getTaxMultiplier();
        int after = Math.max(0, before + command.delta());
        state.setTaxMultiplier(after);
        return List.of(new PolicyAdjustedEvent(command.track(), before, after));
    }

    private List<DomainEvent> applyLegacyPlayCard(GameState state, PlayCardCommand command) {
        PlayerState player = state.currentPlayer();
        if (player == null) {
            return List.of();
        }

        String cardId = command.cardId() == null ? "" : command.cardId();
        String cardName = cardId.isBlank() ? "Unknown card" : cardId;
        int moneyDelta = 0;
        int goodsDelta = 0;
        int taxationDelta = 0;
        boolean manualOverrideApplied = false;

        if (!cardId.isBlank() && cardId.startsWith("manual:") && player.getControlMode() == PlayerControlMode.HUMAN) {
            int beforeMoney = player.getMoney();
            int beforeGoods = player.getGoods();
            int beforeTax = state.getTaxMultiplier();
            applyManualOverrideCard(state, player, cardId.substring("manual:".length()));
            moneyDelta = player.getMoney() - beforeMoney;
            goodsDelta = player.getGoods() - beforeGoods;
            taxationDelta = state.getTaxMultiplier() - beforeTax;
            cardName = "Manual override";
            manualOverrideApplied = true;
        } else if (!cardId.isBlank()) {
            var cardOpt = cardCatalog.findById(cardId);
            if (cardOpt.isPresent()) {
                var card = cardOpt.get();
                cardName = card.getName() == null || card.getName().isBlank() ? cardId : card.getName();
                var outcome = cardEffectProcessor.resolve(state, player, card);
                moneyDelta = outcome.moneyDelta();
                goodsDelta = outcome.goodsDelta();
                taxationDelta = outcome.taxationDelta();
            }
        }

        if (!cardId.isBlank()) {
            player.getHandCards().remove(cardId);
        }
        if (!manualOverrideApplied) {
            player.setMoney(player.getMoney() + moneyDelta);
            player.setGoods(Math.max(0, player.getGoods() + goodsDelta));
            state.setTaxMultiplier(Math.max(0, state.getTaxMultiplier() + taxationDelta));
        }

        return List.of(new CardPlayedEvent(player.getRole(), cardId, cardName, moneyDelta, goodsDelta, taxationDelta));
    }

    private void applyManualOverrideCard(GameState state, PlayerState actor, String rawInstructions) {
        if (rawInstructions == null || rawInstructions.isBlank()) {
            return;
        }

        for (String instruction : rawInstructions.split(";")) {
            String trimmed = instruction == null ? "" : instruction.trim();
            if (trimmed.isBlank()) {
                continue;
            }

            if (trimmed.startsWith("state_to_actor_money:")) {
                int requested = parseIntSuffix(trimmed, "state_to_actor_money:");
                int transferred = Math.min(Math.max(0, requested), Math.max(0, state.getTreasury()));
                state.setTreasury(state.getTreasury() - transferred);
                addMoneyToActor(actor, transferred);
                state.appendLog("MANUAL_OVERRIDE", actor.getPlayerId() + " took " + transferred + " from treasury.");
                continue;
            }

            if (trimmed.startsWith("capitalist_to_actor_money:")) {
                int requested = parseIntSuffix(trimmed, "capitalist_to_actor_money:");
                PlayerState capitalist = findPlayerByClassType(state, ClassType.CAPITALIST);
                if (capitalist != null && capitalist != actor) {
                    int transferred = Math.min(Math.max(0, requested), Math.max(0, capitalist.getRevenue()));
                    capitalist.setRevenue(capitalist.getRevenue() - transferred);
                    addMoneyToActor(actor, transferred);
                    state.appendLog("MANUAL_OVERRIDE", actor.getPlayerId() + " took " + transferred + " from capitalist.");
                }
                continue;
            }

            if (trimmed.startsWith("player_to_actor_money:")) {
                applyPlayerToActorMoneyTransfer(state, actor, trimmed);
                continue;
            }

            if (trimmed.startsWith("actor_to_player_money:")) {
                applyActorToPlayerMoneyTransfer(state, actor, trimmed);
                continue;
            }

            if (trimmed.startsWith("actor_to_state_money:")) {
                int requested = parseIntSuffix(trimmed, "actor_to_state_money:");
                int transferred = consumeMoneyFromActor(actor, requested);
                state.setTreasury(state.getTreasury() + transferred);
                state.appendLog("MANUAL_OVERRIDE", actor.getPlayerId() + " transferred " + transferred + " to treasury.");
                continue;
            }

            if (trimmed.startsWith("actor_money_delta:")) {
                int delta = parseIntSuffix(trimmed, "actor_money_delta:");
                if (actor.getClassType() == ClassType.CAPITALIST) {
                    actor.setRevenue(Math.max(0, actor.getRevenue() + delta));
                } else {
                    actor.setMoney(Math.max(0, actor.getMoney() + delta));
                }
                state.appendLog("MANUAL_OVERRIDE", actor.getPlayerId() + " applied actor_money_delta " + delta + ".");
                continue;
            }

            if (trimmed.startsWith("treasury_delta:")) {
                int delta = parseIntSuffix(trimmed, "treasury_delta:");
                state.setTreasury(Math.max(0, state.getTreasury() + delta));
                state.appendLog("MANUAL_OVERRIDE", "Treasury delta applied: " + delta + ".");
                continue;
            }

            if (trimmed.startsWith("actor_welfare_delta:")) {
                int delta = parseIntSuffix(trimmed, "actor_welfare_delta:");
                actor.setWelfare(Math.max(0, actor.getWelfare() + delta));
                actor.setLastWelfareDelta(delta);
                state.appendLog("MANUAL_OVERRIDE", actor.getPlayerId() + " applied actor_welfare_delta " + delta + ".");
                continue;
            }

            if (trimmed.startsWith("state_to_actor_resource:")) {
                applyStateToActorResourceTransfer(state, actor, trimmed);
                continue;
            }

            if (trimmed.startsWith("capitalist_to_actor_resource:")) {
                applyCapitalistToActorResourceTransfer(state, actor, trimmed);
                continue;
            }

            if (trimmed.startsWith("add_workers_color:")) {
                String payload = trimmed.substring("add_workers_color:".length()).trim();
                String[] parts = payload.split(":");
                if (parts.length >= 2) {
                    WorkerSlotColor color = WorkerSlotColor.fromRaw(parts[0]);
                    int count;
                    try {
                        count = Integer.parseInt(parts[1].trim());
                    } catch (NumberFormatException ex) {
                        count = 0;
                    }
                    addManualWorkers(state, actor, color, Math.max(0, count));
                }
                continue;
            }

            if (trimmed.startsWith("add_workers:")) {
                int count = Math.max(0, parseIntSuffix(trimmed, "add_workers:"));
                addManualWorkers(state, actor, WorkerSlotColor.GRAY, count);
            }
        }
    }

    private void applyPlayerToActorMoneyTransfer(GameState state, PlayerState actor, String instruction) {
        String payload = instruction.substring("player_to_actor_money:".length()).trim();
        String[] parts = payload.split(":");
        if (parts.length < 2) {
            return;
        }
        PlayerState source = state.findPlayerById(parts[0].trim()).orElse(null);
        int requested = parseIntSafe(parts[1]);
        if (source == null || source == actor || requested <= 0) {
            return;
        }
        int transferred = consumeMoneyFromPlayer(source, requested);
        addMoneyToActor(actor, transferred);
        state.appendLog("MANUAL_OVERRIDE", actor.getPlayerId() + " took " + transferred + " from " + source.getPlayerId() + ".");
    }

    private void applyActorToPlayerMoneyTransfer(GameState state, PlayerState actor, String instruction) {
        String payload = instruction.substring("actor_to_player_money:".length()).trim();
        String[] parts = payload.split(":");
        if (parts.length < 2) {
            return;
        }
        PlayerState recipient = state.findPlayerById(parts[0].trim()).orElse(null);
        int requested = parseIntSafe(parts[1]);
        if (recipient == null || recipient == actor || requested <= 0) {
            return;
        }
        int transferred = consumeMoneyFromActor(actor, requested);
        addMoneyToActor(recipient, transferred);
        state.appendLog("MANUAL_OVERRIDE", actor.getPlayerId() + " transferred " + transferred + " to " + recipient.getPlayerId() + ".");
    }

    private void addManualWorkers(GameState state, PlayerState actor, WorkerSlotColor color, int count) {
        if (count <= 0) {
            return;
        }
        WorkerSlotColor safeColor = color == null ? WorkerSlotColor.GRAY : color;
        WorkerQualification qualification = safeColor == WorkerSlotColor.GRAY
                ? WorkerQualification.UNSKILLED
                : WorkerQualification.SKILLED;
        WorkerSector sector = safeColor.toWorkerSector();
        for (int i = 0; i < count; i++) {
            Worker worker = new Worker();
            worker.setId(nextWorkerIdForClass(state, actor.getClassType()));
            worker.setClassType(actor.getClassType());
            worker.setQualificationType(qualification);
            worker.setSector(sector);
            worker.setLocation(WorkerLocation.UNEMPLOYED);
            worker.setTiedContract(false);
            worker.setEnterpriseId(null);
            worker.setSlotId(null);
            state.getWorkers().add(worker);
            actor.setPopulation(actor.getPopulation() + 1);
        }
        state.appendLog("MANUAL_OVERRIDE", actor.getPlayerId() + " added workers: " + count + " (" + safeColor.name() + ").");
    }

    private void applyStateToActorResourceTransfer(GameState state, PlayerState actor, String instruction) {
        String payload = instruction.substring("state_to_actor_resource:".length()).trim();
        String[] parts = payload.split(":");
        if (parts.length < 2) {
            return;
        }
        ResourceType resourceType = ResourceType.fromRaw(parts[0]);
        if (resourceType == null) {
            return;
        }
        int requested = parseIntSafe(parts[1]);
        if (requested <= 0) {
            return;
        }

        String stateServiceResourceId = mapStateServiceResourceId(resourceType);
        int remaining = requested;
        int consumedFromServices = 0;
        if (stateServiceResourceId != null) {
            consumedFromServices = state.consumePublicServiceAmount(stateServiceResourceId, remaining);
            remaining -= consumedFromServices;
        }

        int consumedFromStatePlayer = 0;
        if (remaining > 0) {
            PlayerState statePlayer = findPlayerByClassType(state, ClassType.STATE);
            if (statePlayer != null) {
                consumedFromStatePlayer = statePlayer.consumeResource(resourceType.id(), remaining);
                remaining -= consumedFromStatePlayer;
            }
        }

        int transferred = consumedFromServices + consumedFromStatePlayer;
        if (transferred > 0) {
            actor.addResource(resourceType.id(), transferred);
            state.appendLog("MANUAL_OVERRIDE",
                    actor.getPlayerId() + " took " + transferred + " " + resourceType.id() + " from state.");
        }
    }

    private void applyCapitalistToActorResourceTransfer(GameState state, PlayerState actor, String instruction) {
        String payload = instruction.substring("capitalist_to_actor_resource:".length()).trim();
        String[] parts = payload.split(":");
        if (parts.length < 2) {
            return;
        }
        ResourceType resourceType = ResourceType.fromRaw(parts[0]);
        if (resourceType == null) {
            return;
        }
        int requested = parseIntSafe(parts[1]);
        if (requested <= 0) {
            return;
        }
        PlayerState capitalist = findPlayerByClassType(state, ClassType.CAPITALIST);
        if (capitalist == null || capitalist == actor) {
            return;
        }

        int transferred = capitalist.consumeResource(resourceType.id(), requested);
        if (transferred > 0) {
            actor.addResource(resourceType.id(), transferred);
            state.appendLog("MANUAL_OVERRIDE",
                    actor.getPlayerId() + " took " + transferred + " " + resourceType.id() + " from capitalist.");
        }
    }

    private String mapStateServiceResourceId(ResourceType resourceType) {
        if (resourceType == null) {
            return null;
        }
        if (resourceType == ResourceType.HEALTHCARE || resourceType == ResourceType.EDUCATION || resourceType == ResourceType.MEDIA_INFLUENCE) {
            return resourceType.id();
        }
        if (resourceType == ResourceType.INFLUENCE) {
            return ResourceType.MEDIA_INFLUENCE.id();
        }
        return null;
    }

    private PlayerState findPlayerByClassType(GameState state, ClassType classType) {
        if (state == null || classType == null) {
            return null;
        }
        for (PlayerState player : state.getPlayers()) {
            if (player != null && player.getClassType() == classType) {
                return player;
            }
        }
        return null;
    }

    private void addMoneyToActor(PlayerState actor, int amount) {
        if (actor == null || amount <= 0) {
            return;
        }
        if (actor.getClassType() == ClassType.CAPITALIST) {
            actor.setRevenue(actor.getRevenue() + amount);
            return;
        }
        actor.setMoney(actor.getMoney() + amount);
    }

    private int consumeMoneyFromActor(PlayerState actor, int requested) {
        return consumeMoneyFromPlayer(actor, requested);
    }

    private int consumeMoneyFromPlayer(PlayerState actor, int requested) {
        if (actor == null || requested <= 0) {
            return 0;
        }
        int safeRequested = Math.max(0, requested);
        if (actor.getClassType() == ClassType.CAPITALIST) {
            int transferred = Math.min(safeRequested, Math.max(0, actor.getRevenue()));
            actor.setRevenue(actor.getRevenue() - transferred);
            return transferred;
        }
        int transferred = Math.min(safeRequested, Math.max(0, actor.getMoney()));
        actor.setMoney(actor.getMoney() - transferred);
        return transferred;
    }

    private int parseIntSuffix(String raw, String prefix) {
        String numericPart = raw.substring(prefix.length()).trim();
        if (numericPart.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(numericPart);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private int parseIntSafe(String raw) {
        if (raw == null) {
            return 0;
        }
        String normalized = raw.trim();
        if (normalized.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
