package com.example.hegemony.domain.automa.capitalist;

import java.util.List;
import java.util.Optional;

public interface CapitalistAutomaCardRegistry {
    boolean isLoaded();

    String datasetStatus();

    List<CapitalistAutomaActionCard> actionCards();

    List<CapitalistAutomaInstructionCard> instructionCards();

    Optional<CapitalistAutomaActionCard> findActionCard(int cardNo);
}
