package com.example.hegemony.web;

import com.example.hegemony.application.composer.ComposerMetadata;
import com.example.hegemony.bot.planning.MarketCandidate;
import com.example.hegemony.domain.engine.LegalMove;
import com.example.hegemony.domain.model.GameState;

import java.util.List;

public record GameResponse(
        GameState gameState,
        List<LegalMove> legalMoves,
        ComposerMetadata composerMetadata,
        List<MarketCandidate> marketCandidates
) {
}
