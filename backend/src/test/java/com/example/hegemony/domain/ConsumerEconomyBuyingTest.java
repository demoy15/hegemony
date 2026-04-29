package com.example.hegemony.domain;

import com.example.hegemony.domain.command.BuyGoodsAndServicesCommand;
import com.example.hegemony.domain.command.PurchaseItem;
import com.example.hegemony.domain.engine.ApplyCommandResult;
import com.example.hegemony.domain.engine.GameRulesEngine;
import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.PlayerState;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.SupplierType;
import com.example.hegemony.domain.rules.ValidationReasonCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConsumerEconomyBuyingTest {
    @Test
    void workerCanBuyFoodFromCapitalist() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);

        ApplyCommandResult result = engine.apply(state, new BuyGoodsAndServicesCommand(
                "worker",
                "FOOD",
                List.of(new PurchaseItem(SupplierType.CAPITALIST, "capitalist", 1))
        ));

        assertThat(result.validation().isValid()).isTrue();
    }

    @Test
    void workerCannotBuyFromMoreThanTwoSuppliers() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);

        ApplyCommandResult result = engine.apply(state, new BuyGoodsAndServicesCommand(
                "worker",
                "FOOD",
                List.of(
                        new PurchaseItem(SupplierType.CAPITALIST, "capitalist", 1),
                        new PurchaseItem(SupplierType.MIDDLE_CLASS, "middle_class", 1),
                        new PurchaseItem(SupplierType.EXTERNAL_MARKET, null, 1)
                )
        ));

        assertThat(result.validation().isValid()).isFalse();
        assertThat(result.validation().getReasonCodes()).contains(ValidationReasonCode.TOO_MANY_SUPPLIERS);
    }

    @Test
    void purchasePerSupplierCannotExceedPopulation() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);
        PlayerState worker = state.findPlayerById("worker").orElseThrow();

        ApplyCommandResult result = engine.apply(state, new BuyGoodsAndServicesCommand(
                "worker",
                "FOOD",
                List.of(new PurchaseItem(SupplierType.CAPITALIST, "capitalist", worker.getPopulation() + 1))
        ));

        assertThat(result.validation().isValid()).isFalse();
        assertThat(result.validation().getReasonCodes()).contains(ValidationReasonCode.PURCHASE_EXCEEDS_POPULATION_LIMIT);
    }

    @Test
    void cannotBuyIfSupplierHasInsufficientQuantity() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);

        ApplyCommandResult result = engine.apply(state, new BuyGoodsAndServicesCommand(
                "worker",
                "FOOD",
                List.of(new PurchaseItem(SupplierType.CAPITALIST, "capitalist", 99))
        ));

        assertThat(result.validation().isValid()).isFalse();
        assertThat(result.validation().getReasonCodes()).contains(ValidationReasonCode.SUPPLIER_INSUFFICIENT_QUANTITY);
    }

    @Test
    void cannotBuyIfBuyerHasInsufficientFunds() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);
        state.findPlayerById("worker").orElseThrow().setMoney(0);

        ApplyCommandResult result = engine.apply(state, new BuyGoodsAndServicesCommand(
                "worker",
                "FOOD",
                List.of(new PurchaseItem(SupplierType.CAPITALIST, "capitalist", 1))
        ));

        assertThat(result.validation().isValid()).isFalse();
        assertThat(result.validation().getReasonCodes()).contains(ValidationReasonCode.INSUFFICIENT_FUNDS);
    }

    @Test
    void unsupportedBuyerClassRejectedExplicitly() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);
        state.getTurnOrder().setCurrentPlayerIndex(2);

        ApplyCommandResult result = engine.apply(state, new BuyGoodsAndServicesCommand(
                "capitalist",
                "FOOD",
                List.of(new PurchaseItem(SupplierType.EXTERNAL_MARKET, null, 1))
        ));

        assertThat(result.validation().isValid()).isFalse();
        assertThat(result.validation().getReasonCodes()).contains(ValidationReasonCode.NOT_SUPPORTED_BUYER_CLASS);
    }

    @Test
    void buyingTransfersMoneyToCapitalistRevenue() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);
        PlayerState capitalist = state.findPlayerById("capitalist").orElseThrow();
        int before = capitalist.getRevenue();

        ApplyCommandResult result = engine.apply(state, new BuyGoodsAndServicesCommand(
                "worker",
                "FOOD",
                List.of(new PurchaseItem(SupplierType.CAPITALIST, "capitalist", 1))
        ));

        assertThat(result.validation().isValid()).isTrue();
        assertThat(result.resultingState().findPlayerById("capitalist").orElseThrow().getRevenue()).isGreaterThan(before);
    }

    @Test
    void manualUnitPriceOverrideControlsPurchaseCost() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);
        PlayerState worker = state.findPlayerById("worker").orElseThrow();
        PlayerState capitalist = state.findPlayerById("capitalist").orElseThrow();
        int workerMoneyBefore = worker.getMoney();
        int capitalistRevenueBefore = capitalist.getRevenue();

        ApplyCommandResult result = engine.apply(state, new BuyGoodsAndServicesCommand(
                "worker",
                "FOOD",
                List.of(new PurchaseItem(SupplierType.CAPITALIST, "capitalist", 1, 3))
        ));

        assertThat(result.validation().isValid()).isTrue();
        assertThat(result.resultingState().findPlayerById("worker").orElseThrow().getMoney())
                .isEqualTo(workerMoneyBefore - 3);
        assertThat(result.resultingState().findPlayerById("capitalist").orElseThrow().getRevenue())
                .isEqualTo(capitalistRevenueBefore + 3);
    }

    @Test
    void buyingTransfersMoneyToMiddleClassPool() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);
        PlayerState middle = state.findPlayerById("middle_class").orElseThrow();
        middle.setProducedResourceAmount("food", 3);
        middle.setPrice("food", 12);
        int before = middle.getMoney();

        ApplyCommandResult result = engine.apply(state, new BuyGoodsAndServicesCommand(
                "worker",
                "FOOD",
                List.of(new PurchaseItem(SupplierType.MIDDLE_CLASS, "middle_class", 1))
        ));

        assertThat(result.validation().isValid()).isTrue();
        assertThat(result.resultingState().findPlayerById("middle_class").orElseThrow().getMoney()).isGreaterThan(before);
    }

    @Test
    void buyingTransfersMoneyToStateTreasury() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);
        int before = state.getTreasury();

        ApplyCommandResult result = engine.apply(state, new BuyGoodsAndServicesCommand(
                "worker",
                "HEALTHCARE",
                List.of(new PurchaseItem(SupplierType.STATE, null, 1))
        ));

        assertThat(result.validation().isValid()).isTrue();
        assertThat(result.resultingState().getTreasury()).isGreaterThan(before);
    }

    @Test
    void externalMarketPurchaseDoesNotCreditAPlayer() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);
        int capitalistRevenue = state.findPlayerById("capitalist").orElseThrow().getRevenue();
        int middleMoney = state.findPlayerById("middle_class").orElseThrow().getMoney();
        int stateTreasury = state.getTreasury();

        ApplyCommandResult result = engine.apply(state, new BuyGoodsAndServicesCommand(
                "worker",
                "FOOD",
                List.of(new PurchaseItem(SupplierType.EXTERNAL_MARKET, null, 1))
        ));

        assertThat(result.validation().isValid()).isTrue();
        assertThat(result.resultingState().findPlayerById("capitalist").orElseThrow().getRevenue()).isEqualTo(capitalistRevenue);
        assertThat(result.resultingState().findPlayerById("middle_class").orElseThrow().getMoney()).isEqualTo(middleMoney);
        assertThat(result.resultingState().getTreasury()).isEqualTo(stateTreasury);
    }

    @Test
    void externalMarketFoodPriceUsesFixedBasePlusTariff() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);
        PlayerState worker = state.findPlayerById("worker").orElseThrow();
        int before = worker.getMoney();

        ApplyCommandResult result = engine.apply(state, new BuyGoodsAndServicesCommand(
                "worker",
                "FOOD",
                List.of(new PurchaseItem(SupplierType.EXTERNAL_MARKET, null, 1))
        ));

        assertThat(result.validation().isValid()).isTrue();
        assertThat(result.resultingState().findPlayerById("worker").orElseThrow().getMoney()).isEqualTo(before - 15);
    }

    @Test
    void externalMarketLuxuryTariffDropsToZeroUnderGlobalism() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);
        state.findPolicy(PolicyId.POLICY_6_FOREIGN_TRADE).orElseThrow().setCurrentCourse(PolicyCourse.C);
        PlayerState worker = state.findPlayerById("worker").orElseThrow();
        int before = worker.getMoney();

        ApplyCommandResult result = engine.apply(state, new BuyGoodsAndServicesCommand(
                "worker",
                "LUXURY",
                List.of(new PurchaseItem(SupplierType.EXTERNAL_MARKET, null, 1))
        ));

        assertThat(result.validation().isValid()).isTrue();
        assertThat(result.resultingState().findPlayerById("worker").orElseThrow().getMoney()).isEqualTo(before - 6);
    }

    @Test
    void buyingMovesResourcesToBuyerGoodsAndServicesArea() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);
        int before = state.findPlayerById("worker").orElseThrow().getGoodsAmount("food");

        ApplyCommandResult result = engine.apply(state, new BuyGoodsAndServicesCommand(
                "worker",
                "FOOD",
                List.of(new PurchaseItem(SupplierType.CAPITALIST, "capitalist", 1))
        ));

        assertThat(result.validation().isValid()).isTrue();
        assertThat(result.resultingState().findPlayerById("worker").orElseThrow().getGoodsAmount("food")).isEqualTo(before + 1);
    }
}
