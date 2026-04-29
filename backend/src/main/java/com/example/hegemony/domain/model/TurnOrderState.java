package com.example.hegemony.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TurnOrderState {
    private static final int DEFAULT_ACTIONS_PER_PLAYER = 5;

    private int round = 1;
    private RoundPhase phase = RoundPhase.ACTIONS;
    private List<ClassType> activeClasses = new ArrayList<>();
    private int currentPlayerIndex;
    private int actionsPerPlayer = DEFAULT_ACTIONS_PER_PLAYER;
    private List<Integer> actionsTakenByPlayer = new ArrayList<>();

    public TurnOrderState() {
    }

    public TurnOrderState(int round, RoundPhase phase, List<ClassType> activeClasses, int currentPlayerIndex) {
        this.round = round;
        this.phase = phase;
        this.activeClasses = new ArrayList<>(activeClasses);
        this.currentPlayerIndex = currentPlayerIndex;
        resetActionTracking();
    }

    public TurnOrderState copy() {
        TurnOrderState copy = new TurnOrderState(round, phase, activeClasses, currentPlayerIndex);
        copy.actionsPerPlayer = actionsPerPlayer;
        copy.actionsTakenByPlayer = new ArrayList<>(actionsTakenByPlayer);
        copy.ensureActionTracking();
        return copy;
    }

    public ClassType currentClass() {
        if (activeClasses.isEmpty()) {
            return null;
        }
        return activeClasses.get(currentPlayerIndex);
    }

    public void moveToNextPlayer() {
        if (!activeClasses.isEmpty()) {
            currentPlayerIndex = (currentPlayerIndex + 1) % activeClasses.size();
        }
    }

    public void setActiveClasses(List<ClassType> activeClasses) {
        this.activeClasses = new ArrayList<>(activeClasses);
        resetActionTracking();
    }

    public void resetActionTracking() {
        actionsTakenByPlayer = new ArrayList<>();
        for (int i = 0; i < activeClasses.size(); i++) {
            actionsTakenByPlayer.add(0);
        }
    }

    public int actionsTakenForCurrentPlayer() {
        ensureActionTracking();
        if (currentPlayerIndex < 0 || currentPlayerIndex >= actionsTakenByPlayer.size()) {
            return 0;
        }
        return actionsTakenByPlayer.get(currentPlayerIndex);
    }

    public int actionsRemainingForCurrentPlayer() {
        return Math.max(0, actionsPerPlayer - actionsTakenForCurrentPlayer());
    }

    public void recordCompletedActionForCurrentPlayer() {
        ensureActionTracking();
        if (currentPlayerIndex < 0 || currentPlayerIndex >= actionsTakenByPlayer.size()) {
            return;
        }
        actionsTakenByPlayer.set(
                currentPlayerIndex,
                Math.min(actionsPerPlayer, actionsTakenByPlayer.get(currentPlayerIndex) + 1)
        );
    }

    public boolean allPlayersCompletedActions() {
        ensureActionTracking();
        if (activeClasses.isEmpty()) {
            return true;
        }
        return actionsTakenByPlayer.stream().allMatch(count -> count >= actionsPerPlayer);
    }

    private void ensureActionTracking() {
        if (actionsTakenByPlayer == null) {
            actionsTakenByPlayer = new ArrayList<>();
        }
        while (actionsTakenByPlayer.size() < activeClasses.size()) {
            actionsTakenByPlayer.add(0);
        }
        if (actionsTakenByPlayer.size() > activeClasses.size()) {
            actionsTakenByPlayer = new ArrayList<>(actionsTakenByPlayer.subList(0, activeClasses.size()));
        }
    }
}
