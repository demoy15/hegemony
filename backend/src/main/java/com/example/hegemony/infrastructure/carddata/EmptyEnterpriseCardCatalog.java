package com.example.hegemony.infrastructure.carddata;

import com.example.hegemony.domain.carddata.EnterpriseCardCatalog;
import com.example.hegemony.domain.carddata.EnterpriseCardDefinition;
import com.example.hegemony.domain.model.ClassType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class EmptyEnterpriseCardCatalog implements EnterpriseCardCatalog {
    @Override
    public Optional<EnterpriseCardDefinition> findById(String enterpriseCardId) {
        return Optional.empty();
    }

    @Override
    public List<EnterpriseCardDefinition> listAvailableMarketCards(ClassType ownerClass) {
        return List.of();
    }

    @Override
    public boolean isDatasetInstalled() {
        return false;
    }

    @Override
    public String datasetStatus() {
        return "CARD_DATA_NOT_INSTALLED";
    }
}
