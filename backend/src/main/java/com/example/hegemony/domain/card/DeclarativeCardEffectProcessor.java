package com.example.hegemony.domain.card;

import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.PlayerState;

import java.util.Map;

public class DeclarativeCardEffectProcessor {
    private final Map<String, CustomCardResolver> customResolvers;

    public DeclarativeCardEffectProcessor(Map<String, CustomCardResolver> customResolvers) {
        this.customResolvers = customResolvers;
    }

    public CardEffectOutcome resolve(GameState state, PlayerState player, CardDefinition card) {
        if (card.getCustomResolver() != null && !card.getCustomResolver().isBlank()) {
            CustomCardResolver resolver = customResolvers.get(card.getCustomResolver());
            if (resolver != null) {
                return resolver.resolve(state, player, card);
            }
        }

        CardEffectOutcome outcome = CardEffectOutcome.empty();
        for (CardEffectDefinition effect : card.getEffects()) {
            CardEffectOutcome partial = switch (effect.getType()) {
                case GAIN_MONEY -> new CardEffectOutcome(effect.getAmount(), 0, 0);
                case GAIN_GOODS -> new CardEffectOutcome(0, effect.getAmount(), 0);
                case ADJUST_TAXATION -> new CardEffectOutcome(0, 0, effect.getAmount());
            };
            outcome = outcome.combine(partial);
        }
        return outcome;
    }
}
