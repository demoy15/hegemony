package com.example.hegemony.application;

import com.example.hegemony.domain.model.GameState;

public interface GameStateRepository {
    GameState get();

    void save(GameState state);
}
