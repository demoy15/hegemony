package com.example.hegemony.domain.model;

public class BusinessDealRequirement {
    private String resourceId;
    private int amount;

    public BusinessDealRequirement() {
    }

    public BusinessDealRequirement(String resourceId, int amount) {
        this.resourceId = resourceId;
        this.amount = amount;
    }

    public BusinessDealRequirement copy() {
        return new BusinessDealRequirement(resourceId, amount);
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
