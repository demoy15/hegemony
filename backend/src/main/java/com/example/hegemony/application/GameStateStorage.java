package com.example.hegemony.application;

import com.example.hegemony.domain.model.GameState;

public interface GameStateStorage {
    String save(GameState state, String fileName);

    GameState load(String fileName);
}
