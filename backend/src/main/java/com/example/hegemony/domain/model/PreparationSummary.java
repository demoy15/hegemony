package com.example.hegemony.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PreparationSummary {
    private int round;
    private boolean skipped;
    private boolean resolved;
    private List<String> executedSubsteps = new ArrayList<>();
    private List<String> unsupportedSubsteps = new ArrayList<>();
    private List<String> notes = new ArrayList<>();

    public PreparationSummary() {
    }

    public PreparationSummary copy() {
        PreparationSummary copy = new PreparationSummary();
        copy.round = round;
        copy.skipped = skipped;
        copy.resolved = resolved;
        copy.executedSubsteps = new ArrayList<>(executedSubsteps);
        copy.unsupportedSubsteps = new ArrayList<>(unsupportedSubsteps);
        copy.notes = new ArrayList<>(notes);
        return copy;
    }

    public void setExecutedSubsteps(List<String> executedSubsteps) {
        this.executedSubsteps = executedSubsteps == null ? new ArrayList<>() : new ArrayList<>(executedSubsteps);
    }

    public void setUnsupportedSubsteps(List<String> unsupportedSubsteps) {
        this.unsupportedSubsteps = unsupportedSubsteps == null ? new ArrayList<>() : new ArrayList<>(unsupportedSubsteps);
    }

    public void setNotes(List<String> notes) {
        this.notes = notes == null ? new ArrayList<>() : new ArrayList<>(notes);
    }
}
