package com.example.hegemony.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnterpriseSlot {
    private String id;
    private WorkerQualification requiredQualification = WorkerQualification.UNSKILLED;
    private WorkerSlotColor requiredColor;
    private WorkerSector requiredSector;
    private String occupiedWorkerId;
    private boolean optional;

    public EnterpriseSlot() {
    }

    public EnterpriseSlot(String id, WorkerQualification requiredQualification, WorkerSector requiredSector, String occupiedWorkerId) {
        this(id, requiredQualification, null, requiredSector, occupiedWorkerId);
    }

    public EnterpriseSlot(
            String id,
            WorkerQualification requiredQualification,
            WorkerSlotColor requiredColor,
            WorkerSector requiredSector,
            String occupiedWorkerId
    ) {
        this.id = id;
        this.requiredQualification = requiredQualification;
        this.requiredColor = requiredColor;
        this.requiredSector = requiredSector;
        this.occupiedWorkerId = occupiedWorkerId;
    }

    public EnterpriseSlot copy() {
        EnterpriseSlot copy = new EnterpriseSlot(id, requiredQualification, requiredColor, requiredSector, occupiedWorkerId);
        copy.setOptional(optional);
        return copy;
    }

    public boolean isOccupied() {
        return occupiedWorkerId != null && !occupiedWorkerId.isBlank();
    }
}
