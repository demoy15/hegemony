package com.example.hegemony.domain.model;

public class MigrationCardEntry {
    private WorkerQualification qualificationType;
    private WorkerSector sector;

    public MigrationCardEntry() {
    }

    public MigrationCardEntry(WorkerQualification qualificationType, WorkerSector sector) {
        this.qualificationType = qualificationType;
        this.sector = sector;
    }

    public MigrationCardEntry copy() {
        return new MigrationCardEntry(qualificationType, sector);
    }

    public WorkerQualification getQualificationType() {
        return qualificationType;
    }

    public void setQualificationType(WorkerQualification qualificationType) {
        this.qualificationType = qualificationType;
    }

    public WorkerSector getSector() {
        return sector;
    }

    public void setSector(WorkerSector sector) {
        this.sector = sector;
    }
}
