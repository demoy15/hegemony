package com.example.hegemony.domain.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GameState {
    private List<PlayerState> players = new ArrayList<>();
    private List<PolicyState> policies = new ArrayList<>();
    private List<Enterprise> enterprises = new ArrayList<>();
    private List<Worker> workers = new ArrayList<>();
    private MarketState market = new MarketState();
    private TurnOrderState turnOrder = new TurnOrderState();
    private int treasury;
    private int stateLoans;
    private PublicServicesState publicServices = new PublicServicesState();
    private Map<String, Integer> publicServicesStorage = new HashMap<>();
    private VotingBagState votingBag = new VotingBagState();
    private CurrentVoteState currentVoteState;
    private ProposalResolutionResult lastProposalResolution;
    private ProductionPhaseState productionPhaseState;
    private ProductionPhaseState lastProductionSummary;
    private int roundMarker = 1;
    private int taxMultiplier = 5;
    private List<EventLogEntry> eventLog = new ArrayList<>();
    private long nextLogId = 1;
    private boolean demoMode;
    private boolean demonstrationToken;
    private Map<String, Integer> demonstrationPenaltyAllocation = new HashMap<>();

    private int maxRounds = 5;
    private GameStatus gameStatus = GameStatus.IN_PROGRESS;
    private PreparationSummary lastPreparationSummary;
    private ScoringSummary lastScoringSummary;
    private List<PlayerScoringBreakdown> scoringBreakdown = new ArrayList<>();
    private RoundSummary lastRoundSummary;
    private FinalResult finalResult;
    private List<String> lifecycleUnsupportedNotes = new ArrayList<>();
    private List<String> economyUnsupportedNotes = new ArrayList<>();
    private CardReadinessState cardReadiness = new CardReadinessState();
    private BotTurnSummary lastBotTurnSummary;
    private List<BusinessDealCard> businessDealCards = new ArrayList<>();
    private OrderedCardDeckState businessDealDeck = new OrderedCardDeckState();
    private List<ExportCardState> exportCards = new ArrayList<>();
    private OrderedCardDeckState exportCardDeck = new OrderedCardDeckState();
    private List<MigrationCardState> migrationCards = new ArrayList<>();
    private OrderedCardDeckState migrationDeck = new OrderedCardDeckState();
    private List<Map<String, Object>> stateEventCards = new ArrayList<>();
    private OrderedCardDeckState stateEventDeck = new OrderedCardDeckState();
    private OrderedCardDeckState capitalistAutomaDeck = new OrderedCardDeckState();
    private OrderedCardDeckState workerAutomaDeck = new OrderedCardDeckState();
    private ExportCardState activeExportCard = new ExportCardState();
    private List<Enterprise> capitalistEnterpriseDeck = new ArrayList<>();
    private List<Enterprise> capitalistEnterpriseMarket = new ArrayList<>();
    private List<Enterprise> middleClassEnterpriseDeck = new ArrayList<>();
    private List<Enterprise> middleClassEnterpriseMarket = new ArrayList<>();

    public GameState() {
    }

    public GameState copy() {
        GameState copy = new GameState();
        copy.players = players.stream().map(PlayerState::copy).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        copy.policies = policies.stream().map(PolicyState::copy).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        copy.enterprises = enterprises.stream().map(Enterprise::copy).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        copy.workers = workers.stream().map(Worker::copy).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        copy.market = market == null ? new MarketState() : market.copy();
        copy.turnOrder = turnOrder == null ? new TurnOrderState() : turnOrder.copy();
        copy.treasury = treasury;
        copy.stateLoans = stateLoans;
        copy.publicServices = publicServices == null ? new PublicServicesState() : publicServices.copy();
        copy.publicServicesStorage = new HashMap<>(publicServicesStorage == null ? Map.of() : publicServicesStorage);
        copy.votingBag = votingBag == null ? new VotingBagState() : votingBag.copy();
        copy.currentVoteState = currentVoteState == null ? null : currentVoteState.copy();
        copy.lastProposalResolution = lastProposalResolution == null ? null : lastProposalResolution.copy();
        copy.productionPhaseState = productionPhaseState == null ? null : productionPhaseState.copy();
        copy.lastProductionSummary = lastProductionSummary == null ? null : lastProductionSummary.copy();
        copy.roundMarker = roundMarker;
        copy.taxMultiplier = taxMultiplier;
        copy.eventLog = eventLog.stream()
                .map(entry -> new EventLogEntry(entry.getId(), entry.getType(), entry.getMessage()))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        copy.nextLogId = nextLogId;
        copy.demoMode = demoMode;
        copy.demonstrationToken = demonstrationToken;
        copy.demonstrationPenaltyAllocation = new HashMap<>(demonstrationPenaltyAllocation == null ? Map.of() : demonstrationPenaltyAllocation);
        copy.maxRounds = maxRounds;
        copy.gameStatus = gameStatus;
        copy.lastPreparationSummary = lastPreparationSummary == null ? null : lastPreparationSummary.copy();
        copy.lastScoringSummary = lastScoringSummary == null ? null : lastScoringSummary.copy();
        copy.scoringBreakdown = scoringBreakdown.stream()
                .map(PlayerScoringBreakdown::copy)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        copy.lastRoundSummary = lastRoundSummary == null ? null : lastRoundSummary.copy();
        copy.finalResult = finalResult == null ? null : finalResult.copy();
        copy.lifecycleUnsupportedNotes = new ArrayList<>(lifecycleUnsupportedNotes == null ? List.of() : lifecycleUnsupportedNotes);
        copy.economyUnsupportedNotes = new ArrayList<>(economyUnsupportedNotes == null ? List.of() : economyUnsupportedNotes);
        copy.cardReadiness = cardReadiness == null ? new CardReadinessState() : cardReadiness.copy();
        copy.lastBotTurnSummary = lastBotTurnSummary == null ? null : lastBotTurnSummary.copy();
        copy.businessDealCards = businessDealCards.stream()
                .map(BusinessDealCard::copy)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        copy.businessDealDeck = businessDealDeck == null ? new OrderedCardDeckState() : businessDealDeck.copy();
        copy.exportCards = exportCards.stream()
                .map(ExportCardState::copy)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        copy.exportCardDeck = exportCardDeck == null ? new OrderedCardDeckState() : exportCardDeck.copy();
        copy.migrationCards = migrationCards.stream()
                .map(MigrationCardState::copy)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        copy.migrationDeck = migrationDeck == null ? new OrderedCardDeckState() : migrationDeck.copy();
        copy.stateEventCards = (stateEventCards == null ? List.<Map<String, Object>>of() : stateEventCards).stream()
                .map(HashMap::new)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        copy.stateEventDeck = stateEventDeck == null ? new OrderedCardDeckState() : stateEventDeck.copy();
        copy.capitalistAutomaDeck = capitalistAutomaDeck == null ? new OrderedCardDeckState() : capitalistAutomaDeck.copy();
        copy.workerAutomaDeck = workerAutomaDeck == null ? new OrderedCardDeckState() : workerAutomaDeck.copy();
        copy.activeExportCard = activeExportCard == null ? new ExportCardState() : activeExportCard.copy();
        copy.capitalistEnterpriseDeck = (capitalistEnterpriseDeck == null ? List.<Enterprise>of() : capitalistEnterpriseDeck).stream()
                .map(Enterprise::copy)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        copy.capitalistEnterpriseMarket = (capitalistEnterpriseMarket == null ? List.<Enterprise>of() : capitalistEnterpriseMarket).stream()
                .map(Enterprise::copy)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        copy.middleClassEnterpriseDeck = (middleClassEnterpriseDeck == null ? List.<Enterprise>of() : middleClassEnterpriseDeck).stream()
                .map(Enterprise::copy)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        copy.middleClassEnterpriseMarket = (middleClassEnterpriseMarket == null ? List.<Enterprise>of() : middleClassEnterpriseMarket).stream()
                .map(Enterprise::copy)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        return copy;
    }

    public PlayerState currentPlayer() {
        if (turnOrder.getCurrentPlayerIndex() < 0 || turnOrder.getCurrentPlayerIndex() >= players.size()) {
            return null;
        }
        return players.get(turnOrder.getCurrentPlayerIndex());
    }

    public Optional<PlayerState> findPlayerById(String playerId) {
        return players.stream().filter(p -> p.getPlayerId().equals(playerId)).findFirst();
    }

    public Optional<PolicyState> findPolicy(PolicyId policyId) {
        return policies.stream().filter(policy -> policy.getId() == policyId).findFirst();
    }

    public List<PolicyState> pendingPoliciesInOrder() {
        return policies.stream()
                .filter(policy -> policy.getOccupyingProposalToken() != null)
                .sorted(java.util.Comparator.comparingInt(policy -> policy.getId().ordinal()))
                .toList();
    }

    public Optional<Worker> findWorker(String workerId) {
        return workers.stream().filter(worker -> worker.getId().equals(workerId)).findFirst();
    }

    public Optional<Enterprise> findEnterprise(String enterpriseId) {
        return enterprises.stream().filter(enterprise -> enterprise.getId().equals(enterpriseId)).findFirst();
    }

    public Optional<BusinessDealCard> findBusinessDealCard(String cardId) {
        return businessDealCards.stream().filter(card -> card.getId().equals(cardId)).findFirst();
    }

    public void appendLog(String type, String message) {
        eventLog.add(new EventLogEntry(nextLogId++, type, message));
    }

    public void refreshLegacyPlayerSnapshots() {
        for (PlayerState player : players) {
            ClassType classType = player.getClassType();
            long unemployed = workers.stream()
                    .filter(w -> w.getClassType() == classType && w.getLocation() == WorkerLocation.UNEMPLOYED)
                    .count();
            long employed = workers.stream()
                    .filter(w -> w.getClassType() == classType && w.getLocation() == WorkerLocation.ENTERPRISE_SLOT)
                    .count();
            long ownedEnterprises = enterprises.stream().filter(e -> e.getOwnerClass() == classType).count();

            player.setAvailableWorkers((int) unemployed);
            player.setEmployedWorkers((int) employed);
            player.setEnterprises((int) ownedEnterprises);
        }
    }

    public int getCurrentRound() {
        return turnOrder == null ? roundMarker : turnOrder.getRound();
    }

    public void setCurrentRound(int currentRound) {
        if (turnOrder == null) {
            turnOrder = new TurnOrderState();
        }
        turnOrder.setRound(currentRound);
        roundMarker = currentRound;
    }

    public RoundPhase getCurrentPhase() {
        return turnOrder == null ? RoundPhase.ACTIONS : turnOrder.getPhase();
    }

    public void setCurrentPhase(RoundPhase currentPhase) {
        if (turnOrder == null) {
            turnOrder = new TurnOrderState();
        }
        turnOrder.setPhase(currentPhase);
    }

    public boolean isGameOver() {
        return getGameStatus() == GameStatus.FINISHED || getCurrentPhase() == RoundPhase.GAME_OVER;
    }

    public void setGameOver(boolean gameOver) {
        if (gameOver) {
            setGameStatus(GameStatus.FINISHED);
            setCurrentPhase(RoundPhase.GAME_OVER);
        } else {
            if (getCurrentPhase() == RoundPhase.GAME_OVER) {
                setCurrentPhase(RoundPhase.ACTIONS);
            }
            setGameStatus(GameStatus.IN_PROGRESS);
        }
    }

    public List<PlayerState> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerState> players) {
        this.players = players == null ? new ArrayList<>() : players;
    }

    public List<PolicyState> getPolicies() {
        return policies;
    }

    public void setPolicies(List<PolicyState> policies) {
        this.policies = policies == null ? new ArrayList<>() : policies;
    }

    public List<Enterprise> getEnterprises() {
        return enterprises;
    }

    public void setEnterprises(List<Enterprise> enterprises) {
        this.enterprises = enterprises == null ? new ArrayList<>() : enterprises;
    }

    public List<Worker> getWorkers() {
        return workers;
    }

    public void setWorkers(List<Worker> workers) {
        this.workers = workers == null ? new ArrayList<>() : workers;
    }

    public MarketState getMarket() {
        return market;
    }

    public void setMarket(MarketState market) {
        this.market = market;
    }

    public TurnOrderState getTurnOrder() {
        return turnOrder;
    }

    public void setTurnOrder(TurnOrderState turnOrder) {
        this.turnOrder = turnOrder == null ? new TurnOrderState() : turnOrder;
    }

    // Backward-compatible alias used by existing API/frontend code.
    public TurnOrderState getTurn() {
        return turnOrder;
    }

    public void setTurn(TurnOrderState turnOrder) {
        this.turnOrder = turnOrder == null ? new TurnOrderState() : turnOrder;
    }

    public int getTreasury() {
        return treasury;
    }

    public void setTreasury(int treasury) {
        this.treasury = treasury;
    }

    public int getStateLoans() {
        return stateLoans;
    }

    public void setStateLoans(int stateLoans) {
        this.stateLoans = Math.max(0, stateLoans);
    }

    public PublicServicesState getPublicServices() {
        return publicServices;
    }

    public void setPublicServices(PublicServicesState publicServices) {
        this.publicServices = publicServices == null ? new PublicServicesState() : publicServices;
        syncPublicServicesStorageFromBoard();
    }

    public Map<String, Integer> getPublicServicesStorage() {
        if ((publicServicesStorage == null || publicServicesStorage.isEmpty()) && publicServices != null) {
            syncPublicServicesStorageFromBoard();
        }
        return publicServicesStorage;
    }

    public void setPublicServicesStorage(Map<String, Integer> publicServicesStorage) {
        this.publicServicesStorage = publicServicesStorage == null ? new HashMap<>() : new HashMap<>(publicServicesStorage);
        syncPublicServicesBoardFromStorage();
    }

    public int getPublicServiceAmount(String resourceId) {
        if (resourceId == null || resourceId.isBlank()) {
            return 0;
        }
        return publicServicesStorage.getOrDefault(resourceId.toLowerCase(), 0);
    }

    public void addPublicServiceAmount(String resourceId, int amount) {
        if (resourceId == null || resourceId.isBlank() || amount <= 0) {
            return;
        }
        String key = resourceId.toLowerCase();
        publicServicesStorage.put(key, getPublicServiceAmount(key) + amount);
        syncPublicServicesBoardFromStorage();
    }

    public int consumePublicServiceAmount(String resourceId, int amount) {
        if (resourceId == null || resourceId.isBlank() || amount <= 0) {
            return 0;
        }
        String key = resourceId.toLowerCase();
        int available = getPublicServiceAmount(key);
        int consumed = Math.min(available, amount);
        publicServicesStorage.put(key, available - consumed);
        syncPublicServicesBoardFromStorage();
        return consumed;
    }

    public VotingBagState getVotingBag() {
        return votingBag;
    }

    public void setVotingBag(VotingBagState votingBag) {
        this.votingBag = votingBag == null ? new VotingBagState() : votingBag;
    }

    public CurrentVoteState getCurrentVoteState() {
        return currentVoteState;
    }

    public void setCurrentVoteState(CurrentVoteState currentVoteState) {
        this.currentVoteState = currentVoteState;
    }

    public ProposalResolutionResult getLastProposalResolution() {
        return lastProposalResolution;
    }

    public void setLastProposalResolution(ProposalResolutionResult lastProposalResolution) {
        this.lastProposalResolution = lastProposalResolution;
    }

    public ProductionPhaseState getProductionPhaseState() {
        return productionPhaseState;
    }

    public void setProductionPhaseState(ProductionPhaseState productionPhaseState) {
        this.productionPhaseState = productionPhaseState;
    }

    public ProductionPhaseState getLastProductionSummary() {
        return lastProductionSummary;
    }

    public void setLastProductionSummary(ProductionPhaseState lastProductionSummary) {
        this.lastProductionSummary = lastProductionSummary;
    }

    public int getRoundMarker() {
        return roundMarker;
    }

    public void setRoundMarker(int roundMarker) {
        this.roundMarker = roundMarker;
    }

    public int getTaxMultiplier() {
        return taxMultiplier;
    }

    public void setTaxMultiplier(int taxMultiplier) {
        this.taxMultiplier = taxMultiplier;
    }

    public List<EventLogEntry> getEventLog() {
        return eventLog;
    }

    public void setEventLog(List<EventLogEntry> eventLog) {
        this.eventLog = eventLog == null ? new ArrayList<>() : eventLog;
    }

    public long getNextLogId() {
        return nextLogId;
    }

    public void setNextLogId(long nextLogId) {
        this.nextLogId = nextLogId;
    }

    public boolean isDemoMode() {
        return demoMode;
    }

    public void setDemoMode(boolean demoMode) {
        this.demoMode = demoMode;
    }

    public boolean isDemonstrationToken() {
        return demonstrationToken;
    }

    public void setDemonstrationToken(boolean demonstrationToken) {
        this.demonstrationToken = demonstrationToken;
    }

    public Map<String, Integer> getDemonstrationPenaltyAllocation() {
        if (demonstrationPenaltyAllocation == null) {
            demonstrationPenaltyAllocation = new HashMap<>();
        }
        return demonstrationPenaltyAllocation;
    }

    public void setDemonstrationPenaltyAllocation(Map<String, Integer> demonstrationPenaltyAllocation) {
        this.demonstrationPenaltyAllocation = demonstrationPenaltyAllocation == null
                ? new HashMap<>()
                : new HashMap<>(demonstrationPenaltyAllocation);
    }

    public int getMaxRounds() {
        return maxRounds;
    }

    public void setMaxRounds(int maxRounds) {
        this.maxRounds = maxRounds <= 0 ? 5 : maxRounds;
    }

    public GameStatus getGameStatus() {
        return gameStatus == null ? GameStatus.IN_PROGRESS : gameStatus;
    }

    public void setGameStatus(GameStatus gameStatus) {
        this.gameStatus = gameStatus == null ? GameStatus.IN_PROGRESS : gameStatus;
    }

    public PreparationSummary getLastPreparationSummary() {
        return lastPreparationSummary;
    }

    public void setLastPreparationSummary(PreparationSummary lastPreparationSummary) {
        this.lastPreparationSummary = lastPreparationSummary;
    }

    public ScoringSummary getLastScoringSummary() {
        return lastScoringSummary;
    }

    public void setLastScoringSummary(ScoringSummary lastScoringSummary) {
        this.lastScoringSummary = lastScoringSummary;
    }

    public List<PlayerScoringBreakdown> getScoringBreakdown() {
        return scoringBreakdown;
    }

    public void setScoringBreakdown(List<PlayerScoringBreakdown> scoringBreakdown) {
        this.scoringBreakdown = scoringBreakdown == null ? new ArrayList<>() : scoringBreakdown;
    }

    public RoundSummary getLastRoundSummary() {
        return lastRoundSummary;
    }

    public void setLastRoundSummary(RoundSummary lastRoundSummary) {
        this.lastRoundSummary = lastRoundSummary;
    }

    public FinalResult getFinalResult() {
        return finalResult;
    }

    public void setFinalResult(FinalResult finalResult) {
        this.finalResult = finalResult;
    }

    public List<String> getLifecycleUnsupportedNotes() {
        if (lifecycleUnsupportedNotes == null) {
            lifecycleUnsupportedNotes = new ArrayList<>();
        }
        return lifecycleUnsupportedNotes;
    }

    public void setLifecycleUnsupportedNotes(List<String> lifecycleUnsupportedNotes) {
        this.lifecycleUnsupportedNotes = lifecycleUnsupportedNotes == null ? new ArrayList<>() : lifecycleUnsupportedNotes;
    }

    public List<String> getEconomyUnsupportedNotes() {
        if (economyUnsupportedNotes == null) {
            economyUnsupportedNotes = new ArrayList<>();
        }
        return economyUnsupportedNotes;
    }

    public void setEconomyUnsupportedNotes(List<String> economyUnsupportedNotes) {
        this.economyUnsupportedNotes = economyUnsupportedNotes == null ? new ArrayList<>() : economyUnsupportedNotes;
    }

    public CardReadinessState getCardReadiness() {
        return cardReadiness == null ? new CardReadinessState() : cardReadiness;
    }

    public void setCardReadiness(CardReadinessState cardReadiness) {
        this.cardReadiness = cardReadiness == null ? new CardReadinessState() : cardReadiness;
    }

    public BotTurnSummary getLastBotTurnSummary() {
        return lastBotTurnSummary;
    }

    public void setLastBotTurnSummary(BotTurnSummary lastBotTurnSummary) {
        this.lastBotTurnSummary = lastBotTurnSummary;
    }

    public List<BusinessDealCard> getBusinessDealCards() {
        return businessDealCards == null ? new ArrayList<>() : businessDealCards;
    }

    public void setBusinessDealCards(List<BusinessDealCard> businessDealCards) {
        this.businessDealCards = businessDealCards == null
                ? new ArrayList<>()
                : businessDealCards.stream().map(BusinessDealCard::copy).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public OrderedCardDeckState getBusinessDealDeck() {
        return businessDealDeck == null ? new OrderedCardDeckState() : businessDealDeck;
    }

    public void setBusinessDealDeck(OrderedCardDeckState businessDealDeck) {
        this.businessDealDeck = businessDealDeck == null ? new OrderedCardDeckState() : businessDealDeck;
    }

    public ExportCardState getActiveExportCard() {
        return activeExportCard == null ? new ExportCardState() : activeExportCard;
    }

    public void setActiveExportCard(ExportCardState activeExportCard) {
        this.activeExportCard = activeExportCard == null ? new ExportCardState() : activeExportCard;
    }

    public List<ExportCardState> getExportCards() {
        return exportCards == null ? new ArrayList<>() : exportCards;
    }

    public void setExportCards(List<ExportCardState> exportCards) {
        this.exportCards = exportCards == null
                ? new ArrayList<>()
                : exportCards.stream().map(ExportCardState::copy).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public OrderedCardDeckState getExportCardDeck() {
        return exportCardDeck == null ? new OrderedCardDeckState() : exportCardDeck;
    }

    public void setExportCardDeck(OrderedCardDeckState exportCardDeck) {
        this.exportCardDeck = exportCardDeck == null ? new OrderedCardDeckState() : exportCardDeck;
    }

    public List<MigrationCardState> getMigrationCards() {
        return migrationCards == null ? new ArrayList<>() : migrationCards;
    }

    public void setMigrationCards(List<MigrationCardState> migrationCards) {
        this.migrationCards = migrationCards == null
                ? new ArrayList<>()
                : migrationCards.stream().map(MigrationCardState::copy).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public OrderedCardDeckState getMigrationDeck() {
        return migrationDeck == null ? new OrderedCardDeckState() : migrationDeck;
    }

    public void setMigrationDeck(OrderedCardDeckState migrationDeck) {
        this.migrationDeck = migrationDeck == null ? new OrderedCardDeckState() : migrationDeck;
    }

    public List<Map<String, Object>> getStateEventCards() {
        return stateEventCards == null ? new ArrayList<>() : stateEventCards;
    }

    public void setStateEventCards(List<Map<String, Object>> stateEventCards) {
        this.stateEventCards = stateEventCards == null
                ? new ArrayList<>()
                : stateEventCards.stream().map(HashMap::new).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public OrderedCardDeckState getStateEventDeck() {
        return stateEventDeck == null ? new OrderedCardDeckState() : stateEventDeck;
    }

    public void setStateEventDeck(OrderedCardDeckState stateEventDeck) {
        this.stateEventDeck = stateEventDeck == null ? new OrderedCardDeckState() : stateEventDeck;
    }

    public OrderedCardDeckState getCapitalistAutomaDeck() {
        return capitalistAutomaDeck == null ? new OrderedCardDeckState() : capitalistAutomaDeck;
    }

    public void setCapitalistAutomaDeck(OrderedCardDeckState capitalistAutomaDeck) {
        this.capitalistAutomaDeck = capitalistAutomaDeck == null ? new OrderedCardDeckState() : capitalistAutomaDeck;
    }

    public OrderedCardDeckState getWorkerAutomaDeck() {
        return workerAutomaDeck == null ? new OrderedCardDeckState() : workerAutomaDeck;
    }

    public void setWorkerAutomaDeck(OrderedCardDeckState workerAutomaDeck) {
        this.workerAutomaDeck = workerAutomaDeck == null ? new OrderedCardDeckState() : workerAutomaDeck;
    }

    public List<Enterprise> getCapitalistEnterpriseDeck() {
        return capitalistEnterpriseDeck == null ? new ArrayList<>() : capitalistEnterpriseDeck;
    }

    public void setCapitalistEnterpriseDeck(List<Enterprise> capitalistEnterpriseDeck) {
        this.capitalistEnterpriseDeck = capitalistEnterpriseDeck == null
                ? new ArrayList<>()
                : capitalistEnterpriseDeck.stream().map(Enterprise::copy).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public List<Enterprise> getCapitalistEnterpriseMarket() {
        return capitalistEnterpriseMarket == null ? new ArrayList<>() : capitalistEnterpriseMarket;
    }

    public void setCapitalistEnterpriseMarket(List<Enterprise> capitalistEnterpriseMarket) {
        this.capitalistEnterpriseMarket = capitalistEnterpriseMarket == null
                ? new ArrayList<>()
                : capitalistEnterpriseMarket.stream().map(Enterprise::copy).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public List<Enterprise> getMiddleClassEnterpriseDeck() {
        return middleClassEnterpriseDeck == null ? new ArrayList<>() : middleClassEnterpriseDeck;
    }

    public void setMiddleClassEnterpriseDeck(List<Enterprise> middleClassEnterpriseDeck) {
        this.middleClassEnterpriseDeck = middleClassEnterpriseDeck == null
                ? new ArrayList<>()
                : middleClassEnterpriseDeck.stream().map(Enterprise::copy).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public List<Enterprise> getMiddleClassEnterpriseMarket() {
        return middleClassEnterpriseMarket == null ? new ArrayList<>() : middleClassEnterpriseMarket;
    }

    public void setMiddleClassEnterpriseMarket(List<Enterprise> middleClassEnterpriseMarket) {
        this.middleClassEnterpriseMarket = middleClassEnterpriseMarket == null
                ? new ArrayList<>()
                : middleClassEnterpriseMarket.stream().map(Enterprise::copy).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    private void syncPublicServicesStorageFromBoard() {
        publicServicesStorage.put("healthcare", Math.max(0, publicServices == null ? 0 : publicServices.getHealthcare()));
        publicServicesStorage.put("education", Math.max(0, publicServices == null ? 0 : publicServices.getEducation()));
        publicServicesStorage.put("media_influence", Math.max(0, publicServices == null ? 0 : publicServices.getMediaInfluence()));
    }

    private void syncPublicServicesBoardFromStorage() {
        if (publicServices == null) {
            publicServices = new PublicServicesState();
        }
        publicServices.setHealthcare(Math.max(0, publicServicesStorage.getOrDefault("healthcare", 0)));
        publicServices.setEducation(Math.max(0, publicServicesStorage.getOrDefault("education", 0)));
        publicServices.setMediaInfluence(Math.max(0, publicServicesStorage.getOrDefault("media_influence", 0)));
    }
}
