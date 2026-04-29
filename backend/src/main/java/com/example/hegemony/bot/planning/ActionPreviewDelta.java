package com.example.hegemony.bot.planning;

import java.util.HashMap;
import java.util.Map;

public class ActionPreviewDelta {
    private Map<String, Integer> moneyDeltaByPlayer = new HashMap<>();
    private Map<String, Integer> welfareDeltaByPlayer = new HashMap<>();
    private Map<String, Map<String, Integer>> resourceDeltaByPlayer = new HashMap<>();

    public Map<String, Integer> getMoneyDeltaByPlayer() {
        return moneyDeltaByPlayer;
    }

    public void setMoneyDeltaByPlayer(Map<String, Integer> moneyDeltaByPlayer) {
        this.moneyDeltaByPlayer = moneyDeltaByPlayer == null ? new HashMap<>() : new HashMap<>(moneyDeltaByPlayer);
    }

    public Map<String, Integer> getWelfareDeltaByPlayer() {
        return welfareDeltaByPlayer;
    }

    public void setWelfareDeltaByPlayer(Map<String, Integer> welfareDeltaByPlayer) {
        this.welfareDeltaByPlayer = welfareDeltaByPlayer == null ? new HashMap<>() : new HashMap<>(welfareDeltaByPlayer);
    }

    public Map<String, Map<String, Integer>> getResourceDeltaByPlayer() {
        return resourceDeltaByPlayer;
    }

    public void setResourceDeltaByPlayer(Map<String, Map<String, Integer>> resourceDeltaByPlayer) {
        this.resourceDeltaByPlayer = resourceDeltaByPlayer == null ? new HashMap<>() : new HashMap<>(resourceDeltaByPlayer);
    }
}
