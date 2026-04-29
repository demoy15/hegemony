package com.example.hegemony.domain;

import com.example.hegemony.application.GameInitializer;
import com.example.hegemony.application.BusinessDealDeckManager;
import com.example.hegemony.application.ExportCardManager;
import com.example.hegemony.application.MigrationCardManager;
import com.example.hegemony.application.setup.NoOpAutomaSetupHook;
import com.example.hegemony.application.setup.SetupSpecLoader;
import com.example.hegemony.domain.card.CardCatalog;
import com.example.hegemony.domain.card.CardDefinition;
import com.example.hegemony.domain.carddata.BusinessDealCardCatalog;
import com.example.hegemony.domain.card.DeclarativeCardEffectProcessor;
import com.example.hegemony.domain.command.AdvanceToVotingCommand;
import com.example.hegemony.domain.command.AdvanceToNextRoundCommand;
import com.example.hegemony.domain.command.AdvanceToScoringCommand;
import com.example.hegemony.domain.command.EndTurnCommand;
import com.example.hegemony.domain.command.CommitVoteInfluenceCommand;
import com.example.hegemony.domain.command.DeclareVoteStanceCommand;
import com.example.hegemony.domain.command.ProposeBillCommand;
import com.example.hegemony.domain.command.ResolveScoringPhaseCommand;
import com.example.hegemony.domain.command.ResolveProductionPhaseCommand;
import com.example.hegemony.domain.engine.GameRulesEngine;
import com.example.hegemony.domain.model.CurrentVoteState;
import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.Enterprise;
import com.example.hegemony.domain.model.GameMode;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.PolicyState;
import com.example.hegemony.domain.model.RoundPhase;
import com.example.hegemony.domain.model.VoteStance;
import com.example.hegemony.domain.model.Worker;
import com.example.hegemony.domain.model.WorkerLocation;
import com.example.hegemony.domain.model.WorkerQualification;
import com.example.hegemony.infrastructure.JsonBusinessDealCardCatalog;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CoreTestSupport {
    private CoreTestSupport() {
    }

    public static GameInitializer initializer() {
        return new GameInitializer(
                new SetupSpecLoader("./missing-do-not-use.yaml"),
                new NoOpAutomaSetupHook(),
                new BusinessDealDeckManager(businessDealCatalog()),
                new ExportCardManager(new ObjectMapper()),
                new MigrationCardManager(new ObjectMapper())
        );
    }

    public static GameRulesEngine engine() {
        CardCatalog emptyCatalog = new CardCatalog() {
            @Override
            public Optional<CardDefinition> findById(String cardId) {
                return Optional.empty();
            }

            @Override
            public List<CardDefinition> listAll() {
                return List.of();
            }
        };
        return new GameRulesEngine(
                emptyCatalog,
                new DeclarativeCardEffectProcessor(Map.of()),
                new BusinessDealDeckManager(businessDealCatalog()),
                new ExportCardManager(new ObjectMapper()),
                new MigrationCardManager(new ObjectMapper())
        );
    }

    private static BusinessDealCardCatalog businessDealCatalog() {
        return new JsonBusinessDealCardCatalog(new ObjectMapper());
    }

    public static GameState state(int playerCount) {
        return initializer().createInitialGame(GameMode.HUMAN_ONLY, playerCount, Map.of());
    }

    public static PolicyState policy(GameState state, PolicyId policyId) {
        return state.findPolicy(policyId).orElseThrow();
    }

    public static Enterprise firstEmptyEnterpriseWithAtLeastSlots(GameState state, int minSlots) {
        return state.getEnterprises().stream()
                .filter(Enterprise::isFullyEmpty)
                .filter(e -> e.getSlots().size() >= minSlots)
                .filter(e -> e.getSlots().stream().allMatch(slot -> slot.getRequiredQualification() == WorkerQualification.UNSKILLED))
                .findFirst()
                .orElseGet(() -> state.getEnterprises().stream()
                        .filter(Enterprise::isFullyEmpty)
                        .filter(e -> e.getSlots().size() >= minSlots)
                        .findFirst()
                        .orElseThrow());
    }

    public static List<Worker> unemployedWorkers(GameState state, ClassType classType) {
        return state.getWorkers().stream()
                .filter(w -> w.getClassType() == classType)
                .filter(w -> w.getLocation() == WorkerLocation.UNEMPLOYED)
                .filter(w -> !w.isTiedContract())
                .toList();
    }

    public static Worker addUnemployedWorker(GameState state, String id, ClassType classType, WorkerQualification qualification) {
        Worker worker = new Worker();
        worker.setId(id);
        worker.setClassType(classType);
        worker.setQualificationType(qualification);
        worker.setLocation(WorkerLocation.UNEMPLOYED);
        worker.setTiedContract(false);
        state.getWorkers().add(worker);
        state.refreshLegacyPlayerSnapshots();
        return worker;
    }

    public static GameState stateWithPendingVote(int playerCount, PolicyId policyId, PolicyCourse targetCourse) {
        GameRulesEngine engine = engine();
        GameState state = state(playerCount);

        var proposed = engine.apply(state, new ProposeBillCommand("worker", policyId, targetCourse));
        if (!proposed.validation().isValid()) {
            throw new IllegalStateException("Failed to prepare pending proposal for test: " + proposed.validation().getErrors());
        }

        GameState production = exhaustActionPhase(proposed.resultingState(), engine);
        GameState resolvedProduction = resolveProduction(production, engine);
        resolvedProduction.getVotingBag().setWorker(0);
        resolvedProduction.getVotingBag().setMiddleClass(0);
        resolvedProduction.getVotingBag().setCapitalist(0);
        var advanced = engine.apply(resolvedProduction, new AdvanceToVotingCommand(resolvedProduction.currentPlayer().getPlayerId()));
        if (!advanced.validation().isValid()) {
            throw new IllegalStateException("Failed to advance to voting for test: " + advanced.validation().getErrors());
        }

        return advanced.resultingState();
    }

    public static GameState declareAllMissingStances(GameState state, GameRulesEngine engine, VoteStance defaultStance) {
        GameState current = state;
        CurrentVoteState session = current.getCurrentVoteState();
        if (session == null) {
            return current;
        }

        for (var player : current.getPlayers()) {
            if (session.getStanceByPlayer().containsKey(player.getPlayerId())) {
                continue;
            }
            var applied = engine.apply(current, new DeclareVoteStanceCommand(
                    player.getPlayerId(),
                    session.getActiveProposalPolicyId(),
                    defaultStance.name()
            ));
            if (!applied.validation().isValid()) {
                throw new IllegalStateException("Failed to declare stance in helper: " + applied.validation().getErrors());
            }
            current = applied.resultingState();
            session = current.getCurrentVoteState();
            if (session == null) {
                break;
            }
        }

        return current;
    }

    public static GameState commitZeroInfluenceForAll(GameState state, GameRulesEngine engine) {
        GameState current = state;
        for (var player : current.getPlayers()) {
            if (current.getCurrentVoteState() == null) {
                break;
            }
            if (current.getCurrentVoteState().getInfluenceCommitments().containsKey(player.getPlayerId())) {
                continue;
            }
            var applied = engine.apply(current, new CommitVoteInfluenceCommand(player.getPlayerId(), 0));
            if (!applied.validation().isValid()) {
                throw new IllegalStateException("Failed zero influence commit in helper: " + applied.validation().getErrors());
            }
            current = applied.resultingState();
        }
        return current;
    }

    public static GameState stateInProductionPhase(int playerCount) {
        GameRulesEngine engine = engine();
        return exhaustActionPhase(state(playerCount), engine);
    }

    public static GameState exhaustActionPhase(GameState state, GameRulesEngine engine) {
        GameState current = state;
        int guard = 0;
        while (current.getCurrentPhase() == RoundPhase.ACTIONS && !current.getTurnOrder().allPlayersCompletedActions() && guard++ < 64) {
            var ended = engine.apply(current, new EndTurnCommand());
            if (!ended.validation().isValid()) {
                throw new IllegalStateException("Failed to advance action turn in helper: " + ended.validation().getErrors());
            }
            current = ended.resultingState();
        }
        if (guard >= 64) {
            throw new IllegalStateException("Action phase helper exceeded guard while exhausting turns.");
        }
        return current;
    }

    public static GameState resolveProduction(GameState state, GameRulesEngine engine) {
        if (state.getTurnOrder().getPhase() != RoundPhase.PRODUCTION) {
            throw new IllegalStateException("State is not in production phase.");
        }
        String actor = state.currentPlayer() == null ? "worker" : state.currentPlayer().getPlayerId();
        var resolved = engine.apply(state, new ResolveProductionPhaseCommand(actor));
        if (!resolved.validation().isValid()) {
            throw new IllegalStateException("Failed to resolve production: " + resolved.validation().getErrors());
        }
        return resolved.resultingState();
    }

    public static GameState advanceRound(GameState state, GameRulesEngine engine) {
        String actor = state.currentPlayer() == null ? "worker" : state.currentPlayer().getPlayerId();
        GameState current = state;
        if (current.getTurnOrder().getPhase() == RoundPhase.PRODUCTION && (current.getProductionPhaseState() == null || !current.getProductionPhaseState().isProductionResolved())) {
            current = engine.apply(current, new ResolveProductionPhaseCommand(actor)).resultingState();
        }
        if (current.getTurnOrder().getPhase() == RoundPhase.PRODUCTION) {
            current = engine.apply(current, new AdvanceToVotingCommand(actor)).resultingState();
        }
        if (current.getTurnOrder().getPhase() == RoundPhase.VOTING && current.getCurrentVoteState() == null) {
            current = engine.apply(current, new AdvanceToScoringCommand(actor)).resultingState();
        }
        if (current.getTurnOrder().getPhase() == RoundPhase.SCORING && (current.getLastScoringSummary() == null || current.getLastScoringSummary().getRound() != current.getCurrentRound())) {
            current = engine.apply(current, new ResolveScoringPhaseCommand(actor)).resultingState();
        }
        var advanced = engine.apply(current, new AdvanceToNextRoundCommand(actor));
        if (!advanced.validation().isValid()) {
            throw new IllegalStateException("Failed to advance round: " + advanced.validation().getErrors());
        }
        return advanced.resultingState();
    }
}
