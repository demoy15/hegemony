package com.example.hegemony.domain.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class CardReadinessState {
    private boolean enterpriseCardDatasetInstalled;
    private boolean actionModifierDatasetInstalled;
    private Map<ClassType, Boolean> simpleAutomaCardDatasetInstalledByClass = new EnumMap<>(ClassType.class);
    private List<String> notes = new ArrayList<>();

    public CardReadinessState() {
    }

    public CardReadinessState(
            boolean enterpriseCardDatasetInstalled,
            boolean actionModifierDatasetInstalled,
            Map<ClassType, Boolean> simpleAutomaCardDatasetInstalledByClass,
            List<String> notes
    ) {
        this.enterpriseCardDatasetInstalled = enterpriseCardDatasetInstalled;
        this.actionModifierDatasetInstalled = actionModifierDatasetInstalled;
        this.simpleAutomaCardDatasetInstalledByClass = new EnumMap<>(ClassType.class);
        if (simpleAutomaCardDatasetInstalledByClass != null) {
            this.simpleAutomaCardDatasetInstalledByClass.putAll(simpleAutomaCardDatasetInstalledByClass);
        }
        this.notes = notes == null ? new ArrayList<>() : new ArrayList<>(notes);
    }

    public CardReadinessState copy() {
        return new CardReadinessState(
                enterpriseCardDatasetInstalled,
                actionModifierDatasetInstalled,
                simpleAutomaCardDatasetInstalledByClass,
                notes
        );
    }

    public boolean isEnterpriseCardDatasetInstalled() {
        return enterpriseCardDatasetInstalled;
    }

    public void setEnterpriseCardDatasetInstalled(boolean enterpriseCardDatasetInstalled) {
        this.enterpriseCardDatasetInstalled = enterpriseCardDatasetInstalled;
    }

    public boolean isActionModifierDatasetInstalled() {
        return actionModifierDatasetInstalled;
    }

    public void setActionModifierDatasetInstalled(boolean actionModifierDatasetInstalled) {
        this.actionModifierDatasetInstalled = actionModifierDatasetInstalled;
    }

    public Map<ClassType, Boolean> getSimpleAutomaCardDatasetInstalledByClass() {
        return simpleAutomaCardDatasetInstalledByClass;
    }

    public void setSimpleAutomaCardDatasetInstalledByClass(Map<ClassType, Boolean> simpleAutomaCardDatasetInstalledByClass) {
        this.simpleAutomaCardDatasetInstalledByClass = new EnumMap<>(ClassType.class);
        if (simpleAutomaCardDatasetInstalledByClass != null) {
            this.simpleAutomaCardDatasetInstalledByClass.putAll(simpleAutomaCardDatasetInstalledByClass);
        }
    }

    public List<String> getNotes() {
        return notes;
    }

    public void setNotes(List<String> notes) {
        this.notes = notes == null ? new ArrayList<>() : new ArrayList<>(notes);
    }
}
