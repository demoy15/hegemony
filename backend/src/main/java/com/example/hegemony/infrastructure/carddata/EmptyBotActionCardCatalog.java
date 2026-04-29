package com.example.hegemony.infrastructure.carddata;

import com.example.hegemony.domain.carddata.BotActionCardCatalog;
import com.example.hegemony.domain.carddata.BotActionCardDefinition;
import com.example.hegemony.domain.model.ClassType;

import java.util.List;
import java.util.Optional;

public class EmptyBotActionCardCatalog implements BotActionCardCatalog {
    @Override
    public Optional<BotActionCardDefinition> findById(String botCardId) {
        return Optional.empty();
    }

    @Override
    public List<BotActionCardDefinition> listForClass(ClassType classType) {
        return List.of();
    }

    @Override
    public boolean isSimpleAutomaDatasetInstalled(ClassType classType) {
        return false;
    }

    @Override
    public String datasetStatus(ClassType classType) {
        return "CARD_DATA_NOT_INSTALLED";
    }
}
