package com.example.hegemony.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Worker {
    private String id;
    private ClassType classType;
    private WorkerQualification qualificationType;
    private WorkerSector sector;
    private WorkerLocation location = WorkerLocation.UNEMPLOYED;
    private boolean tiedContract;
    private String enterpriseId;
    private String slotId;

    public Worker() {
    }

    public Worker(
            String id,
            ClassType classType,
            WorkerQualification qualificationType,
            WorkerSector sector,
            WorkerLocation location,
            boolean tiedContract,
            String enterpriseId,
            String slotId
    ) {
        this.id = id;
        this.classType = classType;
        this.qualificationType = qualificationType;
        this.sector = sector;
        this.location = location;
        this.tiedContract = tiedContract;
        this.enterpriseId = enterpriseId;
        this.slotId = slotId;
    }

    public Worker copy() {
        return new Worker(id, classType, qualificationType, sector, location, tiedContract, enterpriseId, slotId);
    }
}
