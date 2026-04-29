package com.example.hegemony.application;

import com.example.hegemony.application.composer.ActionPreviewDelta;
import com.example.hegemony.application.composer.ActionPreviewResult;
import com.example.hegemony.application.composer.ComposerActionTemplate;
import com.example.hegemony.application.composer.ComposerMetadata;
import com.example.hegemony.bot.BotDecision;
import com.example.hegemony.bot.LegalMoveBot;
import com.example.hegemony.domain.command.GameCommand;
import com.example.hegemony.domain.engine.ApplyCommandResult;
import com.example.hegemony.domain.engine.GameRulesEngine;
import com.example.hegemony.domain.engine.LegalMove;
import com.example.hegemony.domain.event.DomainEvent;
import com.example.hegemony.domain.model.ActionType;
import com.example.hegemony.domain.model.GameMode;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.PlayerState;
import com.example.hegemony.domain.model.PolicyState;
import com.example.hegemony.domain.model.Worker;
import com.example.hegemony.domain.rules.ValidationReasonCode;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GameService {
    private static final EnumSet<ActionType> GUIDED_ACTIONS = EnumSet.of(
            ActionType.PROPOSE_BILL,
            ActionType.ADD_VOTING_CUBES,
            ActionType.CALL_EXTRAORDINARY_VOTE,
            ActionType.ASSIGN_WORKERS,
            ActionType.BUY_GOODS_AND_SERVICES,
            ActionType.CONSUME_HEALTHCARE,
            ActionType.CONSUME_EDUCATION,
            ActionType.CONSUME_LUXURY
    );

    private final GameStateRepository repository;
    private final GameRulesEngine engine;
    private final LegalMoveBot bot;
    private final GameCommandFactory commandFactory;
    private final GameInitializer initializer;
    private final GameStateStorage storage;
    private final BotTurnService botTurnService;
    private final BusinessDealDeckManager businessDealDeckManager;
    private final ExportCardManager exportCardManager;
    private final MigrationCardManager migrationCardManager;

    public GameService(
            GameStateRepository repository,
            GameRulesEngine engine,
            LegalMoveBot bot,
            GameCommandFactory commandFactory,
            GameInitializer initializer,
            GameStateStorage storage,
            BotTurnService botTurnService,
            BusinessDealDeckManager businessDealDeckManager,
            ExportCardManager exportCardManager,
            MigrationCardManager migrationCardManager
    ) {
        this.repository = repository;
        this.engine = engine;
        this.bot = bot;
        this.commandFactory = commandFactory;
        this.initializer = initializer;
        this.storage = storage;
        this.botTurnService = botTurnService;
        this.businessDealDeckManager = businessDealDeckManager;
        this.exportCardManager = exportCardManager;
        this.migrationCardManager = migrationCardManager;
    }

    public synchronized GameState getCurrentGame() {
        GameState current = repository.get();
        ensureCardSubsystems(current);
        ensureOpeningWorkerMigration(current);
        return current.copy();
    }

    public synchronized GameState resetGame() {
        return resetGame(4, Map.of(), Map.of());
    }

    public synchronized GameState resetGame(
            int playerCount,
            Map<String, String> controlModes,
            Map<String, String> botStrategyModes
    ) {
        Map<String, Object> config = new HashMap<>();
        config.put("controlModes", controlModes == null ? Map.of() : controlModes);
        config.put("botStrategyModes", botStrategyModes == null ? Map.of() : botStrategyModes);
        GameState reset = initializer.createInitialGame(GameMode.HUMAN_ONLY, playerCount, config);
        ensureCardSubsystems(reset);
        ensureOpeningWorkerMigration(reset);
        repository.save(reset);
        return reset.copy();
    }

    public synchronized CommandExecutionResult executeCommand(ActionType actionType, Map<String, Object> parameters) {
        GameCommand command;
        try {
            command = commandFactory.create(actionType, parameters);
        } catch (Exception ex) {
            return new CommandExecutionResult(
                    false,
                    List.of("Invalid command parameters: " + ex.getMessage()),
                    List.of(ValidationReasonCode.UNSUPPORTED_ACTION),
                    List.of(),
                    repository.get().copy()
            );
        }

        ensureCardSubsystems(repository.get());
        ensureOpeningWorkerMigration(repository.get());

        ApplyCommandResult applyResult;
        try {
            applyResult = engine.apply(repository.get(), command);
        } catch (Exception ex) {
            GameState current = repository.get();
            current.appendLog("ACTION_FAILED", "action failed with exception: " + ex.getClass().getSimpleName());
            return new CommandExecutionResult(
                    false,
                    List.of("Command failed while applying state transition: " + ex.getMessage()),
                    List.of(ValidationReasonCode.UNSUPPORTED_ACTION),
                    List.of(),
                    current.copy()
            );
        }
        if (!applyResult.validation().isValid()) {
            GameState current = repository.get();
            if (!applyResult.validation().getReasonCodes().isEmpty()) {
                current.appendLog("ACTION_REJECTED", "action rejected: " + applyResult.validation().getReasonCodes());
            }
            return new CommandExecutionResult(
                    false,
                    applyResult.validation().getErrors(),
                    applyResult.validation().getReasonCodes(),
                    List.of(),
                    current.copy()
            );
        }

        ensureCardSubsystems(applyResult.resultingState());
        repository.save(applyResult.resultingState());
        return new CommandExecutionResult(
                true,
                List.of(),
                List.of(),
                applyResult.producedEvents(),
                applyResult.resultingState().copy()
        );
    }

    public synchronized List<LegalMove> getLegalMoves() {
        GameState current = repository.get();
        ensureCardSubsystems(current);
        ensureOpeningWorkerMigration(current);
        return engine.generateLegalMoves(current);
    }

    public synchronized BotMoveExecutionResult executeBotMove() {
        GameState current = repository.get();
        ensureCardSubsystems(current);
        BotDecision decision = bot.chooseMove(current, engine);
        ApplyCommandResult applyResult = engine.apply(current, decision.selectedCommand());
        if (!applyResult.validation().isValid()) {
            throw new IllegalStateException("Bot selected illegal move: " + applyResult.validation().getErrors());
        }
        ensureCardSubsystems(applyResult.resultingState());
        repository.save(applyResult.resultingState());
        return new BotMoveExecutionResult(
                decision.selectedCommand().moveId(),
                decision.selectedCommand().type(),
                decision.explanation(),
                decision.legalActionCount(),
                applyResult.producedEvents(),
                applyResult.resultingState().copy()
        );
    }

    public synchronized BotTurnExecutionResult playBotTurn() {
        ensureCardSubsystems(repository.get());
        return botTurnService.playBotTurn();
    }

    public synchronized BotUntilHumanExecutionResult playBotUntilHuman() {
        ensureCardSubsystems(repository.get());
        return botTurnService.playBotUntilHuman();
    }

    public synchronized ActionPreviewResult previewAction(
            ActionType actionType,
            Map<String, Object> parameters,
            String optionalModifier,
            String optionalCardReference
    ) {
        GameCommand command;
        try {
            command = commandFactory.create(actionType, parameters);
        } catch (Exception ex) {
            return new ActionPreviewResult(
                    false,
                    List.of("Invalid command parameters: " + ex.getMessage()),
                    List.of(ValidationReasonCode.UNSUPPORTED_ACTION),
                    new ActionPreviewDelta(),
                    List.of()
            );
        }

        GameState current = repository.get();
        ensureCardSubsystems(current);
        ensureOpeningWorkerMigration(current);
        GameState before = current.copy();

        ApplyCommandResult applyResult;
        try {
            applyResult = engine.apply(current, command);
        } catch (Exception ex) {
            return new ActionPreviewResult(
                    false,
                    List.of("Preview failed while applying projected state: " + ex.getMessage()),
                    List.of(ValidationReasonCode.UNSUPPORTED_ACTION),
                    new ActionPreviewDelta(),
                    List.of()
            );
        }
        if (!applyResult.validation().isValid()) {
            return new ActionPreviewResult(
                    false,
                    applyResult.validation().getErrors(),
                    applyResult.validation().getReasonCodes(),
                    new ActionPreviewDelta(),
                    List.of()
            );
        }

        GameState projected = applyResult.resultingState();
        ActionPreviewDelta delta = buildDelta(before, projected);

        List<String> supportNotes = new ArrayList<>();
        boolean modifierRequested = optionalModifier != null && !optionalModifier.isBlank();
        boolean cardRefRequested = optionalCardReference != null && !optionalCardReference.isBlank();
        if ((modifierRequested || cardRefRequested) && !before.getCardReadiness().isActionModifierDatasetInstalled()) {
            supportNotes.add("CARD_DATA_NOT_AVAILABLE_FOR_ACTION_MODIFIER");
        }

        return new ActionPreviewResult(
                true,
                List.of(),
                List.of(),
                delta,
                supportNotes
        );
    }

    public synchronized ComposerMetadata getComposerMetadata() {
        GameState state = repository.get();
        ensureCardSubsystems(state);
        ensureOpeningWorkerMigration(state);
        List<LegalMove> legalMoves = engine.generateLegalMoves(state);
        Map<ActionType, LegalMove> legalByType = new LinkedHashMap<>();
        for (LegalMove move : legalMoves) {
            legalByType.putIfAbsent(move.actionType(), move);
        }

        List<ComposerActionTemplate> templates = new ArrayList<>();
        for (ActionType actionType : GUIDED_ACTIONS) {
            LegalMove legalMove = legalByType.get(actionType);
            boolean supported = legalMove != null;
            templates.add(new ComposerActionTemplate(
                    actionType,
                    supported
                            ? legalMove.summary()
                            : "Action exists in supported slice but is currently illegal in this state.",
                    supported,
                    supported ? "SUPPORTED_IN_CURRENT_STATE" : "CURRENTLY_ILLEGAL_FOR_STATE",
                    supported ? legalMove.template() : Map.of(),
                    true
            ));
        }

        List<String> unavailable = List.of(
                "START_STRIKE: unsupported in current rules-engine slice.",
                "START_DEMONSTRATION: unsupported in current rules-engine slice.",
                "BUILD_ENTERPRISE: card-ready hook exists, but build execution path is not installed yet.",
                "SELL_ENTERPRISE: card-ready hook exists, but sell execution path is not installed yet."
        );

        PlayerState actor = state.currentPlayer();
        return new ComposerMetadata(
                actor == null ? "" : actor.getPlayerId(),
                templates,
                false,
                "No action modifiers available: card/modifier dataset is not installed yet.",
                unavailable
        );
    }

    public synchronized List<com.example.hegemony.bot.planning.MarketCandidate> getCurrentMarketCandidates() {
        ensureCardSubsystems(repository.get());
        return botTurnService.currentMarketCandidates();
    }

    public synchronized String saveToJson(String fileName) {
        String resolved = (fileName == null || fileName.isBlank()) ? "demo-save.json" : fileName;
        ensureCardSubsystems(repository.get());
        return storage.save(repository.get(), resolved);
    }

    public synchronized GameState loadFromJson(String fileName) {
        String resolved = (fileName == null || fileName.isBlank()) ? "demo-save.json" : fileName;
        GameState loaded = storage.load(resolved);
        ensureCardSubsystems(loaded);
        ensureOpeningWorkerMigration(loaded);
        repository.save(loaded);
        return loaded.copy();
    }

    private void ensureCardSubsystems(GameState state) {
        businessDealDeckManager.ensureInitialized(state);
        exportCardManager.ensureInitialized(state);
        migrationCardManager.ensureInitialized(state);
    }

    private void ensureOpeningWorkerMigration(GameState state) {
        List<DomainEvent> events = engine.applyOpeningWorkerMigrationIfNeeded(state);
        if (!events.isEmpty()) {
            repository.save(state);
        }
    }

    private ActionPreviewDelta buildDelta(GameState before, GameState after) {
        ActionPreviewDelta delta = new ActionPreviewDelta();
        Map<String, Integer> moneyDelta = new HashMap<>();
        Map<String, Integer> welfareDelta = new HashMap<>();
        Map<String, Integer> influenceDelta = new HashMap<>();
        Map<String, Integer> vpDelta = new HashMap<>();
        Map<String, Integer> tokenDelta = new HashMap<>();
        Map<String, Map<String, Integer>> resourceDelta = new HashMap<>();

        Map<String, PlayerState> beforePlayers = indexPlayers(before);
        Map<String, PlayerState> afterPlayers = indexPlayers(after);
        Set<String> playerIds = new HashSet<>();
        playerIds.addAll(beforePlayers.keySet());
        playerIds.addAll(afterPlayers.keySet());

        for (String playerId : playerIds) {
            PlayerState b = beforePlayers.get(playerId);
            PlayerState a = afterPlayers.get(playerId);
            if (b == null || a == null) {
                continue;
            }
            putIfNonZero(moneyDelta, playerId, a.getMoney() - b.getMoney());
            putIfNonZero(welfareDelta, playerId, a.getWelfare() - b.getWelfare());
            putIfNonZero(influenceDelta, playerId, a.getInfluence() - b.getInfluence());
            putIfNonZero(vpDelta, playerId, a.getVictoryPoints() - b.getVictoryPoints());
            putIfNonZero(tokenDelta, playerId, a.availableProposalTokens() - b.availableProposalTokens());

            Map<String, Integer> playerResourceDelta = new HashMap<>();
            Set<String> resources = new HashSet<>();
            resources.addAll(b.getResources().keySet());
            resources.addAll(a.getResources().keySet());
            for (String resource : resources) {
                int amount = a.getResourceAmount(resource) - b.getResourceAmount(resource);
                putIfNonZero(playerResourceDelta, resource, amount);
            }
            if (!playerResourceDelta.isEmpty()) {
                resourceDelta.put(playerId, playerResourceDelta);
            }
        }

        Map<String, Worker> beforeWorkers = indexWorkers(before);
        Map<String, Worker> afterWorkers = indexWorkers(after);
        Map<String, String> workerMovement = new HashMap<>();
        for (Map.Entry<String, Worker> entry : beforeWorkers.entrySet()) {
            Worker afterWorker = afterWorkers.get(entry.getKey());
            if (afterWorker == null) {
                continue;
            }
            Worker beforeWorker = entry.getValue();
            String beforeLoc = describeWorkerLocation(beforeWorker);
            String afterLoc = describeWorkerLocation(afterWorker);
            if (!beforeLoc.equals(afterLoc)) {
                workerMovement.put(beforeWorker.getId(), beforeLoc + " -> " + afterLoc);
            }
        }

        Map<String, PolicyState> beforePolicies = indexPolicies(before);
        Map<String, PolicyState> afterPolicies = indexPolicies(after);
        Map<String, String> policyDelta = new HashMap<>();
        for (Map.Entry<String, PolicyState> entry : beforePolicies.entrySet()) {
            PolicyState afterPolicy = afterPolicies.get(entry.getKey());
            if (afterPolicy == null) {
                continue;
            }
            PolicyState beforePolicy = entry.getValue();
            if (beforePolicy.getCurrentCourse() != afterPolicy.getCurrentCourse()) {
                policyDelta.put(beforePolicy.getId().name(),
                        beforePolicy.getCurrentCourse().name() + " -> " + afterPolicy.getCurrentCourse().name());
            }
        }

        delta.setMoneyDeltaByPlayer(moneyDelta);
        delta.setWelfareDeltaByPlayer(welfareDelta);
        delta.setInfluenceDeltaByPlayer(influenceDelta);
        delta.setVictoryPointDeltaByPlayer(vpDelta);
        delta.setProposalTokenDeltaByPlayer(tokenDelta);
        delta.setResourceDeltaByPlayer(resourceDelta);
        delta.setWorkerMovement(workerMovement);
        delta.setPolicyDelta(policyDelta);
        return delta;
    }

    private Map<String, PlayerState> indexPlayers(GameState state) {
        Map<String, PlayerState> index = new HashMap<>();
        for (PlayerState player : state.getPlayers()) {
            index.put(player.getPlayerId(), player);
        }
        return index;
    }

    private Map<String, Worker> indexWorkers(GameState state) {
        Map<String, Worker> index = new HashMap<>();
        for (Worker worker : state.getWorkers()) {
            index.put(worker.getId(), worker);
        }
        return index;
    }

    private Map<String, PolicyState> indexPolicies(GameState state) {
        Map<String, PolicyState> index = new HashMap<>();
        for (PolicyState policy : state.getPolicies()) {
            index.put(policy.getId().name(), policy);
        }
        return index;
    }

    private String describeWorkerLocation(Worker worker) {
        if (worker.getLocation() == null) {
            return "UNKNOWN";
        }
        if (worker.getLocation().name().equals("ENTERPRISE_SLOT")) {
            return worker.getLocation().name() + ":" + worker.getEnterpriseId() + ":" + worker.getSlotId();
        }
        return worker.getLocation().name();
    }

    private void putIfNonZero(Map<String, Integer> map, String key, int value) {
        if (value != 0) {
            map.put(key, value);
        }
    }
}
