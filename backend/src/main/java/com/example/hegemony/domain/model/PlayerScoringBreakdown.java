package com.example.hegemony.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PlayerScoringBreakdown {
    private String playerId;
    private ClassType classType;
    private int accumulatedBeforePhase;
    private int gainedThisPhase;
    private int totalAfterPhase;
    private List<ScoringSourceEntry> sources = new ArrayList<>();
    private List<String> unsupportedSources = new ArrayList<>();

    public PlayerScoringBreakdown() {
    }

    public PlayerScoringBreakdown copy() {
        PlayerScoringBreakdown copy = new PlayerScoringBreakdown();
        copy.playerId = playerId;
        copy.classType = classType;
        copy.accumulatedBeforePhase = accumulatedBeforePhase;
        copy.gainedThisPhase = gainedThisPhase;
        copy.totalAfterPhase = totalAfterPhase;
        copy.sources = sources.stream().map(ScoringSourceEntry::copy).toList();
        copy.unsupportedSources = new ArrayList<>(unsupportedSources);
        return copy;
    }

    public void setSources(List<ScoringSourceEntry> sources) {
        this.sources = sources == null ? new ArrayList<>() : new ArrayList<>(sources);
    }

    public void setUnsupportedSources(List<String> unsupportedSources) {
        this.unsupportedSources = unsupportedSources == null ? new ArrayList<>() : new ArrayList<>(unsupportedSources);
    }
}
