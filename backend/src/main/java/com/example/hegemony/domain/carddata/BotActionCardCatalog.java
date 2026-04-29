package com.example.hegemony.domain.carddata;

import com.example.hegemony.domain.model.ClassType;

import java.util.List;
import java.util.Optional;

public interface BotActionCardCatalog {
    Optional<BotActionCardDefinition> findById(String botCardId);

    List<BotActionCardDefinition> listForClass(ClassType classType);

    boolean isSimpleAutomaDatasetInstalled(ClassType classType);

    String datasetStatus(ClassType classType);
}
