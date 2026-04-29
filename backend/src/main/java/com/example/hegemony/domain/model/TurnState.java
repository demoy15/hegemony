package com.example.hegemony.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TurnState {
    private int round = 1;
    private int currentPlayerIndex = 0;
    private Phase phase = Phase.TURN_START;

    public TurnState() {
    }

    public TurnState(int round, int currentPlayerIndex, Phase phase) {
        this.round = round;
        this.currentPlayerIndex = currentPlayerIndex;
        this.phase = phase;
    }

    public TurnState copy() {
        return new TurnState(round, currentPlayerIndex, phase);
    }
}
