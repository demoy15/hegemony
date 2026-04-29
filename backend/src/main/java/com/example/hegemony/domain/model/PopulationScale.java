package com.example.hegemony.domain.model;

public final class PopulationScale {
    private PopulationScale() {
    }

    public static int fromWorkerCount(int workers) {
        int normalized = Math.max(0, workers);
        if (normalized == 0) {
            return 0;
        }
        if (normalized <= 11) {
            return 3;
        }
        return Math.min(10, 4 + ((normalized - 12) / 3));
    }
}
