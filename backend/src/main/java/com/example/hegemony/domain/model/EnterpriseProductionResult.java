package com.example.hegemony.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class EnterpriseProductionResult {
    private String enterpriseId;
    private String ownerPlayerId;
    private boolean functioning;
    private Map<String, Integer> wagesPaidByRecipient = new HashMap<>();
    private Map<String, Integer> producedResources = new HashMap<>();

    public EnterpriseProductionResult copy() {
        EnterpriseProductionResult copy = new EnterpriseProductionResult();
        copy.enterpriseId = enterpriseId;
        copy.ownerPlayerId = ownerPlayerId;
        copy.functioning = functioning;
        copy.wagesPaidByRecipient = new HashMap<>(wagesPaidByRecipient);
        copy.producedResources = new HashMap<>(producedResources);
        return copy;
    }

    public void setWagesPaidByRecipient(Map<String, Integer> wagesPaidByRecipient) {
        this.wagesPaidByRecipient = wagesPaidByRecipient == null ? new HashMap<>() : new HashMap<>(wagesPaidByRecipient);
    }

    public void setProducedResources(Map<String, Integer> producedResources) {
        this.producedResources = producedResources == null ? new HashMap<>() : new HashMap<>(producedResources);
    }
}
