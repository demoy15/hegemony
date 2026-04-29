package com.example.hegemony.domain.automa.capitalist;

import com.example.hegemony.domain.model.BotStrategyMode;

public enum CapitalistAutomaExecutionMode {
    SIMPLE,
    COMPLEX;

    public static CapitalistAutomaExecutionMode fromStrategy(BotStrategyMode strategyMode) {
        if (strategyMode == BotStrategyMode.CARD_DRIVEN_COMPLEX_AUTOMA) {
            return COMPLEX;
        }
        return SIMPLE;
    }
}
