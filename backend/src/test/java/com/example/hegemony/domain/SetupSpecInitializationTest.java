package com.example.hegemony.domain;

import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.WorkerQualification;
import com.example.hegemony.domain.model.WorkerSlotColor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SetupSpecInitializationTest {
    @Test
    void createInitialGame_2Players_matchesSpec() {
        GameState state = CoreTestSupport.state(2);

        assertThat(state.getPlayers()).extracting(p -> p.getClassType()).containsExactly(ClassType.WORKER, ClassType.CAPITALIST);
        assertThat(state.getPolicies()).hasSize(7);
        assertThat(CoreTestSupport.policy(state, PolicyId.POLICY_1_FISCAL).getCurrentCourse()).isEqualTo(PolicyCourse.C);
        assertThat(CoreTestSupport.policy(state, PolicyId.POLICY_3_TAXATION).getCurrentCourse()).isEqualTo(PolicyCourse.A);
        assertThat(CoreTestSupport.policy(state, PolicyId.POLICY_5_EDUCATION).getCurrentCourse()).isEqualTo(PolicyCourse.C);

        assertThat(state.getTreasury()).isEqualTo(120);
        assertThat(state.getPublicServices().getHealthcare()).isEqualTo(5);
        assertThat(state.getPublicServices().getEducation()).isEqualTo(5);
        assertThat(state.getPublicServices().getMediaInfluence()).isEqualTo(3);

        assertThat(state.getVotingBag().getWorker()).isEqualTo(8);
        assertThat(state.getVotingBag().getMiddleClass()).isEqualTo(8);
        assertThat(state.getVotingBag().getCapitalist()).isEqualTo(8);

        assertThat(state.getEnterprises()).isNotEmpty();
        assertThat(state.getEnterprises().stream()
                .filter(enterprise -> enterprise.getOwnerClass() == ClassType.STATE)
                .map(enterprise -> enterprise.getId()))
                .containsExactlyInAnyOrder("state_hospital", "state_university", "state_media");
        assertThat(state.findEnterprise("state_hospital").orElseThrow().getProducedResources()).containsEntry("healthcare", 4);
        assertThat(state.findEnterprise("state_university").orElseThrow().getProducedResources()).containsEntry("education", 4);
        assertThat(state.findEnterprise("state_media").orElseThrow().getProducedResources()).containsEntry("media_influence", 3);
        assertThat(state.findEnterprise("state_hospital").orElseThrow().getWageLevel()).isEqualTo(2);
        assertThat(state.findEnterprise("state_hospital").orElseThrow().getWageTrack())
                .containsEntry("low", 15)
                .containsEntry("medium", 20)
                .containsEntry("high", 25);
        assertThat(state.findEnterprise("state_hospital").orElseThrow().getSlots())
                .filteredOn(slot -> slot.getRequiredQualification() == WorkerQualification.SKILLED)
                .extracting(slot -> slot.getRequiredColor())
                .containsOnly(WorkerSlotColor.WHITE);
        assertThat(state.findEnterprise("state_university").orElseThrow().getSlots())
                .filteredOn(slot -> slot.getRequiredQualification() == WorkerQualification.SKILLED)
                .extracting(slot -> slot.getRequiredColor())
                .containsOnly(WorkerSlotColor.ORANGE);
        assertThat(state.findEnterprise("state_media").orElseThrow().getSlots())
                .filteredOn(slot -> slot.getRequiredQualification() == WorkerQualification.SKILLED)
                .extracting(slot -> slot.getRequiredColor())
                .containsOnly(WorkerSlotColor.PURPLE);
        assertThat(state.findEnterprise("state_media").orElseThrow().getSlots())
                .filteredOn(slot -> slot.getRequiredQualification() == WorkerQualification.UNSKILLED)
                .hasSize(1);
        assertThat(state.getEnterprises().stream()
                .filter(enterprise -> enterprise.getOwnerClass() == ClassType.CAPITALIST)
                .map(enterprise -> enterprise.getId()))
                .containsExactlyInAnyOrder("supermarket", "mall", "college", "polyclinic");
        assertThat(state.findEnterprise("supermarket").orElseThrow().getSlots())
                .filteredOn(slot -> slot.getRequiredQualification() == WorkerQualification.SKILLED)
                .extracting(slot -> slot.getRequiredColor())
                .containsOnly(WorkerSlotColor.GREEN);
        assertThat(state.findEnterprise("supermarket").orElseThrow().getSlots())
                .filteredOn(slot -> slot.getRequiredQualification() == WorkerQualification.UNSKILLED)
                .extracting(slot -> slot.getRequiredColor())
                .containsOnly(WorkerSlotColor.GRAY);
        assertThat(state.getWorkers()).isNotEmpty();
        assertThat(state.getWorkers().stream().anyMatch(w -> w.getLocation().name().equals("ENTERPRISE_SLOT"))).isTrue();
        assertThat(state.findPlayerById("worker").orElseThrow().getMoney()).isEqualTo(30);
        assertThat(state.findPlayerById("capitalist").orElseThrow().getProducedResourceAmount("food")).isEqualTo(1);
        assertThat(state.findPlayerById("capitalist").orElseThrow().getProducedResourceAmount("luxury")).isEqualTo(2);
        assertThat(state.findPlayerById("capitalist").orElseThrow().getProducedResourceAmount("education")).isEqualTo(2);
        assertThat(state.findPlayerById("capitalist").orElseThrow().getProducedResourceAmount("influence")).isEqualTo(0);
        assertThat(state.findPlayerById("capitalist").orElseThrow().getPrice("healthcare")).isEqualTo(8);
        assertThat(state.findPlayerById("capitalist").orElseThrow().getPrice("influence")).isEqualTo(0);
    }

    @Test
    void createInitialGame_3Players_matchesSpec() {
        GameState state = CoreTestSupport.state(3);

        assertThat(state.getPlayers()).extracting(p -> p.getClassType())
                .containsExactly(ClassType.WORKER, ClassType.MIDDLE_CLASS, ClassType.CAPITALIST);
        assertThat(state.getPublicServices().getHealthcare()).isEqualTo(6);
        assertThat(state.getPublicServices().getEducation()).isEqualTo(6);
        assertThat(state.getPublicServices().getMediaInfluence()).isEqualTo(4);
        assertThat(state.getEnterprises().stream()
                .filter(enterprise -> enterprise.getOwnerClass() == ClassType.STATE)
                .map(enterprise -> enterprise.getId()))
                .containsExactlyInAnyOrder("university_hospital", "technical_university", "state_media");
        assertThat(state.findPlayerById("middle_class").orElseThrow().getMoney()).isEqualTo(40);
        assertThat(state.findPlayerById("middle_class").orElseThrow().getResources()).containsEntry("food", 1);
        assertThat(state.findEnterprise("minimarket").orElseThrow().getWageTrack())
                .containsEntry("low", 10)
                .containsEntry("medium", 8)
                .containsEntry("high", 6);
        assertThat(state.findEnterprise("minimarket").orElseThrow().getSlots())
                .anySatisfy(slot -> {
                    assertThat(slot.isOptional()).isTrue();
                    assertThat(slot.getRequiredQualification()).isEqualTo(WorkerQualification.UNSKILLED);
                    assertThat(slot.getRequiredColor()).isEqualTo(WorkerSlotColor.GRAY);
                });
    }

    @Test
    void createInitialGame_4Players_matchesSpec() {
        GameState state = CoreTestSupport.state(4);

        assertThat(state.getPlayers()).extracting(p -> p.getClassType())
                .containsExactly(ClassType.WORKER, ClassType.MIDDLE_CLASS, ClassType.CAPITALIST, ClassType.STATE);
        assertThat(state.getTurnOrder().getActiveClasses()).containsExactly(ClassType.WORKER, ClassType.MIDDLE_CLASS, ClassType.CAPITALIST, ClassType.STATE);
        assertThat(state.findPlayerById("state").orElseThrow().getInfluence()).isEqualTo(1);
        assertThat(state.getPolicies()).allSatisfy(policy -> assertThat(policy.getCurrentCourse()).isNotNull());
        assertThat(state.getEnterprises().stream()
                .filter(enterprise -> enterprise.getOwnerClass() == ClassType.STATE)
                .map(enterprise -> enterprise.getId()))
                .containsExactlyInAnyOrder("university_hospital", "technical_university", "state_media");
        assertThat(state.getEnterprises()).isNotEmpty();
        assertThat(state.getWorkers()).isNotEmpty();
    }
}
