package com.example.hegemony.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class FinalResult {
    private int completedRound;
    private boolean tie;
    private boolean tiebreakApplied;
    private boolean unresolvedTie;
    private List<String> winnerPlayerIds = new ArrayList<>();
    private List<FinalStanding> standings = new ArrayList<>();
    private List<PlayerScoringBreakdown> scoringBreakdown = new ArrayList<>();
    private List<String> unsupportedNotes = new ArrayList<>();

    public FinalResult() {
    }

    public FinalResult copy() {
        FinalResult copy = new FinalResult();
        copy.completedRound = completedRound;
        copy.tie = tie;
        copy.tiebreakApplied = tiebreakApplied;
        copy.unresolvedTie = unresolvedTie;
        copy.winnerPlayerIds = new ArrayList<>(winnerPlayerIds);
        copy.standings = standings.stream().map(FinalStanding::copy).toList();
        copy.scoringBreakdown = scoringBreakdown.stream().map(PlayerScoringBreakdown::copy).toList();
        copy.unsupportedNotes = new ArrayList<>(unsupportedNotes);
        return copy;
    }

    public void setWinnerPlayerIds(List<String> winnerPlayerIds) {
        this.winnerPlayerIds = winnerPlayerIds == null ? new ArrayList<>() : new ArrayList<>(winnerPlayerIds);
    }

    public void setStandings(List<FinalStanding> standings) {
        this.standings = standings == null ? new ArrayList<>() : new ArrayList<>(standings);
    }

    public void setScoringBreakdown(List<PlayerScoringBreakdown> scoringBreakdown) {
        this.scoringBreakdown = scoringBreakdown == null ? new ArrayList<>() : new ArrayList<>(scoringBreakdown);
    }

    public void setUnsupportedNotes(List<String> unsupportedNotes) {
        this.unsupportedNotes = unsupportedNotes == null ? new ArrayList<>() : new ArrayList<>(unsupportedNotes);
    }
}
