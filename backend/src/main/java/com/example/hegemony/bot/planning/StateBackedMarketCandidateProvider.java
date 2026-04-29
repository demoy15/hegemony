package com.example.hegemony.bot.planning;

import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.Enterprise;
import com.example.hegemony.domain.model.GameState;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StateBackedMarketCandidateProvider implements MarketCandidateProvider {
    @Override
    public List<MarketCandidate> listCandidates(GameState state, ClassType ownerClass) {
        return state.getEnterprises().stream()
                .filter(enterprise -> enterprise.getOwnerClass() == ownerClass)
                .map(this::toCandidate)
                .toList();
    }

    private MarketCandidate toCandidate(Enterprise enterprise) {
        int freeSlots = (int) enterprise.getSlots().stream().filter(slot -> !slot.isOccupied()).count();
        return new MarketCandidate(
                enterprise.getId(),
                enterprise.getOwnerClass(),
                freeSlots,
                enterprise.getWageLevel(),
                "fallback_market_view"
        );
    }
}
