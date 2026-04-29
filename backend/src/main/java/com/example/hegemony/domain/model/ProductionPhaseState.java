package com.example.hegemony.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ProductionPhaseState {
    private ProductionSubPhase stage = ProductionSubPhase.PRODUCE_GOODS_AND_SERVICES;
    private boolean productionResolved;
    private boolean roundAdvanceReady;
    private List<EnterpriseProductionResult> enterpriseResults = new ArrayList<>();
    private int workerFoodRequired;
    private int workerFoodConsumed;
    private int workerFoodUnmet;
    private int middleClassFoodRequired;
    private int middleClassFoodConsumed;
    private int middleClassFoodUnmet;
    private int workerTaxesPaid;
    private int middleClassTaxesPaid;
    private int capitalistTaxesPaid;
    private boolean insufficientFood;
    private boolean unsupportedMarketAcquisition;
    private boolean unsupportedCapitalistTaxModel;
    private boolean unsupportedMiddleClassTaxModel;

    public ProductionPhaseState copy() {
        ProductionPhaseState copy = new ProductionPhaseState();
        copy.stage = stage;
        copy.productionResolved = productionResolved;
        copy.roundAdvanceReady = roundAdvanceReady;
        copy.enterpriseResults = enterpriseResults.stream().map(EnterpriseProductionResult::copy).toList();
        copy.workerFoodRequired = workerFoodRequired;
        copy.workerFoodConsumed = workerFoodConsumed;
        copy.workerFoodUnmet = workerFoodUnmet;
        copy.middleClassFoodRequired = middleClassFoodRequired;
        copy.middleClassFoodConsumed = middleClassFoodConsumed;
        copy.middleClassFoodUnmet = middleClassFoodUnmet;
        copy.workerTaxesPaid = workerTaxesPaid;
        copy.middleClassTaxesPaid = middleClassTaxesPaid;
        copy.capitalistTaxesPaid = capitalistTaxesPaid;
        copy.insufficientFood = insufficientFood;
        copy.unsupportedMarketAcquisition = unsupportedMarketAcquisition;
        copy.unsupportedCapitalistTaxModel = unsupportedCapitalistTaxModel;
        copy.unsupportedMiddleClassTaxModel = unsupportedMiddleClassTaxModel;
        return copy;
    }

    public void setEnterpriseResults(List<EnterpriseProductionResult> enterpriseResults) {
        this.enterpriseResults = enterpriseResults == null ? new ArrayList<>() : new ArrayList<>(enterpriseResults);
    }
}
