package com.example.hegemony.domain;

import com.example.hegemony.domain.engine.GameRulesEngine;
import com.example.hegemony.domain.command.CapitalistActionCommand;
import com.example.hegemony.domain.model.ActionType;
import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.Enterprise;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.PlayerState;
import com.example.hegemony.domain.model.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StorageLimitTest {
    @Test
    void capitalistFoodOverflowMovesToFreeTradeZone() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = isolatedProductionState(ClassType.CAPITALIST, ResourceType.FOOD.id(), 20);
        PlayerState capitalist = state.findPlayerById("capitalist").orElseThrow();
        capitalist.setProducedResourceStorage(Map.of());
        capitalist.setFreeTradeZoneStorage(Map.of());

        GameState resolved = CoreTestSupport.resolveProduction(state, engine);
        PlayerState after = resolved.findPlayerById("capitalist").orElseThrow();

        assertThat(after.getProducedResourceAmount(ResourceType.FOOD.id())).isEqualTo(8);
        assertThat(after.getFreeTradeZoneAmount(ResourceType.FOOD.id())).isEqualTo(12);
        assertThat(resolved.getEventLog()).anySatisfy(entry -> assertThat(entry.getType()).isEqualTo("FREE_TRADE_ZONE_STORED"));
    }

    @Test
    void capitalistExtraStorageTokenExpandsStandardStorage() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = isolatedProductionState(ClassType.CAPITALIST, ResourceType.FOOD.id(), 16);
        PlayerState capitalist = state.findPlayerById("capitalist").orElseThrow();
        capitalist.setProducedResourceStorage(Map.of());
        capitalist.setFreeTradeZoneStorage(Map.of());
        capitalist.addExtraStorageToken(ResourceType.FOOD.id());

        GameState resolved = CoreTestSupport.resolveProduction(state, engine);
        PlayerState after = resolved.findPlayerById("capitalist").orElseThrow();

        assertThat(after.getProducedResourceAmount(ResourceType.FOOD.id())).isEqualTo(16);
        assertThat(after.getFreeTradeZoneAmount(ResourceType.FOOD.id())).isZero();
    }

    @Test
    void capitalistCanBuyOnlyOneStorageTokenPerResource() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);
        int capitalistIndex = state.getTurnOrder().getActiveClasses().indexOf(ClassType.CAPITALIST);
        state.getTurnOrder().setCurrentPlayerIndex(capitalistIndex);
        state.findPlayerById("capitalist").orElseThrow().setRevenue(40);

        var bought = engine.apply(state, new CapitalistActionCommand(
                ActionType.BUY_STORAGE,
                "capitalist",
                Map.of("resourceType", ResourceType.FOOD.id())
        ));
        assertThat(bought.validation().isValid()).isTrue();

        var duplicate = engine.apply(bought.resultingState(), new CapitalistActionCommand(
                ActionType.BUY_STORAGE,
                "capitalist",
                Map.of("resourceType", ResourceType.FOOD.id())
        ));

        assertThat(duplicate.validation().isValid()).isFalse();
        assertThat(duplicate.validation().getErrors()).anySatisfy(error -> assertThat(error).contains("Only one extra storage token"));
    }

    @Test
    void middleClassStorageBurnsOverflowAboveEight() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = isolatedProductionState(ClassType.MIDDLE_CLASS, ResourceType.LUXURY.id(), 10);
        PlayerState middleClass = state.findPlayerById("middle_class").orElseThrow();
        middleClass.setProducedResourceStorage(Map.of());

        GameState resolved = CoreTestSupport.resolveProduction(state, engine);
        PlayerState after = resolved.findPlayerById("middle_class").orElseThrow();

        assertThat(after.getProducedResourceAmount(ResourceType.LUXURY.id())).isEqualTo(8);
        assertThat(resolved.getEventLog()).anySatisfy(entry -> {
            assertThat(entry.getType()).isEqualTo("PRODUCTION_OVERFLOW");
            assertThat(entry.getMessage()).contains("burned 2 luxury");
        });
    }

    @Test
    void stateServiceStorageLimitIsCurrentProductionPlusSix() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = isolatedProductionState(ClassType.STATE, ResourceType.HEALTHCARE.id(), 10);
        state.setPublicServicesStorage(Map.of(ResourceType.HEALTHCARE.id(), 14));

        GameState resolved = CoreTestSupport.resolveProduction(state, engine);

        assertThat(resolved.getPublicServiceAmount(ResourceType.HEALTHCARE.id())).isEqualTo(16);
        assertThat(resolved.getEventLog()).anySatisfy(entry -> {
            assertThat(entry.getType()).isEqualTo("PRODUCTION_OVERFLOW");
            assertThat(entry.getMessage()).contains("burned 8 healthcare");
        });
    }

    private GameState isolatedProductionState(ClassType ownerClass, String resourceId, int amount) {
        GameState state = CoreTestSupport.stateInProductionPhase(4);
        state.getEnterprises().clear();
        state.findPlayerById("worker").ifPresent(player -> player.setGoodsAmount(ResourceType.FOOD.id(), player.getPopulation()));
        state.findPlayerById("middle_class").ifPresent(player -> player.setGoodsAmount(ResourceType.FOOD.id(), player.getPopulation()));

        Enterprise enterprise = new Enterprise();
        enterprise.setId("storage-test-" + ownerClass.name().toLowerCase());
        enterprise.setName("Storage test enterprise");
        enterprise.setOwnerClass(ownerClass);
        enterprise.setAutomated(true);
        enterprise.setProducedResources(Map.of(resourceId, amount));
        state.getEnterprises().add(enterprise);
        return state;
    }
}
