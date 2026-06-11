package com.example.hegemony.domain.automa.worker;

import java.util.List;
import java.util.Optional;

public interface WorkerAutomaCardRegistry {
    boolean isLoaded();

    String datasetStatus();

    List<WorkerAutomaActionCard> actionCards();

    List<WorkerAutomaInstructionCard> instructionCards();

    Optional<WorkerAutomaActionCard> findActionCard(int cardNo);
}
