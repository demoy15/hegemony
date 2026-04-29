package com.example.hegemony.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VotingBagState {
    private int worker;
    private int middleClass;
    private int capitalist;

    public VotingBagState() {
    }

    public VotingBagState(int worker, int middleClass, int capitalist) {
        this.worker = worker;
        this.middleClass = middleClass;
        this.capitalist = capitalist;
    }

    public VotingBagState copy() {
        return new VotingBagState(worker, middleClass, capitalist);
    }

    public int totalCubes() {
        return Math.max(0, worker) + Math.max(0, middleClass) + Math.max(0, capitalist);
    }

    public void add(VotingCubeOwnerClass ownerClass, int amount) {
        int safeAmount = Math.max(0, amount);
        switch (ownerClass) {
            case WORKER -> worker += safeAmount;
            case MIDDLE_CLASS -> middleClass += safeAmount;
            case CAPITALIST -> capitalist += safeAmount;
        }
    }

    public boolean removeOne(VotingCubeOwnerClass ownerClass) {
        return switch (ownerClass) {
            case WORKER -> {
                if (worker <= 0) {
                    yield false;
                }
                worker--;
                yield true;
            }
            case MIDDLE_CLASS -> {
                if (middleClass <= 0) {
                    yield false;
                }
                middleClass--;
                yield true;
            }
            case CAPITALIST -> {
                if (capitalist <= 0) {
                    yield false;
                }
                capitalist--;
                yield true;
            }
        };
    }
}
