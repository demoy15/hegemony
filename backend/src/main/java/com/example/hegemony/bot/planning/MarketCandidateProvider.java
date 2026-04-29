package com.example.hegemony.bot.planning;

import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.GameState;

import java.util.List;

public interface MarketCandidateProvider {
    List<MarketCandidate> listCandidates(GameState state, ClassType ownerClass);
}
