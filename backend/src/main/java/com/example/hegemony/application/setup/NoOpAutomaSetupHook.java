package com.example.hegemony.application.setup;

import com.example.hegemony.domain.model.GameMode;
import com.example.hegemony.domain.model.GameState;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NoOpAutomaSetupHook implements AutomaSetupHook {
    @Override
    public void apply(GameState state, GameMode mode, int playerCount, Map<String, Object> optionalConfig) {
        // Intentionally no-op in the current slice.
    }
}
