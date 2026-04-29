package com.example.hegemony.application.composer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionPreviewDelta {
    private Map<String, Integer> moneyDeltaByPlayer = new HashMap<>();
    private Map<String, Map<String, Integer>> resourceDeltaByPlayer = new HashMap<>();
    private Map<String, Integer> welfareDeltaByPlayer = new HashMap<>();
    private Map<String, String> workerMovement = new HashMap<>();
    private Map<String, String> policyDelta = new HashMap<>();
    private Map<String, Integer> proposalTokenDeltaByPlayer = new HashMap<>();
    private Map<String, Integer> influenceDeltaByPlayer = new HashMap<>();
    private Map<String, Integer> victoryPointDeltaByPlayer = new HashMap<>();
    private List<String> notes = new ArrayList<>();

    public Map<String, Integer> getMoneyDeltaByPlayer() {
        return moneyDeltaByPlayer;
    }

    public void setMoneyDeltaByPlayer(Map<String, Integer> moneyDeltaByPlayer) {
        this.moneyDeltaByPlayer = moneyDeltaByPlayer == null ? new HashMap<>() : new HashMap<>(moneyDeltaByPlayer);
    }

    public Map<String, Map<String, Integer>> getResourceDeltaByPlayer() {
        return resourceDeltaByPlayer;
    }

    public void setResourceDeltaByPlayer(Map<String, Map<String, Integer>> resourceDeltaByPlayer) {
        this.resourceDeltaByPlayer = resourceDeltaByPlayer == null ? new HashMap<>() : new HashMap<>(resourceDeltaByPlayer);
    }

    public Map<String, Integer> getWelfareDeltaByPlayer() {
        return welfareDeltaByPlayer;
    }

    public void setWelfareDeltaByPlayer(Map<String, Integer> welfareDeltaByPlayer) {
        this.welfareDeltaByPlayer = welfareDeltaByPlayer == null ? new HashMap<>() : new HashMap<>(welfareDeltaByPlayer);
    }

    public Map<String, String> getWorkerMovement() {
        return workerMovement;
    }

    public void setWorkerMovement(Map<String, String> workerMovement) {
        this.workerMovement = workerMovement == null ? new HashMap<>() : new HashMap<>(workerMovement);
    }

    public Map<String, String> getPolicyDelta() {
        return policyDelta;
    }

    public void setPolicyDelta(Map<String, String> policyDelta) {
        this.policyDelta = policyDelta == null ? new HashMap<>() : new HashMap<>(policyDelta);
    }

    public Map<String, Integer> getProposalTokenDeltaByPlayer() {
        return proposalTokenDeltaByPlayer;
    }

    public void setProposalTokenDeltaByPlayer(Map<String, Integer> proposalTokenDeltaByPlayer) {
        this.proposalTokenDeltaByPlayer = proposalTokenDeltaByPlayer == null ? new HashMap<>() : new HashMap<>(proposalTokenDeltaByPlayer);
    }

    public Map<String, Integer> getInfluenceDeltaByPlayer() {
        return influenceDeltaByPlayer;
    }

    public void setInfluenceDeltaByPlayer(Map<String, Integer> influenceDeltaByPlayer) {
        this.influenceDeltaByPlayer = influenceDeltaByPlayer == null ? new HashMap<>() : new HashMap<>(influenceDeltaByPlayer);
    }

    public Map<String, Integer> getVictoryPointDeltaByPlayer() {
        return victoryPointDeltaByPlayer;
    }

    public void setVictoryPointDeltaByPlayer(Map<String, Integer> victoryPointDeltaByPlayer) {
        this.victoryPointDeltaByPlayer = victoryPointDeltaByPlayer == null ? new HashMap<>() : new HashMap<>(victoryPointDeltaByPlayer);
    }

    public List<String> getNotes() {
        return notes;
    }

    public void setNotes(List<String> notes) {
        this.notes = notes == null ? new ArrayList<>() : new ArrayList<>(notes);
    }
}
