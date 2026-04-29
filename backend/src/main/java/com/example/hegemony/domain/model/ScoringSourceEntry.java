package com.example.hegemony.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScoringSourceEntry {
    private String sourceId;
    private int vpDelta;
    private boolean supported = true;
    private String note;

    public ScoringSourceEntry() {
    }

    public ScoringSourceEntry(String sourceId, int vpDelta, boolean supported, String note) {
        this.sourceId = sourceId;
        this.vpDelta = vpDelta;
        this.supported = supported;
        this.note = note;
    }

    public ScoringSourceEntry copy() {
        return new ScoringSourceEntry(sourceId, vpDelta, supported, note);
    }
}
