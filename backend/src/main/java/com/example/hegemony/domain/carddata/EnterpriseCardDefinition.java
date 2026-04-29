package com.example.hegemony.domain.carddata;

import com.example.hegemony.domain.model.ClassType;

import java.util.List;
import java.util.Map;

public record EnterpriseCardDefinition(
        String enterpriseCardId,
        String displayName,
        ClassType ownerClass,
        String sector,
        int cost,
        List<String> workerSlots,
        List<Integer> wageLevels,
        Map<String, Integer> producedResources,
        String sourceCompleteness
) {
}
