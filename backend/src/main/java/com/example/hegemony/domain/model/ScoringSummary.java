package com.example.hegemony.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ScoringSummary {
    private int round;
    private boolean resolved;
    private List<PlayerScoringBreakdown> players = new ArrayList<>();
    private List<String> unsupportedSources = new ArrayList<>();
    private List<String> notes = new ArrayList<>();

    public ScoringSummary() {
    }

    public ScoringSummary copy() {
        ScoringSummary copy = new ScoringSummary();
        copy.round = round;
        copy.resolved = resolved;
        copy.players = players.stream().map(PlayerScoringBreakdown::copy).toList();
        copy.unsupportedSources = new ArrayList<>(unsupportedSources);
        copy.notes = new ArrayList<>(notes);
        return copy;
    }

    public void setPlayers(List<PlayerScoringBreakdown> players) {
        this.players = players == null ? new ArrayList<>() : new ArrayList<>(players);
    }

    public void setUnsupportedSources(List<String> unsupportedSources) {
        this.unsupportedSources = unsupportedSources == null ? new ArrayList<>() : new ArrayList<>(unsupportedSources);
    }

    public void setNotes(List<String> notes) {
        this.notes = notes == null ? new ArrayList<>() : new ArrayList<>(notes);
    }
}
