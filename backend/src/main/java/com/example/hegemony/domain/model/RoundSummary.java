package com.example.hegemony.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoundSummary {
    private int round;
    private PreparationSummary preparationSummary;
    private ScoringSummary scoringSummary;

    public RoundSummary() {
    }

    public RoundSummary copy() {
        RoundSummary copy = new RoundSummary();
        copy.round = round;
        copy.preparationSummary = preparationSummary == null ? null : preparationSummary.copy();
        copy.scoringSummary = scoringSummary == null ? null : scoringSummary.copy();
        return copy;
    }
}
