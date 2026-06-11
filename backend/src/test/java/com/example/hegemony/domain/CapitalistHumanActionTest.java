package com.example.hegemony.domain;

import com.example.hegemony.domain.command.CapitalistActionCommand;
import com.example.hegemony.domain.command.PlayCardCommand;
import com.example.hegemony.domain.engine.GameRulesEngine;
import com.example.hegemony.domain.model.ActionType;
import com.example.hegemony.domain.model.BusinessDealCard;
import com.example.hegemony.domain.model.BusinessDealRequirement;
import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.Enterprise;
import com.example.hegemony.domain.model.EnterpriseSlot;
import com.example.hegemony.domain.model.ExportCardOffer;
import com.example.hegemony.domain.model.ExportCardState;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.OrderedCardDeckState;
import com.example.hegemony.domain.model.PlayerState;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.ResourceType;
import com.example.hegemony.domain.model.Worker;
import com.example.hegemony.domain.model.WorkerLocation;
import com.example.hegemony.domain.model.WorkerQualification;
import com.example.hegemony.domain.model.WorkerSector;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CapitalistHumanActionTest {
    @Test
    void humanCapitalistLegalMovesIncludeMvpActionSet() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = capitalistTurnState();
        PlayerState capitalist = state.findPlayerById("capitalist").orElseThrow();
        capitalist.setProducedResourceAmount(ResourceType.FOOD.id(), 2);
        capitalist.setRevenue(120);
        capitalist.addResource("loan", 1);
        state.setActiveExportCard(exportCard(ResourceType.FOOD.id(), 2, 20));
        setVisibleBusinessDeal(state, new BusinessDealCard(
                "legal-move-test-deal",
                1,
                "Legal move test deal",
                List.of(new BusinessDealRequirement(ResourceType.FOOD.id(), 1)),
                0,
                6,
                0,
                0,
                null
        ));

        assertThat(engine.generateLegalMoves(state))
                .extracting(move -> move.actionType())
                .contains(
                        ActionType.BUILD_ENTERPRISE,
                        ActionType.SELL_ENTERPRISE,
                        ActionType.SELL_ON_EXTERNAL_MARKET,
                        ActionType.MAKE_BUSINESS_DEAL,
                        ActionType.LOBBY_INTERESTS,
                        ActionType.CHANGE_PRICES,
                        ActionType.CHANGE_WAGES,
                        ActionType.BUY_STORAGE,
                        ActionType.TAKE_STATE_BENEFITS,
                        ActionType.REPAY_LOAN
                );
    }

    @Test
    void lobbySpendsCapitalistFundsAndAddsInfluence() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = capitalistTurnState();
        PlayerState before = state.findPlayerById("capitalist").orElseThrow();
        before.setRevenue(120);
        int influenceBefore = before.getInfluence();

        var result = engine.apply(state, new CapitalistActionCommand(
                ActionType.LOBBY_INTERESTS,
                "capitalist",
                Map.of()
        ));

        assertThat(result.validation().isValid()).isTrue();
        PlayerState after = result.resultingState().findPlayerById("capitalist").orElseThrow();
        assertThat(after.getRevenue()).isEqualTo(90);
        assertThat(after.getInfluence()).isEqualTo(influenceBefore + 3);
        assertThat(result.producedEvents()).anySatisfy(event -> assertThat(event.type()).isEqualTo("CAPITALIST_LOBBIED"));
    }

    @Test
    void buildAndSellEnterpriseMutateBoardAndCapitalistFunds() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = capitalistTurnState();
        state.findPlayerById("capitalist").orElseThrow().setRevenue(120);
        int enterpriseCountBefore = state.getEnterprises().size();
        assertThat(state.getCapitalistEnterpriseMarket()).hasSize(4);
        String marketEnterpriseId = state.getCapitalistEnterpriseMarket().getFirst().getId();

        var built = engine.apply(state, new CapitalistActionCommand(
                ActionType.BUILD_ENTERPRISE,
                "capitalist",
                Map.of("enterpriseId", marketEnterpriseId)
        ));

        assertThat(built.validation().isValid()).isTrue();
        assertThat(built.resultingState().getEnterprises()).hasSize(enterpriseCountBefore + 1);
        assertThat(built.resultingState().findEnterprise(marketEnterpriseId)).isPresent();
        assertThat(built.resultingState().getCapitalistEnterpriseMarket())
                .extracting(enterprise -> enterprise.getId())
                .doesNotContain(marketEnterpriseId);

        var sold = engine.apply(built.resultingState(), new CapitalistActionCommand(
                ActionType.SELL_ENTERPRISE,
                "capitalist",
                Map.of("enterpriseId", marketEnterpriseId)
        ));

        assertThat(sold.validation().isValid()).isTrue();
        assertThat(sold.resultingState().findEnterprise(marketEnterpriseId)).isEmpty();
        assertThat(sold.resultingState().findPlayerById("capitalist").orElseThrow().getRevenue()).isGreaterThanOrEqualTo(120);
    }

    @Test
    void middleClassHumanCanApplyManualCorrection() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = middleClassTurnState();
        PlayerState middleClass = state.findPlayerById("middle_class").orElseThrow();
        middleClass.setMoney(10);
        middleClass.setWelfare(1);
        middleClass.setVictoryPoints(0);
        state.setTreasury(100);

        var result = engine.apply(state, new PlayCardCommand(
                "manual:state_to_actor_money:7;actor_welfare_delta:1;manual_victory_points_delta:middle_class:3"
        ));

        assertThat(result.validation().isValid()).isTrue();
        PlayerState after = result.resultingState().findPlayerById("middle_class").orElseThrow();
        assertThat(after.getMoney()).isEqualTo(17);
        assertThat(after.getWelfare()).isEqualTo(2);
        assertThat(after.getVictoryPoints()).isEqualTo(3);
        assertThat(result.resultingState().getTreasury()).isEqualTo(93);
    }

    @Test
    void capitalistHumanManualCorrectionUsesRevenue() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = capitalistTurnState();
        PlayerState capitalist = state.findPlayerById("capitalist").orElseThrow();
        capitalist.setMoney(4);
        capitalist.setRevenue(20);
        state.setTreasury(100);

        var result = engine.apply(state, new PlayCardCommand(
                "manual:state_to_actor_money:7;actor_money_delta:5"
        ));

        assertThat(result.validation().isValid()).isTrue();
        PlayerState after = result.resultingState().findPlayerById("capitalist").orElseThrow();
        assertThat(after.getRevenue()).isEqualTo(32);
        assertThat(after.getMoney()).isEqualTo(4);
        assertThat(result.resultingState().getTreasury()).isEqualTo(93);
    }

    @Test
    void middleClassBuildEnterpriseRequiresOwnWorkerAndStaffsItImmediately() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = middleClassTurnState();
        PlayerState middleClass = state.findPlayerById("middle_class").orElseThrow();
        middleClass.setMoney(40);
        var worker = CoreTestSupport.unemployedWorkers(state, ClassType.MIDDLE_CLASS).getFirst();
        CoreTestSupport.addUnemployedWorker(state, "worker-hired-for-middle-build", ClassType.WORKER, WorkerQualification.UNSKILLED);
        assertThat(state.getMiddleClassEnterpriseMarket()).hasSize(3);
        assertThat(state.getMiddleClassEnterpriseDeck()).hasSize(14);
        String marketEnterpriseId = state.getMiddleClassEnterpriseMarket().getFirst().getId();
        int cost = state.getMiddleClassEnterpriseMarket().getFirst().getCost();

        var result = engine.apply(state, new CapitalistActionCommand(
                ActionType.BUILD_ENTERPRISE,
                "middle_class",
                Map.of("enterpriseId", marketEnterpriseId, "hireWorkerClass", true, "wageLevel", 2)
        ));

        assertThat(result.validation().isValid()).isTrue();
        PlayerState after = result.resultingState().findPlayerById("middle_class").orElseThrow();
        assertThat(after.getMoney()).isEqualTo(40 - cost);
        var enterprise = result.resultingState().findEnterprise(marketEnterpriseId).orElseThrow();
        assertThat(enterprise.getOwnerClass()).isEqualTo(ClassType.MIDDLE_CLASS);
        assertThat(enterprise.isFunctioning()).isTrue();
        assertThat(result.resultingState().getMiddleClassEnterpriseMarket()).hasSize(3);
        assertThat(result.resultingState().getMiddleClassEnterpriseMarket())
                .extracting(Enterprise::getId)
                .doesNotContain(marketEnterpriseId)
                .contains("tutoring_company");
        var assigned = result.resultingState().findWorker(worker.getId()).orElseThrow();
        assertThat(assigned.getLocation()).isEqualTo(WorkerLocation.ENTERPRISE_SLOT);
        assertThat(assigned.getEnterpriseId()).isEqualTo(marketEnterpriseId);
    }

    @Test
    void middleClassBuildLegalMoveCarriesMarketEnterpriseParameters() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = middleClassTurnState();
        PlayerState middleClass = state.findPlayerById("middle_class").orElseThrow();
        middleClass.setMoney(40);
        String marketEnterpriseId = state.getMiddleClassEnterpriseMarket().getFirst().getId();
        int cost = state.getMiddleClassEnterpriseMarket().getFirst().getCost();

        assertThat(engine.generateLegalMoves(state))
                .filteredOn(move -> move.actionType() == ActionType.BUILD_ENTERPRISE)
                .anySatisfy(move -> {
                    assertThat(move.id()).contains(marketEnterpriseId);
                    assertThat(move.template()).containsEntry("actorPlayerId", "middle_class");
                    assertThat(move.template()).containsEntry("enterpriseId", marketEnterpriseId);
                    assertThat(move.template()).containsEntry("cost", cost);
                });
    }

    @Test
    void middleClassEnterpriseDeckUsesRealCardStats() {
        GameState state = middleClassTurnState();

        assertThat(state.getMiddleClassEnterpriseMarket()).hasSize(3);
        assertThat(state.getMiddleClassEnterpriseMarket().size() + state.getMiddleClassEnterpriseDeck().size()).isEqualTo(17);
        Enterprise doctorsOffice = state.getMiddleClassEnterpriseMarket().stream()
                .filter(enterprise -> enterprise.getId().equals("doctors_office"))
                .findFirst()
                .orElseThrow();
        assertThat(doctorsOffice.getCost()).isEqualTo(12);
        assertThat(doctorsOffice.getProducedResources()).containsEntry(ResourceType.HEALTHCARE.id(), 2);
        assertThat(doctorsOffice.getProductionPerWorkers()).isEqualTo(2);
        assertThat(doctorsOffice.getWageTrack()).containsEntry("low", 10).containsEntry("medium", 8).containsEntry("high", 6);
        assertThat(doctorsOffice.getSlots()).extracting(EnterpriseSlot::getId)
                .containsExactly("doctors_office-middle-slot-1", "doctors_office-hired-worker-slot-1");

        Enterprise prAgency = state.getMiddleClassEnterpriseMarket().stream()
                .filter(enterprise -> enterprise.getId().equals("pr_agency"))
                .findFirst()
                .orElseThrow();
        assertThat(prAgency.getCost()).isEqualTo(20);
        assertThat(prAgency.getProducedResources()).containsEntry("media", 3);
        assertThat(prAgency.getWageTrack()).containsEntry("low", 10).containsEntry("medium", 8).containsEntry("high", 6);
        assertThat(prAgency.getSlots()).extracting(EnterpriseSlot::getId)
                .containsExactly("pr_agency-middle-slot-1", "pr_agency-middle-slot-2", "pr_agency-hired-worker-slot-1");
    }

    @Test
    void middleClassWageLegalMoveCarriesEnterpriseTarget() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = middleClassTurnState();
        state.getEnterprises().add(enterprise("middle-wage-target", ClassType.MIDDLE_CLASS, 20, 1, List.of(new EnterpriseSlot(
                "middle-wage-target-hired-worker-slot-1",
                WorkerQualification.UNSKILLED,
                null,
                null
        ))));

        assertThat(engine.generateLegalMoves(state))
                .filteredOn(move -> move.actionType() == ActionType.CHANGE_WAGES)
                .anySatisfy(move -> {
                    assertThat(move.id()).contains("middle-wage-target");
                    assertThat(move.template()).containsEntry("actorPlayerId", "middle_class");
                    assertThat(move.template()).containsEntry("enterpriseId", "middle-wage-target");
                });
    }

    @Test
    void middleClassBuildCanUseOwnUntiedWorkersAndOptionallyHireWorkerClass() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = middleClassTurnState();
        PlayerState middleClass = state.findPlayerById("middle_class").orElseThrow();
        middleClass.setMoney(80);
        String marketEnterpriseId = state.getMiddleClassEnterpriseMarket().getFirst().getId();
        Worker movedWorker = CoreTestSupport.unemployedWorkers(state, ClassType.MIDDLE_CLASS).getFirst();
        Enterprise source = enterprise("middle-source", ClassType.MIDDLE_CLASS, 10, 1, List.of(new EnterpriseSlot(
                "middle-source-slot-1",
                WorkerQualification.UNSKILLED,
                null,
                movedWorker.getId()
        )));
        state.getEnterprises().add(source);
        movedWorker.setLocation(WorkerLocation.ENTERPRISE_SLOT);
        movedWorker.setEnterpriseId(source.getId());
        movedWorker.setSlotId("middle-source-slot-1");
        movedWorker.setTiedContract(false);
        CoreTestSupport.addUnemployedWorker(state, "worker-hired-by-middle", ClassType.WORKER, WorkerQualification.UNSKILLED);

        var result = engine.apply(state, new CapitalistActionCommand(
                ActionType.BUILD_ENTERPRISE,
                "middle_class",
                Map.of(
                        "enterpriseId", marketEnterpriseId,
                        "middleClassWorkerIds", List.of(movedWorker.getId()),
                        "hireWorkerClass", true,
                        "wageLevel", 2
                )
        ));

        assertThat(result.validation().isValid()).isTrue();
        Enterprise built = result.resultingState().findEnterprise(marketEnterpriseId).orElseThrow();
        assertThat(built.getSlots()).hasSize(2);
        assertThat(result.resultingState().findEnterprise("middle-source").orElseThrow().getSlots().getFirst().getOccupiedWorkerId()).isNull();
        assertThat(result.resultingState().findWorker(movedWorker.getId()).orElseThrow().isTiedContract()).isTrue();
        assertThat(result.resultingState().getWorkers())
                .filteredOn(worker -> worker.getClassType() == ClassType.WORKER)
                .filteredOn(worker -> built.getId().equals(worker.getEnterpriseId()))
                .anySatisfy(worker -> assertThat(worker.isTiedContract()).isTrue());
    }

    @Test
    void middleClassCannotSellEnterpriseWithTiedWorkersAndCanSellAfterContractsClear() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = middleClassTurnState();
        PlayerState middleClass = state.findPlayerById("middle_class").orElseThrow();
        middleClass.setMoney(40);
        String marketEnterpriseId = state.getMiddleClassEnterpriseMarket().getFirst().getId();
        int cost = state.getMiddleClassEnterpriseMarket().getFirst().getCost();

        var built = engine.apply(state, new CapitalistActionCommand(
                ActionType.BUILD_ENTERPRISE,
                "middle_class",
                Map.of("enterpriseId", marketEnterpriseId)
        ));
        assertThat(built.validation().isValid()).isTrue();

        var rejected = engine.apply(built.resultingState(), new CapitalistActionCommand(
                ActionType.SELL_ENTERPRISE,
                "middle_class",
                Map.of("enterpriseId", marketEnterpriseId)
        ));
        assertThat(rejected.validation().isValid()).isFalse();

        GameState contractsCleared = built.resultingState();
        Enterprise enterprise = contractsCleared.findEnterprise(marketEnterpriseId).orElseThrow();
        String workerId = enterprise.getSlots().getFirst().getOccupiedWorkerId();
        contractsCleared.findWorker(workerId).orElseThrow().setTiedContract(false);
        int moneyBeforeSale = contractsCleared.findPlayerById("middle_class").orElseThrow().getMoney();

        var sold = engine.apply(contractsCleared, new CapitalistActionCommand(
                ActionType.SELL_ENTERPRISE,
                "middle_class",
                Map.of("enterpriseId", marketEnterpriseId)
        ));

        assertThat(sold.validation().isValid()).isTrue();
        assertThat(sold.resultingState().findEnterprise(marketEnterpriseId)).isEmpty();
        assertThat(sold.resultingState().findPlayerById("middle_class").orElseThrow().getMoney()).isEqualTo(moneyBeforeSale + cost);
        Worker released = sold.resultingState().findWorker(workerId).orElseThrow();
        assertThat(released.getLocation()).isEqualTo(WorkerLocation.UNEMPLOYED);
        assertThat(released.isTiedContract()).isFalse();
    }

    @Test
    void middleClassCanChangePricesWagesAndRepayLoan() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = middleClassTurnState();
        PlayerState middleClass = state.findPlayerById("middle_class").orElseThrow();
        middleClass.setMoney(90);
        middleClass.addResource("loan", 1);
        CoreTestSupport.policy(state, PolicyId.POLICY_2_LABOR_MARKET).setCurrentCourse(PolicyCourse.C);
        Worker hiredWorker = CoreTestSupport.addUnemployedWorker(state, "worker-middle-wage", ClassType.WORKER, WorkerQualification.UNSKILLED);
        Enterprise enterprise = enterprise("middle-wage-enterprise", ClassType.MIDDLE_CLASS, 20, 1, List.of(new EnterpriseSlot(
                "middle-wage-enterprise-slot-1",
                WorkerQualification.UNSKILLED,
                null,
                hiredWorker.getId()
        )));
        state.getEnterprises().add(enterprise);
        hiredWorker.setLocation(WorkerLocation.ENTERPRISE_SLOT);
        hiredWorker.setEnterpriseId(enterprise.getId());
        hiredWorker.setSlotId("middle-wage-enterprise-slot-1");
        hiredWorker.setTiedContract(false);

        var pricesChanged = engine.apply(state, new CapitalistActionCommand(
                ActionType.CHANGE_PRICES,
                "middle_class",
                Map.of("prices", Map.of(ResourceType.FOOD.id(), 4, ResourceType.LUXURY.id(), 5))
        ));
        assertThat(pricesChanged.validation().isValid()).isTrue();
        assertThat(pricesChanged.resultingState().findPlayerById("middle_class").orElseThrow().getPrice(ResourceType.FOOD.id())).isEqualTo(4);

        var wagesRaised = engine.apply(pricesChanged.resultingState(), new CapitalistActionCommand(
                ActionType.CHANGE_WAGES,
                "middle_class",
                Map.of("enterpriseId", enterprise.getId(), "wageLevel", 2)
        ));
        assertThat(wagesRaised.validation().isValid()).isTrue();
        assertThat(wagesRaised.resultingState().findWorker(hiredWorker.getId()).orElseThrow().isTiedContract()).isTrue();

        var wagesLowered = engine.apply(wagesRaised.resultingState(), new CapitalistActionCommand(
                ActionType.CHANGE_WAGES,
                "middle_class",
                Map.of("enterpriseId", enterprise.getId(), "wageLevel", 1)
        ));
        assertThat(wagesLowered.validation().isValid()).isFalse();

        var loanRepaid = engine.apply(wagesRaised.resultingState(), new CapitalistActionCommand(
                ActionType.REPAY_LOAN,
                "middle_class",
                Map.of()
        ));
        assertThat(loanRepaid.validation().isValid()).isTrue();
        assertThat(loanRepaid.resultingState().findPlayerById("middle_class").orElseThrow().getMoney()).isEqualTo(40);
        assertThat(loanRepaid.resultingState().findPlayerById("middle_class").orElseThrow().getResourceAmount("loan")).isZero();
    }

    @Test
    void externalMarketSaleMovesStoredGoodsIntoRevenue() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = capitalistTurnState();
        PlayerState capitalist = state.findPlayerById("capitalist").orElseThrow();
        capitalist.setRevenue(0);
        capitalist.setProducedResourceStorage(Map.of(ResourceType.FOOD.id(), 1));
        capitalist.setFreeTradeZoneStorage(Map.of(ResourceType.FOOD.id(), 1));
        capitalist.setGoodsAmount(ResourceType.FOOD.id(), 99);
        state.setActiveExportCard(exportCard(ResourceType.FOOD.id(), 2, 20));

        var result = engine.apply(state, new CapitalistActionCommand(
                ActionType.SELL_ON_EXTERNAL_MARKET,
                "capitalist",
                Map.of()
        ));

        assertThat(result.validation().isValid()).isTrue();
        PlayerState after = result.resultingState().findPlayerById("capitalist").orElseThrow();
        assertThat(after.getProducedResourceAmount(ResourceType.FOOD.id())).isZero();
        assertThat(after.getFreeTradeZoneAmount(ResourceType.FOOD.id())).isZero();
        assertThat(after.getGoodsAmount(ResourceType.FOOD.id())).isEqualTo(99);
        assertThat(after.getRevenue()).isEqualTo(20);
        assertThat(result.resultingState().getActiveExportCard().getAvailableOperations()).isEqualTo(7);
    }

    @Test
    void middleClassExternalMarketSaleGainsVictoryPointPerOperation() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = middleClassTurnState();
        PlayerState middleClass = state.findPlayerById("middle_class").orElseThrow();
        middleClass.setMoney(0);
        middleClass.setVictoryPoints(0);
        middleClass.setProducedResourceStorage(Map.of(ResourceType.LUXURY.id(), 2));
        state.setActiveExportCard(exportCard(ResourceType.LUXURY.id(), 2, 14));

        var result = engine.apply(state, new CapitalistActionCommand(
                ActionType.SELL_ON_EXTERNAL_MARKET,
                "middle_class",
                Map.of()
        ));

        assertThat(result.validation().isValid()).isTrue();
        PlayerState after = result.resultingState().findPlayerById("middle_class").orElseThrow();
        assertThat(after.getProducedResourceAmount(ResourceType.LUXURY.id())).isZero();
        assertThat(after.getMoney()).isEqualTo(14);
        assertThat(after.getVictoryPoints()).isEqualTo(1);
    }

    @Test
    void businessDealBuysVisibleDealGoodsAndPaysDutyToTreasury() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = capitalistTurnState();
        PlayerState capitalist = state.findPlayerById("capitalist").orElseThrow();
        capitalist.setRevenue(30);
        capitalist.setProducedResourceStorage(Map.of(ResourceType.FOOD.id(), 7));
        state.findPolicy(PolicyId.POLICY_6_FOREIGN_TRADE).orElseThrow().setCurrentCourse(PolicyCourse.B);
        state.setTreasury(0);
        setVisibleBusinessDeal(state, new BusinessDealCard(
                "test-deal",
                1,
                "Test deal",
                List.of(new BusinessDealRequirement(ResourceType.FOOD.id(), 3)),
                0,
                6,
                0,
                0,
                null
        ));

        var result = engine.apply(state, new CapitalistActionCommand(
                ActionType.MAKE_BUSINESS_DEAL,
                "capitalist",
                Map.of("dealId", "test-deal")
        ));

        assertThat(result.validation().isValid()).isTrue();
        PlayerState after = result.resultingState().findPlayerById("capitalist").orElseThrow();
        assertThat(after.getProducedResourceAmount(ResourceType.FOOD.id())).isEqualTo(8);
        assertThat(after.getFreeTradeZoneAmount(ResourceType.FOOD.id())).isEqualTo(2);
        assertThat(after.getRevenue()).isEqualTo(23);
        assertThat(result.resultingState().getTreasury()).isEqualTo(1);
        assertThat(result.resultingState().getBusinessDealDeck().getVisibleCardIds()).doesNotContain("test-deal");
    }

    @Test
    void humanStateLegalMovesIncludeStateActionSet() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = stateTurnState();
        PlayerState statePlayer = state.findPlayerById("state").orElseThrow();
        statePlayer.setInfluence(2);
        statePlayer.setLegitimacyWorker(2);
        statePlayer.setLegitimacyMiddleClass(2);
        statePlayer.setLegitimacyCapitalist(2);
        statePlayer.addProducedResource(ResourceType.FOOD.id(), 2);
        state.addPublicServiceAmount(ResourceType.MEDIA_INFLUENCE.id(), 3);
        state.setStateLoans(1);
        state.setTreasury(60);
        state.setActiveExportCard(exportCard(ResourceType.FOOD.id(), 2, 20));

        assertThat(engine.generateLegalMoves(state))
                .extracting(move -> move.actionType())
                .contains(
                        ActionType.PROPOSE_BILL,
                        ActionType.RESPOND_TO_EVENT,
                        ActionType.SELL_ON_EXTERNAL_MARKET,
                        ActionType.MEET_DEPUTIES,
                        ActionType.INTRODUCE_EXTRA_TAX,
                        ActionType.RUN_CAMPAIGN,
                        ActionType.CHANGE_WAGES,
                        ActionType.REPAY_LOAN
                );
    }

    @Test
    void humanStateCanRunCoreMvpActions() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = stateTurnState();
        PlayerState statePlayer = state.findPlayerById("state").orElseThrow();
        statePlayer.setInfluence(2);
        statePlayer.setLegitimacyWorker(2);
        statePlayer.setLegitimacyMiddleClass(2);
        statePlayer.setLegitimacyCapitalist(2);
        state.addPublicServiceAmount(ResourceType.MEDIA_INFLUENCE.id(), 3);

        var deputies = engine.apply(state, new CapitalistActionCommand(
                ActionType.MEET_DEPUTIES,
                "state",
                Map.of("targetClass", "WORKER")
        ));
        assertThat(deputies.validation().isValid()).isTrue();
        assertThat(deputies.resultingState().findPlayerById("state").orElseThrow().getInfluence()).isZero();
        assertThat(deputies.resultingState().findPlayerById("worker").orElseThrow().getInfluence()).isGreaterThanOrEqualTo(2);
        assertThat(deputies.resultingState().findPlayerById("state").orElseThrow().getLegitimacyWorker()).isEqualTo(3);

        GameState campaignState = stateTurnState();
        campaignState.findPlayerById("state").orElseThrow().setInfluence(0);
        campaignState.setPublicServicesStorage(Map.of(ResourceType.MEDIA_INFLUENCE.id(), 3));
        var campaign = engine.apply(campaignState, new CapitalistActionCommand(
                ActionType.RUN_CAMPAIGN,
                "state",
                Map.of("amount", 3)
        ));
        assertThat(campaign.validation().isValid()).isTrue();
        assertThat(campaign.resultingState().findPlayerById("state").orElseThrow().getInfluence()).isEqualTo(3);
        assertThat(campaign.resultingState().getPublicServiceAmount(ResourceType.MEDIA_INFLUENCE.id())).isZero();
    }

    @Test
    void stateEventDeckIsAvailableOnlyWithStatePlayer() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = stateTurnState();

        var eventMoves = engine.generateLegalCommands(state).stream()
                .filter(CapitalistActionCommand.class::isInstance)
                .map(CapitalistActionCommand.class::cast)
                .filter(command -> command.type() == ActionType.RESPOND_TO_EVENT)
                .toList();

        assertThat(state.getStateEventCards()).hasSize(25);
        assertThat(state.getStateEventDeck().getVisibleCardIds()).containsExactly(
                "urgent_deflation_concern",
                "enterprise_internet_speed"
        );
        assertThat(eventMoves).anySatisfy(command -> assertThat(command.parameters()).containsEntry("eventId", "urgent_deflation_concern"));

        GameState twoPlayerState = CoreTestSupport.state(2);
        engine.ensureStateEventDeck(twoPlayerState);
        assertThat(twoPlayerState.getStateEventCards()).isEmpty();
        assertThat(twoPlayerState.getStateEventDeck().getVisibleCardIds()).isEmpty();
    }

    @Test
    void stateCanResolveEventResponseAndDrawNextEvent() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = stateTurnState();
        state.setTreasury(100);

        var result = engine.apply(state, new CapitalistActionCommand(
                ActionType.RESPOND_TO_EVENT,
                "state",
                Map.of("eventId", "urgent_deflation_concern", "targetClass", "STATE", "eventOption", "45", "amount", 45)
        ));

        assertThat(result.validation().isValid()).as(result.validation().getErrors().toString()).isTrue();
        GameState resolved = result.resultingState();
        assertThat(resolved.findPlayerById("state").orElseThrow().getVictoryPoints()).isEqualTo(6);
        assertThat(resolved.getTreasury()).isEqualTo(55);
        assertThat(resolved.getStateEventDeck().getVisibleCardIds()).containsExactly(
                "enterprise_internet_speed",
                "neighboring_states_war"
        );
        assertThat(resolved.getEventLog()).anySatisfy(entry -> assertThat(entry.getType()).isEqualTo("STATE_EVENT_RESPONDED"));
    }

    @Test
    void stateEventNoActionAppliesLegitimacyPenalty() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = stateTurnState();
        PlayerState statePlayer = state.findPlayerById("state").orElseThrow();
        statePlayer.setLegitimacyWorker(2);
        statePlayer.setLegitimacyMiddleClass(3);
        statePlayer.setLegitimacyCapitalist(4);

        var result = engine.apply(state, new CapitalistActionCommand(
                ActionType.RESPOND_TO_EVENT,
                "state",
                Map.of("eventId", "urgent_deflation_concern", "noAction", true)
        ));

        assertThat(result.validation().isValid()).as(result.validation().getErrors().toString()).isTrue();
        PlayerState resolvedStatePlayer = result.resultingState().findPlayerById("state").orElseThrow();
        assertThat(resolvedStatePlayer.getLegitimacyWorker()).isEqualTo(1);
        assertThat(resolvedStatePlayer.getLegitimacyMiddleClass()).isEqualTo(2);
        assertThat(resolvedStatePlayer.getLegitimacyCapitalist()).isEqualTo(4);
    }

    private GameState capitalistTurnState() {
        GameState state = CoreTestSupport.state(4);
        int capitalistIndex = state.getTurnOrder().getActiveClasses().indexOf(ClassType.CAPITALIST);
        state.getTurnOrder().setCurrentPlayerIndex(capitalistIndex);
        return state;
    }

    private GameState stateTurnState() {
        GameState state = CoreTestSupport.state(4);
        int stateIndex = state.getTurnOrder().getActiveClasses().indexOf(ClassType.STATE);
        state.getTurnOrder().setCurrentPlayerIndex(stateIndex);
        return state;
    }

    private GameState middleClassTurnState() {
        GameState state = CoreTestSupport.state(4);
        int middleClassIndex = state.getTurnOrder().getActiveClasses().indexOf(ClassType.MIDDLE_CLASS);
        state.getTurnOrder().setCurrentPlayerIndex(middleClassIndex);
        return state;
    }

    private Enterprise enterprise(String id, ClassType ownerClass, int cost, int wageLevel, List<EnterpriseSlot> slots) {
        Enterprise enterprise = new Enterprise();
        enterprise.setId(id);
        enterprise.setName(id);
        enterprise.setCategory("test");
        enterprise.setCost(cost);
        enterprise.setOwnerClass(ownerClass);
        enterprise.setSector(WorkerSector.GENERAL);
        enterprise.setWageLevel(wageLevel);
        enterprise.setProductionAmount(1);
        enterprise.setProducedResources(Map.of(ResourceType.FOOD.id(), 1));
        enterprise.setSlots(slots);
        return enterprise;
    }

    private ExportCardState exportCard(String resourceId, int quantity, int revenue) {
        return new ExportCardState(
                "test-export",
                "Test export",
                "Test export card",
                8,
                1,
                false,
                1,
                java.util.List.of(new ExportCardOffer(resourceId, quantity, revenue)),
                null
        );
    }

    private void setVisibleBusinessDeal(GameState state, BusinessDealCard card) {
        state.setBusinessDealCards(List.of(card));
        OrderedCardDeckState deck = new OrderedCardDeckState();
        deck.setDeckId("business-deals");
        deck.setOrderedCardIds(List.of(card.getId()));
        deck.setVisibleCardIds(List.of(card.getId()));
        deck.setVisibleWindowSize(1);
        deck.setNextCardIndex(0);
        state.setBusinessDealDeck(deck);
    }
}
