package com.example.hegemony.domain;

import com.example.hegemony.application.setup.SetupSpecLoader;
import com.example.hegemony.application.setup.SetupSpecModel;
import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.Enterprise;
import com.example.hegemony.domain.model.WorkerQualification;
import com.example.hegemony.domain.model.WorkerSlotColor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CapitalistEnterpriseRegistryTest {
    @Test
    void capitalistStartingLayoutMatchesSetupCardSet() {
        var capitalistEnterprises = CoreTestSupport.state(2).getEnterprises().stream()
                .filter(item -> item.getOwnerClass() == ClassType.CAPITALIST)
                .toList();

        assertThat(capitalistEnterprises).extracting(Enterprise::getId)
                .containsExactlyInAnyOrder("supermarket", "mall", "college", "polyclinic");
        assertThat(capitalistEnterprises).extracting(Enterprise::getId).doesNotContain("telecom_company");

        Enterprise mall = capitalistEnterprises.stream()
                .filter(item -> "mall".equals(item.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(mall.getProducedResources()).containsEntry("luxury", 6);
        assertThat(mall.getSlots()).hasSize(2);
        assertThat(mall.getSlots().stream().filter(slot -> slot.getRequiredColor() == WorkerSlotColor.BLUE)).hasSize(1);
        assertThat(mall.getSlots().stream().filter(slot -> slot.getRequiredQualification() == WorkerQualification.UNSKILLED)).hasSize(1);
    }

    @Test
    void setupRegistryContainsAutomatedEnterpriseDefinition() {
        SetupSpecModel spec = new SetupSpecLoader("./missing-do-not-use.yaml").load();
        SetupSpecModel.EnterpriseDefinition automobileFactory = spec.capitalistEnterpriseDefinition("automobile_factory");

        assertThat(automobileFactory).isNotNull();
        assertThat(automobileFactory.isAutomated()).isTrue();
        assertThat(automobileFactory.getWorkers().getSlots()).isEmpty();
        assertThat(automobileFactory.getWages()).isNull();
    }

    @Test
    void automatedEnterpriseFunctionsWithoutWorkers() {
        Enterprise enterprise = new Enterprise();
        enterprise.setAutomated(true);
        enterprise.setSlots(java.util.List.of());

        assertThat(enterprise.isFunctioning()).isTrue();

        enterprise.setAutomated(false);
        assertThat(enterprise.isFunctioning()).isFalse();
    }
}
