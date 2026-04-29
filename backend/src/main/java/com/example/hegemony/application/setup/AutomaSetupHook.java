package com.example.hegemony.application.setup;

import com.example.hegemony.domain.model.GameMode;
import com.example.hegemony.domain.model.GameState;

import java.util.Map;

public interface AutomaSetupHook {
    void apply(GameState state, GameMode mode, int playerCount, Map<String, Object> optionalConfig);
}
