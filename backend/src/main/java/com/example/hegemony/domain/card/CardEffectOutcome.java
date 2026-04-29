package com.example.hegemony.domain.card;

public record CardEffectOutcome(int moneyDelta, int goodsDelta, int taxationDelta) {
    public static CardEffectOutcome empty() {
        return new CardEffectOutcome(0, 0, 0);
    }

    public CardEffectOutcome combine(CardEffectOutcome other) {
        return new CardEffectOutcome(
                moneyDelta + other.moneyDelta,
                goodsDelta + other.goodsDelta,
                taxationDelta + other.taxationDelta
        );
    }
}
