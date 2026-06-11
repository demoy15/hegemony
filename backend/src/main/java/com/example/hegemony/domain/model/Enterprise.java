package com.example.hegemony.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Enterprise {
    private String id;
    private String name;
    private String category;
    private int cost;
    private ClassType ownerClass;
    private WorkerSector sector = WorkerSector.GENERAL;
    private int wageLevel;
    private boolean automated;
    private int productionAmount;
    private Integer productionPerWorkers;
    private boolean strikeToken;
    private Map<String, Integer> wageTrack = new HashMap<>();
    private Map<String, Integer> producedResources = new HashMap<>();
    private List<EnterpriseSlot> slots = new ArrayList<>();

    public Enterprise() {
    }

    public Enterprise(
            String id,
            String name,
            String category,
            int cost,
            ClassType ownerClass,
            WorkerSector sector,
            int wageLevel,
            boolean automated,
            int productionAmount,
            Integer productionPerWorkers,
            boolean strikeToken,
            Map<String, Integer> wageTrack,
            Map<String, Integer> producedResources,
            List<EnterpriseSlot> slots
    ) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.cost = cost;
        this.ownerClass = ownerClass;
        this.sector = sector;
        this.wageLevel = wageLevel;
        this.automated = automated;
        this.productionAmount = productionAmount;
        this.productionPerWorkers = productionPerWorkers;
        this.strikeToken = strikeToken;
        this.wageTrack = new HashMap<>(wageTrack == null ? Map.of() : wageTrack);
        this.producedResources = new HashMap<>(producedResources);
        this.slots = new ArrayList<>(slots);
    }

    public Enterprise copy() {
        return new Enterprise(
                id,
                name,
                category,
                cost,
                ownerClass,
                sector,
                wageLevel,
                automated,
                productionAmount,
                productionPerWorkers,
                strikeToken,
                wageTrack,
                producedResources,
                slots.stream().map(EnterpriseSlot::copy).toList()
        );
    }

    public boolean isFunctioning() {
        if (automated && slots.isEmpty()) {
            return true;
        }
        if (slots.isEmpty()) {
            return false;
        }
        return slots.stream().filter(slot -> !slot.isOptional()).allMatch(EnterpriseSlot::isOccupied);
    }

    public boolean isFullyEmpty() {
        return slots.stream().filter(slot -> !slot.isOptional()).noneMatch(EnterpriseSlot::isOccupied);
    }

    public boolean isPartiallyFilled() {
        return !isFunctioning() && !isFullyEmpty();
    }

    public void setProducedResources(Map<String, Integer> producedResources) {
        this.producedResources = producedResources == null ? new HashMap<>() : new HashMap<>(producedResources);
    }

    public void setWageTrack(Map<String, Integer> wageTrack) {
        this.wageTrack = wageTrack == null ? new HashMap<>() : new HashMap<>(wageTrack);
    }

    public void setSlots(List<EnterpriseSlot> slots) {
        this.slots = new ArrayList<>(slots);
    }
}
