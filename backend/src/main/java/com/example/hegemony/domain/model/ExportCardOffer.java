package com.example.hegemony.domain.model;

public class ExportCardOffer {
    private String resourceId;
    private int quantity;
    private int revenue;

    public ExportCardOffer() {
    }

    public ExportCardOffer(String resourceId, int quantity, int revenue) {
        this.resourceId = resourceId;
        this.quantity = quantity;
        this.revenue = revenue;
    }

    public ExportCardOffer copy() {
        return new ExportCardOffer(resourceId, quantity, revenue);
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getRevenue() {
        return revenue;
    }

    public void setRevenue(int revenue) {
        this.revenue = revenue;
    }
}
