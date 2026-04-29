package com.example.hegemony.domain.card;

import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.PlayerState;

public interface CustomCardResolver {
    CardEffectOutcome resolve(GameState state, PlayerState player, CardDefinition card);
}
