package com.example.hegemony.domain.card;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardEffectDefinition {
    private CardEffectType type;
    private int amount;

    public CardEffectDefinition() {
    }

    public CardEffectDefinition(CardEffectType type, int amount) {
        this.type = type;
        this.amount = amount;
    }
}
