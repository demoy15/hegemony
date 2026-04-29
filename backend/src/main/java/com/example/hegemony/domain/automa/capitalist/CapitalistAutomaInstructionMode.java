package com.example.hegemony.domain.automa.capitalist;

public enum CapitalistAutomaInstructionMode {
    SIMPLE,
    COMPLEX,
    BOTH;

    public boolean supports(CapitalistAutomaExecutionMode mode) {
        if (this == BOTH) {
            return true;
        }
        return (this == SIMPLE && mode == CapitalistAutomaExecutionMode.SIMPLE)
                || (this == COMPLEX && mode == CapitalistAutomaExecutionMode.COMPLEX);
    }
}
