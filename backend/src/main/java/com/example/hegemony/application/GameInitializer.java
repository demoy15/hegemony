package com.example.hegemony.application;

import com.example.hegemony.application.setup.AutomaSetupHook;
import com.example.hegemony.application.setup.SetupSpecLoader;
import com.example.hegemony.application.setup.SetupSpecModel;
import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.Enterprise;
import com.example.hegemony.domain.model.EnterpriseSlot;
import com.example.hegemony.domain.model.BotStrategyMode;
import com.example.hegemony.domain.model.CardReadinessState;
import com.example.hegemony.domain.model.GameMode;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.GameStatus;
import com.example.hegemony.domain.model.PlayerState;
import com.example.hegemony.domain.model.PlayerControlMode;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.PolicyState;
import com.example.hegemony.domain.model.PreparationSummary;
import com.example.hegemony.domain.model.ProposalToken;
import com.example.hegemony.domain.model.ResourceType;
import com.example.hegemony.domain.model.RoundPhase;
import com.example.hegemony.domain.model.RoundSummary;
import com.example.hegemony.domain.model.TurnOrderState;
import com.example.hegemony.domain.model.Worker;
import com.example.hegemony.domain.model.WorkerLocation;
import com.example.hegemony.domain.model.WorkerQualification;
import com.example.hegemony.domain.model.WorkerSlotColor;
import com.example.hegemony.domain.model.WorkerSector;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class GameInitializer {
    private final SetupSpecLoader setupSpecLoader;
    private final AutomaSetupHook automaSetupHook;
    private final BusinessDealDeckManager businessDealDeckManager;
    private final ExportCardManager exportCardManager;
    private final MigrationCardManager migrationCardManager;

    public GameInitializer(
            SetupSpecLoader setupSpecLoader,
            AutomaSetupHook automaSetupHook,
            BusinessDealDeckManager businessDealDeckManager,
            ExportCardManager exportCardManager,
            MigrationCardManager migrationCardManager
    ) {
        this.setupSpecLoader = setupSpecLoader;
        this.automaSetupHook = automaSetupHook;
        this.businessDealDeckManager = businessDealDeckManager;
        this.exportCardManager = exportCardManager;
        this.migrationCardManager = migrationCardManager;
    }

    public GameState createInitialGame(GameMode mode, int playerCount, Map<String, Object> optionalConfig) {
        if (!List.of(2, 3, 4).contains(playerCount)) {
            throw new IllegalArgumentException("Supported player counts: 2, 3, 4");
        }

        SetupSpecModel spec = setupSpecLoader.load();
        GameState state = new GameState();
        state.setDemoMode(false);

        List<ClassType> activeClasses = new ArrayList<>(spec.getActiveClassesByPlayerCount().getOrDefault(playerCount, List.of()));
        List<ClassType> turnOrder = List.of(ClassType.WORKER, ClassType.MIDDLE_CLASS, ClassType.CAPITALIST, ClassType.STATE)
                .stream()
                .filter(activeClasses::contains)
                .toList();

        Map<ClassType, PlayerControlMode> controlModes = resolveControlModes(optionalConfig, turnOrder);
        Map<ClassType, BotStrategyMode> botStrategyModes = resolveBotStrategyModes(optionalConfig, turnOrder);

        state.setPlayers(createPlayers(spec, turnOrder, controlModes, botStrategyModes));
        state.setPolicies(createPolicies(spec));
        state.setTurnOrder(new TurnOrderState(spec.getRoundMarker(), RoundPhase.ACTIONS, turnOrder, 0));
        state.setCurrentRound(spec.getRoundMarker());
        state.setCurrentPhase(RoundPhase.ACTIONS);
        state.setMaxRounds(5);
        state.setGameStatus(GameStatus.IN_PROGRESS);
        state.setTreasury(spec.getTreasury());
        state.setPublicServices(spec.getPublicServicesByPlayerCount().get(playerCount).copy());
        state.setVotingBag(spec.getVotingBag().copy());
        state.setRoundMarker(spec.getRoundMarker());
        state.setTaxMultiplier(spec.getTaxMultiplier());
        CardReadinessState cardReadiness = new CardReadinessState();
        Map<ClassType, Boolean> simpleAutomaByClass = new EnumMap<>(ClassType.class);
        for (ClassType classType : ClassType.values()) {
            simpleAutomaByClass.put(classType, false);
        }
        cardReadiness.setSimpleAutomaCardDatasetInstalledByClass(simpleAutomaByClass);
        cardReadiness.setEnterpriseCardDatasetInstalled(false);
        cardReadiness.setActionModifierDatasetInstalled(false);
        cardReadiness.setNotes(List.of("Card datasets are not installed in this iteration."));
        state.setCardReadiness(cardReadiness);

        PreparationSummary roundOnePreparation = new PreparationSummary();
        roundOnePreparation.setRound(1);
        roundOnePreparation.setSkipped(true);
        roundOnePreparation.setResolved(true);
        roundOnePreparation.setNotes(List.of("Round 1 preparation is skipped by rules."));
        state.setLastPreparationSummary(roundOnePreparation);

        RoundSummary roundSummary = new RoundSummary();
        roundSummary.setRound(1);
        roundSummary.setPreparationSummary(roundOnePreparation.copy());
        state.setLastRoundSummary(roundSummary);

        List<Enterprise> enterprises = createEnterprises(spec, playerCount);
        List<Worker> workers = createWorkersAndPopulateSlots(spec, playerCount, enterprises);
        state.setEnterprises(enterprises);
        state.setWorkers(workers);
        state.setCapitalistEnterpriseDeck(createCapitalistEnterpriseDeck(spec, enterprises));
        refillCapitalistEnterpriseMarket(state);
        state.setMiddleClassEnterpriseDeck(createMiddleClassEnterpriseDeck());
        refillMiddleClassEnterpriseMarket(state);
        state.refreshLegacyPlayerSnapshots();

        state.setNextLogId(1);
        businessDealDeckManager.ensureInitialized(state);
        exportCardManager.ensureInitialized(state);
        migrationCardManager.ensureInitialized(state);
        state.appendLog("GAME_SETUP", "Game initialized from setup-spec for " + playerCount + " players.");
        automaSetupHook.apply(state, mode, playerCount, optionalConfig == null ? Map.of() : optionalConfig);
        return state;
    }

    public GameState createInitialGame(int playerCount) {
        return createInitialGame(GameMode.HUMAN_ONLY, playerCount, Map.of());
    }

    public GameState demoState() {
        GameState state = createInitialGame(4);
        state.setDemoMode(true);
        state.appendLog("DEMO_MODE", "Legacy demo actions are enabled only as explicitly marked legacy moves.");
        return state;
    }

    private List<PlayerState> createPlayers(
            SetupSpecModel spec,
            List<ClassType> activeClasses,
            Map<ClassType, PlayerControlMode> controlModes,
            Map<ClassType, BotStrategyMode> botStrategyModes
    ) {
        List<PlayerState> players = new ArrayList<>();
        for (ClassType classType : activeClasses) {
            SetupSpecModel.PlayerSetup setup = spec.getPlayers().get(classType);
            PlayerState player = new PlayerState();
            player.setPlayerId(classType.playerId());
            player.setClassType(classType);
            player.setControlMode(controlModes.getOrDefault(classType, PlayerControlMode.HUMAN));
            player.setBotStrategyMode(botStrategyModes.getOrDefault(classType, BotStrategyMode.HEURISTIC_FALLBACK));
            if (setup != null) {
                player.setMoney(setup.getMoney());
                if (classType == ClassType.CAPITALIST) {
                    player.setRevenue(setup.getMoney());
                    player.setCapital(0);
                }
                player.setInfluence(setup.getInfluence());
                player.setPopulation(setup.getPopulation());
                player.setWelfare(setup.getWelfare());
                player.setLastWelfareDelta(0);
                player.setLegitimacyWorker(setup.getLegitimacyWorker());
                player.setLegitimacyMiddleClass(setup.getLegitimacyMiddleClass());
                player.setLegitimacyCapitalist(setup.getLegitimacyCapitalist());
                Map<String, Integer> normalizedResources = normalizeResourceStorage(setup.getResources());
                if (classType == ClassType.CAPITALIST) {
                    player.setProducedResourceStorage(normalizedResources);
                    player.setGoodsAndServicesArea(Map.of());
                } else {
                    player.setGoodsAndServicesArea(normalizedResources);
                    player.setProducedResourceStorage(Map.of());
                }
                player.setPrices(normalizePriceStorage(setup.getPrices()));
                List<ProposalToken> tokens = new ArrayList<>();
                for (int i = 1; i <= setup.getProposalTokenCount(); i++) {
                    tokens.add(new ProposalToken(
                            classType.playerId() + "-proposal-" + i,
                            classType.playerId(),
                            classType,
                            true,
                            null,
                            null
                    ));
                }
                player.setProposalTokens(tokens);
            }
            players.add(player);
        }
        return players;
    }

    @SuppressWarnings("unchecked")
    private Map<ClassType, PlayerControlMode> resolveControlModes(Map<String, Object> optionalConfig, List<ClassType> activeClasses) {
        Map<ClassType, PlayerControlMode> resolved = new EnumMap<>(ClassType.class);
        for (ClassType classType : activeClasses) {
            resolved.put(classType, PlayerControlMode.HUMAN);
        }

        if (optionalConfig == null) {
            return resolved;
        }
        Object raw = optionalConfig.get("controlModes");
        if (!(raw instanceof Map<?, ?> map)) {
            return resolved;
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            try {
                ClassType classType = ClassType.valueOf(String.valueOf(entry.getKey()).toUpperCase(Locale.ROOT));
                PlayerControlMode mode = PlayerControlMode.valueOf(String.valueOf(entry.getValue()).toUpperCase(Locale.ROOT));
                if (activeClasses.contains(classType)) {
                    resolved.put(classType, classType == ClassType.STATE ? PlayerControlMode.HUMAN : mode);
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed setup values and keep deterministic defaults.
            }
        }
        return resolved;
    }

    @SuppressWarnings("unchecked")
    private Map<ClassType, BotStrategyMode> resolveBotStrategyModes(Map<String, Object> optionalConfig, List<ClassType> activeClasses) {
        Map<ClassType, BotStrategyMode> resolved = new EnumMap<>(ClassType.class);
        for (ClassType classType : activeClasses) {
            resolved.put(classType, BotStrategyMode.HEURISTIC_FALLBACK);
        }

        if (optionalConfig == null) {
            return resolved;
        }
        Object raw = optionalConfig.get("botStrategyModes");
        if (!(raw instanceof Map<?, ?> map)) {
            return resolved;
        }
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            try {
                ClassType classType = ClassType.valueOf(String.valueOf(entry.getKey()).toUpperCase(Locale.ROOT));
                BotStrategyMode mode = BotStrategyMode.valueOf(String.valueOf(entry.getValue()).toUpperCase(Locale.ROOT));
                if (activeClasses.contains(classType)) {
                    resolved.put(classType, classType == ClassType.STATE ? BotStrategyMode.HEURISTIC_FALLBACK : mode);
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed setup values and keep deterministic defaults.
            }
        }
        return resolved;
    }

    private List<PolicyState> createPolicies(SetupSpecModel spec) {
        List<PolicyState> policies = new ArrayList<>();
        for (PolicyId policyId : PolicyId.values()) {
            PolicyCourse course = spec.getInitialPolicyCourses().getOrDefault(policyId, PolicyCourse.B);
            policies.add(new PolicyState(policyId, course, null, false));
        }
        return policies;
    }

    private List<Enterprise> createEnterprises(SetupSpecModel spec, int playerCount) {
        Map<String, List<SlotTemplate>> placementTemplates = new HashMap<>();
        for (SetupSpecModel.WorkerPlacementSeed placement : spec.placementsFor(playerCount)) {
            if (placement.enterpriseId() == null) {
                continue;
            }
            placementTemplates.computeIfAbsent(placement.enterpriseId(), ignored -> new ArrayList<>());
            for (int i = 0; i < placement.count(); i++) {
                placementTemplates.get(placement.enterpriseId()).add(new SlotTemplate(placement.qualification(), null, placement.sector()));
            }
        }

        List<Enterprise> enterprises = new ArrayList<>();
        for (SetupSpecModel.EnterpriseSeed seed : spec.enterprisesFor(playerCount)) {
            Enterprise enterprise = new Enterprise();
            enterprise.setId(seed.id());
            enterprise.setName(seed.id());
            enterprise.setCategory(seed.sector().name().toLowerCase(Locale.ROOT));
            enterprise.setCost(0);
            enterprise.setOwnerClass(seed.ownerClass());
            enterprise.setSector(seed.sector());
            enterprise.setWageLevel(seed.wageLevel());

            SetupSpecModel.EnterpriseDefinition capitalistDefinition = seed.ownerClass() == ClassType.CAPITALIST
                    ? spec.capitalistEnterpriseDefinition(seed.id())
                    : null;

            if (capitalistDefinition != null) {
                applyCapitalistDefinition(enterprise, capitalistDefinition);
            } else {
                enterprise.setProducedResources(inferProducedResources(seed.id(), seed.sector(), seed.ownerClass()));
                enterprise.setProductionAmount(enterprise.getProducedResources().values().stream().findFirst().orElse(0));
                enterprise.setProductionPerWorkers(null);
                enterprise.setAutomated(false);
                if (seed.ownerClass() == ClassType.STATE) {
                    enterprise.setWageTrack(Map.of("low", 15, "medium", 20, "high", 25));
                } else if (seed.ownerClass() == ClassType.MIDDLE_CLASS) {
                    enterprise.setWageTrack(defaultMiddleClassWageTrack());
                }
                List<SlotTemplate> slotTemplates = placementTemplates.get(seed.id());
                List<EnterpriseSlot> slots = buildSlotsFromTemplates(seed.id(), slotTemplates);
                if (seed.ownerClass() == ClassType.MIDDLE_CLASS) {
                    slots = withOptionalMiddleClassHiredSlot(seed.id(), slots, colorForSlot(WorkerQualification.UNSKILLED, seed.sector()));
                }
                enterprise.setSlots(slots);
            }
            enterprises.add(enterprise);
        }
        return enterprises;
    }

    private List<Enterprise> createCapitalistEnterpriseDeck(SetupSpecModel spec, List<Enterprise> startingEnterprises) {
        List<String> existingIds = startingEnterprises.stream()
                .map(Enterprise::getId)
                .toList();
        List<Enterprise> deck = spec.getCapitalistEnterpriseDefinitions().values().stream()
                .filter(definition -> !definition.isStarting())
                .filter(definition -> !existingIds.contains(definition.getId()))
                .map(definition -> {
                    Enterprise enterprise = new Enterprise();
                    enterprise.setId(definition.getId());
                    enterprise.setOwnerClass(ClassType.CAPITALIST);
                    enterprise.setWageLevel(2);
                    applyCapitalistDefinition(enterprise, definition);
                    return enterprise;
                })
                .toList();
        deck = new ArrayList<>(deck);
        Collections.shuffle(deck);
        return deck;
    }

    private void refillCapitalistEnterpriseMarket(GameState state) {
        List<Enterprise> market = new ArrayList<>(state.getCapitalistEnterpriseMarket());
        List<Enterprise> deck = new ArrayList<>(state.getCapitalistEnterpriseDeck());
        while (market.size() < 4 && !deck.isEmpty()) {
            market.add(deck.remove(0));
        }
        state.setCapitalistEnterpriseMarket(market);
        state.setCapitalistEnterpriseDeck(deck);
    }

    private List<Enterprise> createMiddleClassEnterpriseDeck() {
        List<Enterprise> deck = new ArrayList<>();
        deck.add(middleClassEnterprise(
                "doctors_office",
                "Doctor's Office",
                "healthcare",
                12,
                WorkerSector.HEALTHCARE,
                ResourceType.HEALTHCARE.id(),
                2,
                2,
                wageTrack(10, 8, 6),
                middleWithHiredSlots("doctors_office", WorkerSlotColor.RED, WorkerSlotColor.GRAY)
        ));
        deck.add(middleClassEnterprise(
                "pr_agency",
                "PR Agency",
                "media",
                20,
                WorkerSector.MEDIA,
                "media",
                3,
                null,
                defaultMiddleClassWageTrack(),
                twoMiddleSlots("pr_agency", WorkerSlotColor.PURPLE)
        ));
        deck.add(middleClassEnterprise(
                "fast_food_restaurant",
                "Fast Food Restaurant",
                "food",
                20,
                WorkerSector.RETAIL,
                ResourceType.FOOD.id(),
                3,
                null,
                defaultMiddleClassWageTrack(),
                twoMiddleSlots("fast_food_restaurant", WorkerSlotColor.GREEN)
        ));
        deck.add(middleClassEnterprise(
                "tutoring_company",
                "Tutoring Company",
                "education",
                12,
                WorkerSector.EDUCATION,
                ResourceType.EDUCATION.id(),
                2,
                2,
                wageTrack(10, 8, 6),
                middleWithHiredSlots("tutoring_company", WorkerSlotColor.ORANGE, WorkerSlotColor.GRAY)
        ));
        deck.add(middleClassEnterprise(
                "private_school",
                "Private School",
                "education",
                20,
                WorkerSector.EDUCATION,
                ResourceType.EDUCATION.id(),
                2,
                4,
                wageTrack(15, 12, 9),
                middleWithHiredSlots("private_school", WorkerSlotColor.ORANGE, WorkerSlotColor.ORANGE)
        ));
        deck.add(middleClassEnterprise(
                "training_center",
                "Training Center",
                "education",
                16,
                WorkerSector.EDUCATION,
                ResourceType.EDUCATION.id(),
                4,
                null,
                defaultMiddleClassWageTrack(),
                twoMiddleSlots("training_center", WorkerSlotColor.ORANGE)
        ));
        deck.add(middleClassEnterprise(
                "convenience_store",
                "Convenience Store",
                "food",
                14,
                WorkerSector.RETAIL,
                ResourceType.FOOD.id(),
                2,
                1,
                wageTrack(10, 8, 6),
                middleWithHiredSlots("convenience_store", WorkerSlotColor.GREEN, WorkerSlotColor.GRAY)
        ));
        deck.add(middleClassEnterprise(
                "medical_laboratory",
                "Medical Laboratory",
                "healthcare",
                20,
                WorkerSector.HEALTHCARE,
                ResourceType.HEALTHCARE.id(),
                2,
                4,
                wageTrack(15, 12, 9),
                middleWithHiredSlots("medical_laboratory", WorkerSlotColor.RED, WorkerSlotColor.RED)
        ));
        deck.add(middleClassEnterprise(
                "electronics_store",
                "Electronics Store",
                "luxury",
                20,
                WorkerSector.BLUE,
                ResourceType.LUXURY.id(),
                2,
                4,
                wageTrack(15, 12, 9),
                middleWithHiredSlots("electronics_store", WorkerSlotColor.BLUE, WorkerSlotColor.BLUE)
        ));
        deck.add(middleClassEnterprise(
                "local_newspaper",
                "Local Newspaper",
                "media",
                14,
                WorkerSector.MEDIA,
                "media",
                2,
                1,
                wageTrack(10, 8, 6),
                middleWithHiredSlots("local_newspaper", WorkerSlotColor.PURPLE, WorkerSlotColor.GRAY)
        ));
        deck.add(middleClassEnterprise(
                "game_store",
                "Game Store",
                "luxury",
                12,
                WorkerSector.BLUE,
                ResourceType.LUXURY.id(),
                2,
                2,
                wageTrack(10, 8, 6),
                middleWithHiredSlots("game_store", WorkerSlotColor.BLUE, WorkerSlotColor.GRAY)
        ));
        deck.add(middleClassEnterprise(
                "organic_farm",
                "Organic Farm",
                "food",
                20,
                WorkerSector.RETAIL,
                ResourceType.FOOD.id(),
                2,
                2,
                wageTrack(15, 12, 9),
                middleWithHiredSlots("organic_farm", WorkerSlotColor.GREEN, WorkerSlotColor.GREEN)
        ));
        deck.add(middleClassEnterprise(
                "pharmacy",
                "Pharmacy",
                "healthcare",
                16,
                WorkerSector.HEALTHCARE,
                ResourceType.HEALTHCARE.id(),
                4,
                null,
                defaultMiddleClassWageTrack(),
                twoMiddleSlots("pharmacy", WorkerSlotColor.RED)
        ));
        deck.add(middleClassEnterprise(
                "regional_radio_station",
                "Regional Radio Station",
                "media",
                20,
                WorkerSector.MEDIA,
                "media",
                2,
                2,
                wageTrack(15, 12, 9),
                middleWithHiredSlots("regional_radio_station", WorkerSlotColor.PURPLE, WorkerSlotColor.PURPLE)
        ));
        deck.add(middleClassEnterprise(
                "jewelry_store",
                "Jewelry Store",
                "luxury",
                16,
                WorkerSector.BLUE,
                ResourceType.LUXURY.id(),
                4,
                null,
                defaultMiddleClassWageTrack(),
                twoMiddleSlots("jewelry_store", WorkerSlotColor.BLUE)
        ));
        deck.add(middleClassEnterprise(
                "doctors_office_starting",
                "Doctor's Office",
                "healthcare",
                12,
                WorkerSector.HEALTHCARE,
                ResourceType.HEALTHCARE.id(),
                2,
                2,
                wageTrack(10, 8, 6),
                middleWithHiredSlots("doctors_office_starting", WorkerSlotColor.RED, WorkerSlotColor.GRAY)
        ));
        deck.add(middleClassEnterprise(
                "convenience_store_starting",
                "Convenience Store",
                "food",
                14,
                WorkerSector.RETAIL,
                ResourceType.FOOD.id(),
                2,
                1,
                wageTrack(10, 8, 6),
                middleWithHiredSlots("convenience_store_starting", WorkerSlotColor.GREEN, WorkerSlotColor.GRAY)
        ));
        return deck;
    }

    private Enterprise middleClassEnterprise(
            String id,
            String name,
            String category,
            int cost,
            WorkerSector sector,
            String producedResourceId,
            int productionAmount,
            Integer productionPerWorkers,
            Map<String, Integer> wageTrack,
            List<EnterpriseSlot> slots
    ) {
        Enterprise enterprise = new Enterprise();
        enterprise.setId(id);
        enterprise.setName(name);
        enterprise.setCategory(category);
        enterprise.setCost(cost);
        enterprise.setOwnerClass(ClassType.MIDDLE_CLASS);
        enterprise.setSector(sector);
        enterprise.setWageLevel(2);
        enterprise.setAutomated(false);
        enterprise.setProducedResources(Map.of(producedResourceId, productionAmount));
        enterprise.setProductionAmount(productionAmount);
        enterprise.setProductionPerWorkers(productionPerWorkers);
        enterprise.setWageTrack(wageTrack == null ? defaultMiddleClassWageTrack() : wageTrack);
        enterprise.setSlots(slots);
        return enterprise;
    }

    private Map<String, Integer> defaultMiddleClassWageTrack() {
        return wageTrack(10, 8, 6);
    }

    private Map<String, Integer> wageTrack(int low, int medium, int high) {
        return Map.of("low", low, "medium", medium, "high", high);
    }

    private List<EnterpriseSlot> middleWithHiredSlots(String enterpriseId, WorkerSlotColor middleColor, WorkerSlotColor hiredColor) {
        return List.of(
                middleSlot(enterpriseId, 1, middleColor),
                hiredSlot(enterpriseId, hiredColor)
        );
    }

    private List<EnterpriseSlot> twoMiddleSlots(String enterpriseId, WorkerSlotColor skilledColor) {
        return withOptionalMiddleClassHiredSlot(enterpriseId, List.of(
                middleSlot(enterpriseId, 1, skilledColor),
                new EnterpriseSlot(enterpriseId + "-middle-slot-2", WorkerQualification.UNSKILLED, WorkerSlotColor.GRAY, null, null)
        ), WorkerSlotColor.GRAY);
    }

    private EnterpriseSlot middleSlot(String enterpriseId, int index, WorkerSlotColor color) {
        return new EnterpriseSlot(
                enterpriseId + "-middle-slot-" + index,
                WorkerQualification.SKILLED,
                color,
                null,
                null
        );
    }

    private EnterpriseSlot hiredSlot(String enterpriseId, WorkerSlotColor color) {
        EnterpriseSlot slot = new EnterpriseSlot(
                enterpriseId + "-hired-worker-slot-1",
                WorkerQualification.UNSKILLED,
                color,
                null,
                null
        );
        slot.setOptional(true);
        return slot;
    }

    private List<EnterpriseSlot> withOptionalMiddleClassHiredSlot(String enterpriseId, List<EnterpriseSlot> slots, WorkerSlotColor color) {
        List<EnterpriseSlot> result = new ArrayList<>(slots == null ? List.of() : slots);
        boolean hasHiredSlot = result.stream().anyMatch(slot -> slot.getId() != null && slot.getId().contains("hired-worker-slot"));
        if (!hasHiredSlot) {
            result.add(hiredSlot(enterpriseId, color == null ? WorkerSlotColor.GRAY : color));
        }
        return result;
    }

    private void refillMiddleClassEnterpriseMarket(GameState state) {
        List<Enterprise> market = new ArrayList<>(state.getMiddleClassEnterpriseMarket());
        List<Enterprise> deck = new ArrayList<>(state.getMiddleClassEnterpriseDeck());
        while (market.size() < 3 && !deck.isEmpty()) {
            market.add(deck.remove(0));
        }
        state.setMiddleClassEnterpriseMarket(market);
        state.setMiddleClassEnterpriseDeck(deck);
    }

    private void applyCapitalistDefinition(Enterprise enterprise, SetupSpecModel.EnterpriseDefinition definition) {
        enterprise.setName(definition.getName() == null || definition.getName().isBlank() ? definition.getId() : definition.getName());
        enterprise.setCategory(definition.getCategory());
        enterprise.setCost(definition.getCost());
        enterprise.setSector(mapSectorFromCategory(definition.getCategory()));
        enterprise.setAutomated(definition.isAutomated());

        SetupSpecModel.ProductionDefinition production = definition.getProduction();
        if (production != null && production.getOutput() != null && !production.getOutput().isBlank()) {
            enterprise.setProducedResources(Map.of(production.getOutput().toLowerCase(Locale.ROOT), Math.max(0, production.getAmount())));
            enterprise.setProductionAmount(Math.max(0, production.getAmount()));
            enterprise.setProductionPerWorkers(production.getPerWorkers());
        } else {
            enterprise.setProducedResources(Map.of());
            enterprise.setProductionAmount(0);
            enterprise.setProductionPerWorkers(null);
        }

        SetupSpecModel.WageTrack wages = definition.getWages();
        if (wages != null) {
            Map<String, Integer> wageTrack = new HashMap<>();
            if (wages.getLow() != null) {
                wageTrack.put("low", Math.max(0, wages.getLow()));
            }
            if (wages.getMedium() != null) {
                wageTrack.put("medium", Math.max(0, wages.getMedium()));
            }
            if (wages.getHigh() != null) {
                wageTrack.put("high", Math.max(0, wages.getHigh()));
            }
            enterprise.setWageTrack(wageTrack);
        } else {
            enterprise.setWageTrack(Map.of());
        }

        enterprise.setSlots(buildSlotsFromDefinition(definition));
    }

    private List<EnterpriseSlot> buildSlotsFromTemplates(String enterpriseId, List<SlotTemplate> slotTemplates) {
        List<EnterpriseSlot> slots = new ArrayList<>();
        if (slotTemplates == null || slotTemplates.isEmpty()) {
            if ("state_media".equals(enterpriseId)) {
                slots.add(new EnterpriseSlot(enterpriseId + "-slot-1", WorkerQualification.SKILLED, WorkerSlotColor.PURPLE, WorkerSector.PURPLE, null));
                slots.add(new EnterpriseSlot(enterpriseId + "-slot-2", WorkerQualification.UNSKILLED, WorkerSlotColor.GRAY, null, null));
                return slots;
            }
            slots.add(new EnterpriseSlot(enterpriseId + "-slot-1", WorkerQualification.UNSKILLED, null, null, null));
            slots.add(new EnterpriseSlot(enterpriseId + "-slot-2", WorkerQualification.UNSKILLED, null, null, null));
            return slots;
        }
        for (int i = 0; i < slotTemplates.size(); i++) {
            SlotTemplate template = slotTemplates.get(i);
            WorkerSlotColor requiredColor = template.color() == null
                    ? colorForSlot(template.qualification(), template.sector())
                    : template.color();
            slots.add(new EnterpriseSlot(
                    enterpriseId + "-slot-" + (i + 1),
                    template.qualification(),
                    requiredColor,
                    template.sector(),
                    null
            ));
        }
        return slots;
    }

    private WorkerSlotColor colorForSlot(WorkerQualification qualification, WorkerSector sector) {
        if (qualification == WorkerQualification.UNSKILLED) {
            return WorkerSlotColor.GRAY;
        }
        return switch (sector) {
            case WHITE, HEALTHCARE -> WorkerSlotColor.WHITE;
            case ORANGE, EDUCATION -> WorkerSlotColor.ORANGE;
            case PURPLE, MEDIA -> WorkerSlotColor.PURPLE;
            case GREEN, RETAIL -> WorkerSlotColor.GREEN;
            case BLUE -> WorkerSlotColor.BLUE;
            case RED -> WorkerSlotColor.WHITE;
            default -> null;
        };
    }

    private List<EnterpriseSlot> buildSlotsFromDefinition(SetupSpecModel.EnterpriseDefinition definition) {
        List<EnterpriseSlot> slots = new ArrayList<>();
        if (definition == null || definition.getWorkers() == null || definition.getWorkers().getSlots() == null) {
            return slots;
        }
        int index = 1;
        for (SetupSpecModel.WorkerSlotDefinition slotDefinition : definition.getWorkers().getSlots()) {
            int count = Math.max(0, slotDefinition.getCount());
            for (int i = 0; i < count; i++) {
                WorkerSlotColor color = slotDefinition.getColor();
                WorkerSector requiredSector = color == null ? null : color.toWorkerSector();
                slots.add(new EnterpriseSlot(
                        definition.getId() + "-slot-" + index++,
                        slotDefinition.getQualification(),
                        color,
                        requiredSector,
                        null
                ));
            }
        }
        return slots;
    }

    private List<Worker> createWorkersAndPopulateSlots(SetupSpecModel spec, int playerCount, List<Enterprise> enterprises) {
        List<Worker> workers = new ArrayList<>();
        Map<ClassType, AtomicInteger> counters = new EnumMap<>(ClassType.class);
        for (ClassType classType : ClassType.values()) {
            counters.put(classType, new AtomicInteger(0));
        }

        for (SetupSpecModel.WorkerPlacementSeed placement : spec.placementsFor(playerCount)) {
            for (int i = 0; i < placement.count(); i++) {
                int next = counters.get(placement.classType()).incrementAndGet();
                String workerId = placement.classType().playerId() + "-worker-" + next;
                Worker worker = new Worker();
                worker.setId(workerId);
                worker.setClassType(placement.classType());
                worker.setQualificationType(placement.qualification());
                worker.setSector(placement.sector());

                if (placement.enterpriseId() == null) {
                    worker.setLocation(WorkerLocation.UNEMPLOYED);
                    worker.setTiedContract(false);
                } else {
                    Enterprise enterprise = enterprises.stream()
                            .filter(e -> e.getId().equals(placement.enterpriseId()))
                            .findFirst()
                            .orElseGet(() -> {
                                Enterprise created = new Enterprise();
                                created.setId(placement.enterpriseId());
                                created.setName(placement.enterpriseId());
                                created.setOwnerClass(placement.classType());
                                created.setSector(inferEnterpriseSector(placement.enterpriseId()));
                                created.setWageLevel(2);
                                created.setProducedResources(inferProducedResources(placement.enterpriseId(), created.getSector(), placement.classType()));
                                enterprises.add(created);
                                return created;
                            });

                    EnterpriseSlot slot = findFirstCompatibleFreeSlot(enterprise, worker);
                    if (slot == null) {
                        String slotId = placement.enterpriseId() + "-slot-extra-" + (enterprise.getSlots().size() + 1);
                        slot = new EnterpriseSlot(slotId, placement.qualification(), null, placement.sector(), null);
                        enterprise.getSlots().add(slot);
                    }

                    slot.setOccupiedWorkerId(workerId);
                    worker.setLocation(WorkerLocation.ENTERPRISE_SLOT);
                    worker.setEnterpriseId(enterprise.getId());
                    worker.setSlotId(slot.getId());
                    worker.setTiedContract(true);
                }
                workers.add(worker);
            }
        }

        return workers;
    }

    private WorkerSector inferEnterpriseSector(String enterpriseId) {
        String id = enterpriseId.toLowerCase();
        if (id.contains("hospital") || id.contains("clinic")) {
            return WorkerSector.HEALTHCARE;
        }
        if (id.contains("college") || id.contains("university")) {
            return WorkerSector.EDUCATION;
        }
        if (id.contains("media") || id.contains("influence")) {
            return WorkerSector.MEDIA;
        }
        if (id.contains("market") || id.contains("shop")) {
            return WorkerSector.RETAIL;
        }
        return WorkerSector.GENERAL;
    }

    private Map<String, Integer> inferProducedResources(String enterpriseId, WorkerSector sector, ClassType ownerClass) {
        Map<String, Integer> produced = new HashMap<>();
        String id = enterpriseId == null ? "" : enterpriseId.toLowerCase();
        boolean stateEnterprise = ownerClass == ClassType.STATE || id.startsWith("state_") || id.contains("state");

        if (id.contains("market") || id.contains("farm")) {
            produced.put("food", 1);
            return produced;
        }
        if (id.contains("hospital") || id.contains("clinic")) {
            produced.put("healthcare", stateEnterprise ? 4 : 1);
            return produced;
        }
        if (id.contains("college") || id.contains("university")) {
            produced.put("education", stateEnterprise ? 4 : 1);
            return produced;
        }
        if (id.contains("media") || id.contains("influence")) {
            produced.put(stateEnterprise ? "media_influence" : "influence", stateEnterprise ? 3 : 1);
            return produced;
        }

        switch (sector) {
            case RETAIL, GREEN -> produced.put("food", 1);
            case BLUE -> produced.put("luxury", 1);
            case RED, WHITE, HEALTHCARE -> produced.put("healthcare", 1);
            case ORANGE, EDUCATION -> produced.put("education", 1);
            case PURPLE, MEDIA -> produced.put("influence", 1);
            default -> produced.put("food", 1);
        }
        return produced;
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

    private Map<String, Integer> normalizeResourceStorage(Map<String, Integer> raw) {
        Map<String, Integer> normalized = new HashMap<>();
        normalized.put("food", 0);
        normalized.put("luxury", 0);
        normalized.put("healthcare", 0);
        normalized.put("education", 0);
        normalized.put("media_influence", 0);
        normalized.put("influence", 0);

        if (raw != null) {
            for (Map.Entry<String, Integer> entry : raw.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank()) {
                    continue;
                }
                normalized.put(entry.getKey(), Math.max(0, entry.getValue() == null ? 0 : entry.getValue()));
            }
        }
        return normalized;
    }

    private Map<String, Integer> normalizePriceStorage(Map<String, Integer> raw) {
        Map<String, Integer> normalized = new HashMap<>();
        normalized.put("food", 0);
        normalized.put("luxury", 0);
        normalized.put("healthcare", 0);
        normalized.put("education", 0);
        normalized.put("influence", 0);

        if (raw != null) {
            for (Map.Entry<String, Integer> entry : raw.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank()) {
                    continue;
                }
                normalized.put(entry.getKey(), Math.max(0, entry.getValue() == null ? 0 : entry.getValue()));
            }
        }
        return normalized;
    }

    private EnterpriseSlot findFirstCompatibleFreeSlot(Enterprise enterprise, Worker worker) {
        for (EnterpriseSlot slot : enterprise.getSlots()) {
            if (slot.isOccupied()) {
                continue;
            }
            if (slot.getRequiredQualification() == WorkerQualification.SKILLED) {
                if (worker.getQualificationType() != WorkerQualification.SKILLED) {
                    continue;
                }
                if (slot.getRequiredSector() != null && slot.getRequiredSector() != worker.getSector()) {
                    continue;
                }
            }
            return slot;
        }
        return null;
    }

    private record SlotTemplate(WorkerQualification qualification, WorkerSlotColor color, WorkerSector sector) {
    }
}
