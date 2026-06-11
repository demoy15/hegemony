package com.example.hegemony.domain;

import com.example.hegemony.application.setup.SetupSpecLoader;
import com.example.hegemony.application.setup.SetupSpecModel;
import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.Enterprise;
import com.example.hegemony.domain.model.WorkerQualification;
import com.example.hegemony.domain.model.WorkerSlotColor;
import com.example.hegemony.domain.model.WorkerSector;
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
    void setupRegistryContainsCapitalistEnterpriseCardsFromScans() {
        SetupSpecModel spec = new SetupSpecLoader("./missing-do-not-use.yaml").load();

        assertThat(spec.getCapitalistEnterpriseDefinitions()).hasSize(24);
        assertThat(spec.getCapitalistEnterpriseDefinitions().keySet())
                .contains(
                        "polyclinic",
                        "college",
                        "mall",
                        "supermarket",
                        "vegetable_farm",
                        "radio_station",
                        "hotel",
                        "telecom_company",
                        "academy",
                        "automobile_factory",
                        "university",
                        "fast_food_chain",
                        "electronics_manufacturer",
                        "fish_farm",
                        "publishing_house",
                        "stadium",
                        "hospital",
                        "medical_village",
                        "lobbying_firm",
                        "institute_of_technology",
                        "automated_grain_farm",
                        "fashion_company",
                        "pharmaceutical_company",
                        "automated_dairy_farm"
                );

        SetupSpecModel.EnterpriseDefinition clinic = spec.capitalistEnterpriseDefinition("polyclinic");
        assertThat(clinic.getName()).isEqualTo("Клиника");
        assertThat(clinic.getCost()).isEqualTo(16);
        assertThat(clinic.getProduction().getAmount()).isEqualTo(6);
        assertThat(clinic.getProduction().getPerWorkers()).isEqualTo(2);
        assertThat(clinic.getWorkers().getSlots().get(0).getColor()).isEqualTo(WorkerSlotColor.RED);
        assertThat(clinic.getWorkers().getSlots().get(0).getColor().toWorkerSector()).isEqualTo(WorkerSector.WHITE);

        SetupSpecModel.EnterpriseDefinition vegetableFarm = spec.capitalistEnterpriseDefinition("vegetable_farm");
        assertThat(vegetableFarm.getProduction().getAmount()).isEqualTo(5);
        assertThat(vegetableFarm.getProduction().getPerWorkers()).isNull();

        SetupSpecModel.EnterpriseDefinition stadium = spec.capitalistEnterpriseDefinition("stadium");
        assertThat(stadium.getCost()).isEqualTo(20);
        assertThat(stadium.getProduction().getAmount()).isEqualTo(8);
        assertThat(stadium.getProduction().getPerWorkers()).isEqualTo(3);

        SetupSpecModel.EnterpriseDefinition automatedDairyFarm = spec.capitalistEnterpriseDefinition("automated_dairy_farm");
        assertThat(automatedDairyFarm.isAutomated()).isTrue();
        assertThat(automatedDairyFarm.getWorkers().getSlots()).isEmpty();
        assertThat(automatedDairyFarm.getWages()).isNull();
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
