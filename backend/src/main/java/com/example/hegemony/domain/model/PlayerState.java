package com.example.hegemony.domain.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerState {
    private String playerId;
    private ClassType classType;
    private int money;
    private int revenue;
    private int capital;
    private int wealthTrackLevel;
    private int influence;
    private int population;
    private int welfare;
    private int lastWelfareDelta;
    private int victoryPoints;
    private int legitimacyWorker;
    private int legitimacyMiddleClass;
    private int legitimacyCapitalist;
    private PlayerControlMode controlMode = PlayerControlMode.HUMAN;
    private BotStrategyMode botStrategyMode = BotStrategyMode.HEURISTIC_FALLBACK;
    private Map<String, Integer> resources = new HashMap<>();
    private Map<String, Integer> goodsAndServicesArea = new HashMap<>();
    private Map<String, Integer> producedResourceStorage = new HashMap<>();
    private Map<String, Integer> freeTradeZoneStorage = new HashMap<>();
    private Map<String, Integer> extraStorageTokens = new HashMap<>();
    private Map<String, Integer> prices = new HashMap<>();
    private List<ProposalToken> proposalTokens = new ArrayList<>();
    private List<String> handCards = new ArrayList<>();

    // Legacy demo snapshot fields retained for compatibility with older endpoints/UI pieces.
    private int availableWorkers;
    private int employedWorkers;
    private int enterprises;
    private int goods;

    public PlayerState() {
    }

    public PlayerState(
            String playerId,
            ClassType classType,
            int money,
            int revenue,
            int capital,
            int wealthTrackLevel,
            int influence,
            int population,
            int welfare,
            int lastWelfareDelta,
            int victoryPoints,
            int legitimacyWorker,
            int legitimacyMiddleClass,
            int legitimacyCapitalist,
            PlayerControlMode controlMode,
            BotStrategyMode botStrategyMode,
            Map<String, Integer> resources,
            Map<String, Integer> goodsAndServicesArea,
            Map<String, Integer> producedResourceStorage,
            Map<String, Integer> freeTradeZoneStorage,
            Map<String, Integer> extraStorageTokens,
            Map<String, Integer> prices,
            List<ProposalToken> proposalTokens,
            List<String> handCards,
            int availableWorkers,
            int employedWorkers,
            int enterprises,
            int goods
    ) {
        this.playerId = playerId;
        this.classType = classType;
        this.money = money;
        this.revenue = revenue;
        this.capital = capital;
        this.wealthTrackLevel = wealthTrackLevel;
        this.influence = influence;
        this.population = population;
        this.welfare = welfare;
        this.lastWelfareDelta = lastWelfareDelta;
        this.victoryPoints = victoryPoints;
        this.legitimacyWorker = legitimacyWorker;
        this.legitimacyMiddleClass = legitimacyMiddleClass;
        this.legitimacyCapitalist = legitimacyCapitalist;
        this.controlMode = controlMode == null ? PlayerControlMode.HUMAN : controlMode;
        this.botStrategyMode = botStrategyMode == null ? BotStrategyMode.HEURISTIC_FALLBACK : botStrategyMode;
        this.resources = new HashMap<>(resources == null ? Map.of() : resources);
        this.goodsAndServicesArea = new HashMap<>(goodsAndServicesArea == null ? Map.of() : goodsAndServicesArea);
        this.producedResourceStorage = new HashMap<>(producedResourceStorage == null ? Map.of() : producedResourceStorage);
        this.freeTradeZoneStorage = new HashMap<>(freeTradeZoneStorage == null ? Map.of() : freeTradeZoneStorage);
        this.extraStorageTokens = new HashMap<>(extraStorageTokens == null ? Map.of() : extraStorageTokens);
        this.prices = new HashMap<>(prices == null ? Map.of() : prices);
        this.proposalTokens = new ArrayList<>(proposalTokens == null ? List.of() : proposalTokens);
        this.handCards = new ArrayList<>(handCards == null ? List.of() : handCards);
        this.availableWorkers = availableWorkers;
        this.employedWorkers = employedWorkers;
        this.enterprises = enterprises;
        this.goods = goods;
        syncLegacyResources();
    }

    public PlayerState copy() {
        return new PlayerState(
                playerId,
                classType,
                money,
                revenue,
                capital,
                wealthTrackLevel,
                influence,
                population,
                welfare,
                lastWelfareDelta,
                victoryPoints,
                legitimacyWorker,
                legitimacyMiddleClass,
                legitimacyCapitalist,
                controlMode,
                botStrategyMode,
                resources,
                goodsAndServicesArea,
                producedResourceStorage,
                freeTradeZoneStorage,
                extraStorageTokens,
                prices,
                proposalTokens.stream().map(ProposalToken::copy).toList(),
                handCards,
                availableWorkers,
                employedWorkers,
                enterprises,
                goods
        );
    }

    public int availableProposalTokens() {
        return (int) proposalTokens.stream().filter(ProposalToken::isAvailable).count();
    }

    public ProposalToken takeFirstAvailableProposalToken() {
        for (ProposalToken token : proposalTokens) {
            if (token.isAvailable()) {
                token.setAvailable(false);
                token.setOwnerPlayerId(playerId);
                return token;
            }
        }
        return null;
    }

    public void returnProposalToken(String tokenId) {
        for (ProposalToken token : proposalTokens) {
            if (token.getId().equals(tokenId)) {
                token.setAvailable(true);
                token.setPolicyId(null);
                token.setTargetCourse(null);
                return;
            }
        }
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public ClassType getClassType() {
        return classType;
    }

    public void setClassType(ClassType classType) {
        this.classType = classType;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public int getRevenue() {
        return revenue;
    }

    public void setRevenue(int revenue) {
        this.revenue = revenue;
    }

    public int getCapital() {
        return capital;
    }

    public void setCapital(int capital) {
        this.capital = capital;
    }

    public int getWealthTrackLevel() {
        return wealthTrackLevel;
    }

    public void setWealthTrackLevel(int wealthTrackLevel) {
        this.wealthTrackLevel = Math.max(0, wealthTrackLevel);
    }

    public int getInfluence() {
        return influence;
    }

    public void setInfluence(int influence) {
        this.influence = influence;
    }

    public int getPopulation() {
        return population;
    }

    public void setPopulation(int population) {
        this.population = population;
    }

    public int getWelfare() {
        return welfare;
    }

    public void setWelfare(int welfare) {
        this.welfare = welfare;
    }

    public int getLastWelfareDelta() {
        return lastWelfareDelta;
    }

    public void setLastWelfareDelta(int lastWelfareDelta) {
        this.lastWelfareDelta = lastWelfareDelta;
    }

    public int getVictoryPoints() {
        return victoryPoints;
    }

    public void setVictoryPoints(int victoryPoints) {
        this.victoryPoints = victoryPoints;
    }

    public int getLegitimacyWorker() {
        return legitimacyWorker;
    }

    public void setLegitimacyWorker(int legitimacyWorker) {
        this.legitimacyWorker = legitimacyWorker;
    }

    public int getLegitimacyMiddleClass() {
        return legitimacyMiddleClass;
    }

    public void setLegitimacyMiddleClass(int legitimacyMiddleClass) {
        this.legitimacyMiddleClass = legitimacyMiddleClass;
    }

    public int getLegitimacyCapitalist() {
        return legitimacyCapitalist;
    }

    public void setLegitimacyCapitalist(int legitimacyCapitalist) {
        this.legitimacyCapitalist = legitimacyCapitalist;
    }

    public PlayerControlMode getControlMode() {
        return controlMode;
    }

    public void setControlMode(PlayerControlMode controlMode) {
        this.controlMode = controlMode == null ? PlayerControlMode.HUMAN : controlMode;
    }

    public BotStrategyMode getBotStrategyMode() {
        return botStrategyMode;
    }

    public void setBotStrategyMode(BotStrategyMode botStrategyMode) {
        this.botStrategyMode = botStrategyMode == null ? BotStrategyMode.HEURISTIC_FALLBACK : botStrategyMode;
    }

    public Map<String, Integer> getResources() {
        syncLegacyResources();
        return resources;
    }

    public void setResources(Map<String, Integer> resources) {
        this.resources = resources == null ? new HashMap<>() : new HashMap<>(resources);
        if (classType == ClassType.CAPITALIST) {
            this.producedResourceStorage = new HashMap<>(this.resources);
            this.goodsAndServicesArea = new HashMap<>();
            this.freeTradeZoneStorage = new HashMap<>();
        } else {
            this.goodsAndServicesArea = new HashMap<>(this.resources);
            if (classType != ClassType.MIDDLE_CLASS) {
                this.producedResourceStorage = new HashMap<>();
            }
            this.freeTradeZoneStorage = new HashMap<>();
        }
        syncLegacyResources();
    }

    public Map<String, Integer> getGoodsAndServicesArea() {
        return goodsAndServicesArea;
    }

    public void setGoodsAndServicesArea(Map<String, Integer> goodsAndServicesArea) {
        this.goodsAndServicesArea = goodsAndServicesArea == null ? new HashMap<>() : new HashMap<>(goodsAndServicesArea);
        syncLegacyResources();
    }

    public Map<String, Integer> getProducedResourceStorage() {
        return producedResourceStorage;
    }

    public void setProducedResourceStorage(Map<String, Integer> producedResourceStorage) {
        this.producedResourceStorage = producedResourceStorage == null ? new HashMap<>() : new HashMap<>(producedResourceStorage);
        syncLegacyResources();
    }

    public Map<String, Integer> getFreeTradeZoneStorage() {
        return freeTradeZoneStorage;
    }

    public void setFreeTradeZoneStorage(Map<String, Integer> freeTradeZoneStorage) {
        this.freeTradeZoneStorage = freeTradeZoneStorage == null ? new HashMap<>() : new HashMap<>(freeTradeZoneStorage);
        syncLegacyResources();
    }

    public int getFreeTradeZoneAmount(String resourceId) {
        if (resourceId == null || resourceId.isBlank()) {
            return 0;
        }
        return freeTradeZoneStorage.getOrDefault(normalizeResourceKey(resourceId), 0);
    }

    public void setFreeTradeZoneAmount(String resourceId, int amount) {
        if (resourceId == null || resourceId.isBlank()) {
            return;
        }
        freeTradeZoneStorage.put(normalizeResourceKey(resourceId), Math.max(0, amount));
        syncLegacyResources();
    }

    public void addFreeTradeZoneResource(String resourceId, int amount) {
        if (resourceId == null || resourceId.isBlank() || amount <= 0) {
            return;
        }
        String key = normalizeResourceKey(resourceId);
        freeTradeZoneStorage.put(key, getFreeTradeZoneAmount(key) + amount);
        syncLegacyResources();
    }

    public int consumeFreeTradeZoneResource(String resourceId, int amount) {
        if (resourceId == null || resourceId.isBlank() || amount <= 0) {
            return 0;
        }
        String key = normalizeResourceKey(resourceId);
        int available = getFreeTradeZoneAmount(key);
        int consumed = Math.min(available, amount);
        freeTradeZoneStorage.put(key, available - consumed);
        syncLegacyResources();
        return consumed;
    }

    public Map<String, Integer> getExtraStorageTokens() {
        return extraStorageTokens;
    }

    public void setExtraStorageTokens(Map<String, Integer> extraStorageTokens) {
        this.extraStorageTokens = extraStorageTokens == null ? new HashMap<>() : new HashMap<>(extraStorageTokens);
    }

    public int getExtraStorageTokens(String resourceId) {
        if (resourceId == null || resourceId.isBlank()) {
            return 0;
        }
        return extraStorageTokens.getOrDefault(normalizeResourceKey(resourceId), 0);
    }

    public void addExtraStorageToken(String resourceId) {
        if (resourceId == null || resourceId.isBlank()) {
            return;
        }
        String key = normalizeResourceKey(resourceId);
        extraStorageTokens.put(key, getExtraStorageTokens(key) + 1);
    }

    public Map<String, Integer> getPrices() {
        return prices;
    }

    public void setPrices(Map<String, Integer> prices) {
        this.prices = prices == null ? new HashMap<>() : new HashMap<>(prices);
    }

    public int getResourceAmount(String resourceId) {
        if (resourceId == null || resourceId.isBlank()) {
            return 0;
        }
        String key = normalizeResourceKey(resourceId);
        return getGoodsAmount(key) + getProducedResourceAmount(key) + getFreeTradeZoneAmount(key);
    }

    public void setResourceAmount(String resourceId, int amount) {
        if (resourceId == null || resourceId.isBlank()) {
            return;
        }
        String key = normalizeResourceKey(resourceId);
        if (classType == ClassType.CAPITALIST) {
            producedResourceStorage.put(key, Math.max(0, amount));
        } else {
            goodsAndServicesArea.put(key, Math.max(0, amount));
        }
        syncLegacyResources();
    }

    public void addResource(String resourceId, int amount) {
        if (resourceId == null || resourceId.isBlank() || amount <= 0) {
            return;
        }
        if (classType == ClassType.CAPITALIST) {
            addProducedResource(resourceId, amount);
        } else {
            addGoods(resourceId, amount);
        }
    }

    public int consumeResource(String resourceId, int amount) {
        if (resourceId == null || resourceId.isBlank() || amount <= 0) {
            return 0;
        }
        int consumedFromGoods = consumeGoods(resourceId, amount);
        int remaining = amount - consumedFromGoods;
        if (remaining <= 0) {
            return consumedFromGoods;
        }
        int consumedFromProduced = consumeProducedResource(resourceId, remaining);
        remaining -= consumedFromProduced;
        if (remaining <= 0) {
            return consumedFromGoods + consumedFromProduced;
        }
        return consumedFromGoods + consumedFromProduced + consumeFreeTradeZoneResource(resourceId, remaining);
    }

    public int getGoodsAmount(String resourceId) {
        if (resourceId == null || resourceId.isBlank()) {
            return 0;
        }
        return goodsAndServicesArea.getOrDefault(normalizeResourceKey(resourceId), 0);
    }

    public void setGoodsAmount(String resourceId, int amount) {
        if (resourceId == null || resourceId.isBlank()) {
            return;
        }
        goodsAndServicesArea.put(normalizeResourceKey(resourceId), Math.max(0, amount));
        syncLegacyResources();
    }

    public void addGoods(String resourceId, int amount) {
        if (resourceId == null || resourceId.isBlank() || amount <= 0) {
            return;
        }
        String key = normalizeResourceKey(resourceId);
        goodsAndServicesArea.put(key, getGoodsAmount(key) + amount);
        syncLegacyResources();
    }

    public int consumeGoods(String resourceId, int amount) {
        if (resourceId == null || resourceId.isBlank() || amount <= 0) {
            return 0;
        }
        String key = normalizeResourceKey(resourceId);
        int available = getGoodsAmount(key);
        int consumed = Math.min(available, amount);
        goodsAndServicesArea.put(key, available - consumed);
        syncLegacyResources();
        return consumed;
    }

    public int getProducedResourceAmount(String resourceId) {
        if (resourceId == null || resourceId.isBlank()) {
            return 0;
        }
        return producedResourceStorage.getOrDefault(normalizeResourceKey(resourceId), 0);
    }

    public void setProducedResourceAmount(String resourceId, int amount) {
        if (resourceId == null || resourceId.isBlank()) {
            return;
        }
        producedResourceStorage.put(normalizeResourceKey(resourceId), Math.max(0, amount));
        syncLegacyResources();
    }

    public void addProducedResource(String resourceId, int amount) {
        if (resourceId == null || resourceId.isBlank() || amount <= 0) {
            return;
        }
        String key = normalizeResourceKey(resourceId);
        producedResourceStorage.put(key, getProducedResourceAmount(key) + amount);
        syncLegacyResources();
    }

    public int consumeProducedResource(String resourceId, int amount) {
        if (resourceId == null || resourceId.isBlank() || amount <= 0) {
            return 0;
        }
        String key = normalizeResourceKey(resourceId);
        int available = getProducedResourceAmount(key);
        int consumed = Math.min(available, amount);
        producedResourceStorage.put(key, available - consumed);
        syncLegacyResources();
        return consumed;
    }

    public int getPrice(String resourceId) {
        if (resourceId == null || resourceId.isBlank()) {
            return 0;
        }
        return prices.getOrDefault(normalizeResourceKey(resourceId), 0);
    }

    public void setPrice(String resourceId, int unitPrice) {
        if (resourceId == null || resourceId.isBlank()) {
            return;
        }
        prices.put(normalizeResourceKey(resourceId), Math.max(0, unitPrice));
    }

    public List<ProposalToken> getProposalTokens() {
        return proposalTokens;
    }

    public void setProposalTokens(List<ProposalToken> proposalTokens) {
        this.proposalTokens = proposalTokens == null ? new ArrayList<>() : new ArrayList<>(proposalTokens);
    }

    public List<String> getHandCards() {
        return handCards;
    }

    public void setHandCards(List<String> handCards) {
        this.handCards = handCards == null ? new ArrayList<>() : new ArrayList<>(handCards);
    }

    public int getAvailableWorkers() {
        return availableWorkers;
    }

    public void setAvailableWorkers(int availableWorkers) {
        this.availableWorkers = availableWorkers;
    }

    public int getEmployedWorkers() {
        return employedWorkers;
    }

    public void setEmployedWorkers(int employedWorkers) {
        this.employedWorkers = employedWorkers;
    }

    public int getEnterprises() {
        return enterprises;
    }

    public void setEnterprises(int enterprises) {
        this.enterprises = enterprises;
    }

    public int getGoods() {
        return goods;
    }

    public void setGoods(int goods) {
        this.goods = goods;
    }

    public PlayerRole getRole() {
        return classType == null ? null : PlayerRole.valueOf(classType.name());
    }

    public void setRole(PlayerRole role) {
        this.classType = role == null ? null : ClassType.valueOf(role.name());
    }

    private void syncLegacyResources() {
        Map<String, Integer> merged = new HashMap<>();
        if (goodsAndServicesArea != null) {
            for (Map.Entry<String, Integer> entry : goodsAndServicesArea.entrySet()) {
                merged.merge(normalizeResourceKey(entry.getKey()), Math.max(0, entry.getValue()), Integer::sum);
            }
        }
        if (producedResourceStorage != null) {
            for (Map.Entry<String, Integer> entry : producedResourceStorage.entrySet()) {
                merged.merge(normalizeResourceKey(entry.getKey()), Math.max(0, entry.getValue()), Integer::sum);
            }
        }
        if (freeTradeZoneStorage != null) {
            for (Map.Entry<String, Integer> entry : freeTradeZoneStorage.entrySet()) {
                merged.merge(normalizeResourceKey(entry.getKey()), Math.max(0, entry.getValue()), Integer::sum);
            }
        }
        resources = merged;
    }

    private String normalizeResourceKey(String resourceId) {
        ResourceType resourceType = ResourceType.fromRaw(resourceId);
        return resourceType == null ? resourceId.toLowerCase() : resourceType.id();
    }
}
