package com.example.hegemony.domain.model;

import java.util.ArrayList;
import java.util.List;

public class BusinessDealCard {
    private String id;
    private int sequence;
    private String title;
    private List<BusinessDealRequirement> requirements = new ArrayList<>();
    private int payout;
    private int thresholdAmount;
    private int policyABonus;
    private int policyBBonus;
    private String sourceImageRef;

    public BusinessDealCard() {
    }

    public BusinessDealCard(
            String id,
            int sequence,
            String title,
            List<BusinessDealRequirement> requirements,
            int payout,
            int thresholdAmount,
            int policyABonus,
            int policyBBonus,
            String sourceImageRef
    ) {
        this.id = id;
        this.sequence = sequence;
        setRequirements(requirements);
        this.title = title;
        this.payout = payout;
        this.thresholdAmount = thresholdAmount;
        this.policyABonus = policyABonus;
        this.policyBBonus = policyBBonus;
        this.sourceImageRef = sourceImageRef;
    }

    public BusinessDealCard copy() {
        return new BusinessDealCard(
                id,
                sequence,
                title,
                requirements.stream().map(BusinessDealRequirement::copy).toList(),
                payout,
                thresholdAmount,
                policyABonus,
                policyBBonus,
                sourceImageRef
        );
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<BusinessDealRequirement> getRequirements() {
        return requirements;
    }

    public void setRequirements(List<BusinessDealRequirement> requirements) {
        this.requirements = requirements == null ? new ArrayList<>() : new ArrayList<>(requirements);
    }

    public int getPayout() {
        return payout;
    }

    public void setPayout(int payout) {
        this.payout = payout;
    }

    public int getThresholdAmount() {
        return thresholdAmount;
    }

    public void setThresholdAmount(int thresholdAmount) {
        this.thresholdAmount = thresholdAmount;
    }

    public int getPolicyABonus() {
        return policyABonus;
    }

    public void setPolicyABonus(int policyABonus) {
        this.policyABonus = policyABonus;
    }

    public int getPolicyBBonus() {
        return policyBBonus;
    }

    public void setPolicyBBonus(int policyBBonus) {
        this.policyBBonus = policyBBonus;
    }

    public String getSourceImageRef() {
        return sourceImageRef;
    }

    public void setSourceImageRef(String sourceImageRef) {
        this.sourceImageRef = sourceImageRef;
    }
}
