package com.example.hegemony.domain.carddata;

import com.example.hegemony.domain.model.ClassType;

import java.util.List;
import java.util.Optional;

public interface EnterpriseCardCatalog {
    Optional<EnterpriseCardDefinition> findById(String enterpriseCardId);

    List<EnterpriseCardDefinition> listAvailableMarketCards(ClassType ownerClass);

    boolean isDatasetInstalled();

    String datasetStatus();
}
