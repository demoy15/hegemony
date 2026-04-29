package com.example.hegemony.infrastructure;

import com.example.hegemony.application.GameInitializer;
import com.example.hegemony.application.GameStateRepository;
import com.example.hegemony.domain.model.GameMode;
import com.example.hegemony.domain.model.GameState;

public class InMemoryGameStateRepository implements GameStateRepository {
    private GameState state;

    public InMemoryGameStateRepository(GameInitializer initializer) {
        this.state = initializer.createInitialGame(GameMode.HUMAN_ONLY, 4, java.util.Map.of());
    }

    @Override
    public synchronized GameState get() {
        return state;
    }

    @Override
    public synchronized void save(GameState state) {
        this.state = state;
    }
}
