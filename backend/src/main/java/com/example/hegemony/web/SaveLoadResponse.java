package com.example.hegemony.web;

import com.example.hegemony.domain.model.GameState;

public record SaveLoadResponse(String filePath, GameState gameState) {
}
