package com.example.hegemony.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FinalStanding {
    private String playerId;
    private ClassType classType;
    private int totalVp;
    private int rank;

    public FinalStanding() {
    }

    public FinalStanding(String playerId, ClassType classType, int totalVp, int rank) {
        this.playerId = playerId;
        this.classType = classType;
        this.totalVp = totalVp;
        this.rank = rank;
    }

    public FinalStanding copy() {
        return new FinalStanding(playerId, classType, totalVp, rank);
    }
}
