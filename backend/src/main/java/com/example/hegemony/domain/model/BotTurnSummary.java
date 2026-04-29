package com.example.hegemony.domain.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BotTurnSummary {
    private ClassType actingClass;
    private String actingPlayerId;
    private String selectedMoveId;
    private ActionType selectedAction;
    private Map<String, Object> chosenTargets = new HashMap<>();
    private boolean cardModifierPathUsed;
    private String plannerId;
    private String rationale;
    private int legalOptionsConsidered;
    private boolean fallbackHeuristicMode;
    private BotStrategyMode strategyModeUsed = BotStrategyMode.HEURISTIC_FALLBACK;
    private List<String> eventSummaries = new ArrayList<>();
    private Map<String, Object> automaTrace = new HashMap<>();

    public BotTurnSummary copy() {
        BotTurnSummary copy = new BotTurnSummary();
        copy.actingClass = actingClass;
        copy.actingPlayerId = actingPlayerId;
        copy.selectedMoveId = selectedMoveId;
        copy.selectedAction = selectedAction;
        copy.chosenTargets = new HashMap<>(chosenTargets);
        copy.cardModifierPathUsed = cardModifierPathUsed;
        copy.plannerId = plannerId;
        copy.rationale = rationale;
        copy.legalOptionsConsidered = legalOptionsConsidered;
        copy.fallbackHeuristicMode = fallbackHeuristicMode;
        copy.strategyModeUsed = strategyModeUsed;
        copy.eventSummaries = new ArrayList<>(eventSummaries);
        copy.automaTrace = new HashMap<>(automaTrace);
        return copy;
    }

    public ClassType getActingClass() {
        return actingClass;
    }

    public void setActingClass(ClassType actingClass) {
        this.actingClass = actingClass;
    }

    public String getActingPlayerId() {
        return actingPlayerId;
    }

    public void setActingPlayerId(String actingPlayerId) {
        this.actingPlayerId = actingPlayerId;
    }

    public String getSelectedMoveId() {
        return selectedMoveId;
    }

    public void setSelectedMoveId(String selectedMoveId) {
        this.selectedMoveId = selectedMoveId;
    }

    public ActionType getSelectedAction() {
        return selectedAction;
    }

    public void setSelectedAction(ActionType selectedAction) {
        this.selectedAction = selectedAction;
    }

    public Map<String, Object> getChosenTargets() {
        return chosenTargets;
    }

    public void setChosenTargets(Map<String, Object> chosenTargets) {
        this.chosenTargets = chosenTargets == null ? new HashMap<>() : new HashMap<>(chosenTargets);
    }

    public boolean isCardModifierPathUsed() {
        return cardModifierPathUsed;
    }

    public void setCardModifierPathUsed(boolean cardModifierPathUsed) {
        this.cardModifierPathUsed = cardModifierPathUsed;
    }

    public String getPlannerId() {
        return plannerId;
    }

    public void setPlannerId(String plannerId) {
        this.plannerId = plannerId;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public int getLegalOptionsConsidered() {
        return legalOptionsConsidered;
    }

    public void setLegalOptionsConsidered(int legalOptionsConsidered) {
        this.legalOptionsConsidered = legalOptionsConsidered;
    }

    public boolean isFallbackHeuristicMode() {
        return fallbackHeuristicMode;
    }

    public void setFallbackHeuristicMode(boolean fallbackHeuristicMode) {
        this.fallbackHeuristicMode = fallbackHeuristicMode;
    }

    public BotStrategyMode getStrategyModeUsed() {
        return strategyModeUsed;
    }

    public void setStrategyModeUsed(BotStrategyMode strategyModeUsed) {
        this.strategyModeUsed = strategyModeUsed == null ? BotStrategyMode.HEURISTIC_FALLBACK : strategyModeUsed;
    }

    public List<String> getEventSummaries() {
        return eventSummaries;
    }

    public void setEventSummaries(List<String> eventSummaries) {
        this.eventSummaries = eventSummaries == null ? new ArrayList<>() : new ArrayList<>(eventSummaries);
    }

    public Map<String, Object> getAutomaTrace() {
        return automaTrace;
    }

    public void setAutomaTrace(Map<String, Object> automaTrace) {
        this.automaTrace = automaTrace == null ? new HashMap<>() : new HashMap<>(automaTrace);
    }
}
