package com.example.hegemony.application;

import com.example.hegemony.application.setup.SetupSpecLoader;
import com.example.hegemony.domain.CoreTestSupport;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaActionCard;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaActionSymbol;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaBonus;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaCardRegistry;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaInstructionCard;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaInstructionMode;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaInstructionType;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaPolicyTag;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaSpecialAction;
import com.example.hegemony.domain.model.BotStrategyMode;
import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.Enterprise;
import com.example.hegemony.domain.model.EnterpriseSlot;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.PlayerControlMode;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.RoundPhase;
import com.example.hegemony.domain.model.WorkerQualification;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AutomaSimpleModeTurnServiceTest {
    @Test
    void first_action_executes_when_bonus_makes_it_possible() {
        GameState state = prepareCapitalistSimpleTurnState();
        var capitalist = state.findPlayerById("capitalist").orElseThrow();
        capitalist.setMoney(10);
        ensureUnemployedWorkers(state, 4);

        CapitalistAutomaActionCard card = card(
                301,
                List.of(CapitalistAutomaActionSymbol.BUILD_ENTERPRISE, CapitalistAutomaActionSymbol.PROPOSE_BILL),
                List.of(),
                new CapitalistAutomaBonus("first_action_bonus", "money_discount", Map.of("amount", 10), "bonus"),
                null
        );
        AutomaSimpleModeTurnService service = serviceWithCards(List.of(card));

        var actor = state.currentPlayer();
        Optional<AutomaSimpleModeTurnService.ResolvedTurn> result = service.resolveAndApply(state, actor);

        assertThat(result).isPresent();
        assertThat(result.get().summary().getAutomaTrace().get("selectedAutomaAction")).isEqualTo("BUILD_ENTERPRISE");
        assertThat(result.get().summary().getAutomaTrace().get("firstActionBonusApplied")).isEqualTo(true);
    }

    @Test
    void falls_back_to_second_action_when_first_unavailable() {
        GameState state = prepareCapitalistSimpleTurnState();
        var capitalist = state.findPlayerById("capitalist").orElseThrow();
        capitalist.setMoney(0);

        CapitalistAutomaActionCard card = card(
                302,
                List.of(CapitalistAutomaActionSymbol.BUILD_ENTERPRISE, CapitalistAutomaActionSymbol.PROPOSE_BILL),
                List.of(),
                null,
                null
        );
        AutomaSimpleModeTurnService service = serviceWithCards(List.of(card));

        var result = service.resolveAndApply(state, state.currentPlayer()).orElseThrow();
        assertThat(result.summary().getSelectedAction().name()).isEqualTo("PROPOSE_BILL");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks = (List<Map<String, Object>>) result.summary().getAutomaTrace().get("checkedSlots");
        assertThat(checks.get(0).get("canExecute")).isEqualTo(false);
        assertThat(checks.get(1).get("canExecute")).isEqualTo(true);
    }

    @Test
    void falls_back_to_third_then_fourth_in_order() {
        GameState state = prepareCapitalistSimpleTurnState();
        var capitalist = state.findPlayerById("capitalist").orElseThrow();
        capitalist.setMoney(0);

        CapitalistAutomaActionCard card = card(
                303,
                List.of(
                        CapitalistAutomaActionSymbol.BUILD_ENTERPRISE,
                        CapitalistAutomaActionSymbol.SELL_ON_EXTERNAL_MARKET,
                        CapitalistAutomaActionSymbol.LOBBY_INTERESTS,
                        CapitalistAutomaActionSymbol.PROPOSE_BILL
                ),
                List.of(),
                null,
                null
        );
        AutomaSimpleModeTurnService service = serviceWithCards(List.of(card));

        var result = service.resolveAndApply(state, state.currentPlayer()).orElseThrow();
        assertThat(result.summary().getSelectedAction().name()).isEqualTo("PROPOSE_BILL");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks = (List<Map<String, Object>>) result.summary().getAutomaTrace().get("checkedSlots");
        assertThat(checks.get(0).get("canExecute")).isEqualTo(false);
        assertThat(checks.get(1).get("canExecute")).isEqualTo(false);
        assertThat(checks.get(2).get("canExecute")).isEqualTo(false);
        assertThat(checks.get(3).get("canExecute")).isEqualTo(true);
    }

    @Test
    void reactsToStrikeByRaisingWagesWhenRevealedCardHasSpeechSymbol() {
        GameState state = prepareCapitalistSimpleTurnState();
        Enterprise target = state.getEnterprises().stream()
                .filter(enterprise -> enterprise.getOwnerClass() == ClassType.CAPITALIST)
                .filter(enterprise -> enterprise.getSlots().stream().anyMatch(EnterpriseSlot::isOccupied))
                .findFirst()
                .orElseThrow();
        target.setStrikeToken(true);
        target.setWageLevel(1);

        CapitalistAutomaActionCard card = card(
                313,
                List.of(CapitalistAutomaActionSymbol.REACT_TO_STRIKE, CapitalistAutomaActionSymbol.LOBBY_INTERESTS),
                List.of(),
                null,
                null
        );
        AutomaSimpleModeTurnService service = serviceWithCards(List.of(card));

        var result = service.resolveAndApply(state, state.currentPlayer()).orElseThrow();

        Enterprise after = result.state().findEnterprise(target.getId()).orElseThrow();
        assertThat(after.getWageLevel()).isEqualTo(3);
        assertThat(after.isStrikeToken()).isTrue();
        assertThat(result.state().getEventLog())
                .anySatisfy(entry -> assertThat(entry.getType()).isEqualTo("AUTOMA_STRIKE_REACTION"));
        assertThat(after.getSlots())
                .filteredOn(EnterpriseSlot::isOccupied)
                .allSatisfy(slot -> assertThat(result.state().findWorker(slot.getOccupiedWorkerId()).orElseThrow().isTiedContract()).isTrue());
    }

    @Test
    void uses_special_action_when_all_four_base_actions_unavailable() {
        GameState state = prepareCapitalistSimpleTurnState();
        var capitalist = state.findPlayerById("capitalist").orElseThrow();
        capitalist.setMoney(0);
        int workerBefore = state.getVotingBag().getWorker();

        CapitalistAutomaActionCard card = card(
                304,
                List.of(
                        CapitalistAutomaActionSymbol.BUILD_ENTERPRISE,
                        CapitalistAutomaActionSymbol.SELL_ENTERPRISE,
                        CapitalistAutomaActionSymbol.RECONFIGURE_EQUIPMENT,
                        CapitalistAutomaActionSymbol.LOBBY_INTERESTS
                ),
                List.of(),
                null,
                new CapitalistAutomaSpecialAction(
                        "DRAW_AND_FILTER_CAPITALIST_VOTE_CUBES_FROM_BAG",
                        Map.of("cubes_to_draw", 6),
                        List.of(),
                        "special"
                )
        );
        AutomaSimpleModeTurnService service = serviceWithCards(List.of(card));

        var result = service.resolveAndApply(state, state.currentPlayer()).orElseThrow();
        assertThat(result.summary().getAutomaTrace().get("selectedAutomaAction")).isEqualTo("SPECIAL_ACTION");
        assertThat(result.state().getVotingBag().getWorker()).isLessThan(workerBefore);
    }

    @Test
    void uses_political_pressure_when_special_action_unavailable() {
        GameState state = prepareCapitalistSimpleTurnState();
        var capitalist = state.findPlayerById("capitalist").orElseThrow();
        capitalist.setMoney(0);
        int capitalistBefore = state.getVotingBag().getCapitalist();

        CapitalistAutomaActionCard card = card(
                305,
                List.of(
                        CapitalistAutomaActionSymbol.BUILD_ENTERPRISE,
                        CapitalistAutomaActionSymbol.SELL_ENTERPRISE,
                        CapitalistAutomaActionSymbol.RECONFIGURE_EQUIPMENT,
                        CapitalistAutomaActionSymbol.LOBBY_INTERESTS
                ),
                List.of(),
                null,
                new CapitalistAutomaSpecialAction("UNKNOWN_SPECIAL", Map.of(), List.of(), "special")
        );
        AutomaSimpleModeTurnService service = serviceWithCards(List.of(card));

        var result = service.resolveAndApply(state, state.currentPlayer()).orElseThrow();
        assertThat(result.summary().getAutomaTrace().get("selectedAutomaAction")).isEqualTo("APPLY_POLITICAL_PRESSURE");
        assertThat(result.state().getVotingBag().getCapitalist()).isEqualTo(capitalistBefore + 3);
    }

    @Test
    void start_of_turn_auto_free_actions_run_before_card_resolution() {
        GameState state = prepareCapitalistSimpleTurnState();
        var capitalistBefore = state.findPlayerById("capitalist").orElseThrow().copy();

        CapitalistAutomaActionCard card = card(
                306,
                List.of(CapitalistAutomaActionSymbol.BUILD_ENTERPRISE, CapitalistAutomaActionSymbol.PROPOSE_BILL),
                List.of(),
                null,
                null
        );
        AutomaSimpleModeTurnService service = serviceWithCards(List.of(card));

        var result = service.resolveAndApply(state, state.currentPlayer()).orElseThrow();
        var capitalistAfter = result.state().findPlayerById("capitalist").orElseThrow();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> freeActions = (List<Map<String, Object>>) result.summary().getAutomaTrace().get("freeActions");
        assertThat(freeActions).isNotEmpty();
        assertThat(freeActions.get(0).get("action")).isEqualTo("CHANGE_PRICES");
        assertThat(capitalistAfter.getPrices()).isNotEqualTo(capitalistBefore.getPrices());
    }

    @Test
    void end_turn_is_called_after_successful_automa_action() {
        GameState state = prepareCapitalistSimpleTurnState();
        int capitalistIndex = state.getTurnOrder().getActiveClasses().indexOf(ClassType.CAPITALIST);
        int beforeTurns = state.getTurnOrder().getActionsTakenByPlayer().get(capitalistIndex);

        CapitalistAutomaActionCard card = card(
                307,
                List.of(CapitalistAutomaActionSymbol.PROPOSE_BILL),
                List.of(),
                null,
                null
        );
        AutomaSimpleModeTurnService service = serviceWithCards(List.of(card));

        var result = service.resolveAndApply(state, state.currentPlayer()).orElseThrow();
        assertThat(result.state().getTurnOrder().getActionsTakenByPlayer().get(capitalistIndex)).isEqualTo(beforeTurns + 1);
        assertThat(result.state().currentPlayer().getPlayerId()).isNotEqualTo("capitalist");
    }

    @Test
    void end_turn_is_called_after_pressure_fallback() {
        GameState state = prepareCapitalistSimpleTurnState();
        var capitalist = state.findPlayerById("capitalist").orElseThrow();
        capitalist.setMoney(0);
        int capitalistIndex = state.getTurnOrder().getActiveClasses().indexOf(ClassType.CAPITALIST);
        int beforeTurns = state.getTurnOrder().getActionsTakenByPlayer().get(capitalistIndex);

        CapitalistAutomaActionCard card = card(
                308,
                List.of(
                        CapitalistAutomaActionSymbol.BUILD_ENTERPRISE,
                        CapitalistAutomaActionSymbol.SELL_ENTERPRISE,
                        CapitalistAutomaActionSymbol.RECONFIGURE_EQUIPMENT,
                        CapitalistAutomaActionSymbol.LOBBY_INTERESTS
                ),
                List.of(),
                null,
                new CapitalistAutomaSpecialAction("UNKNOWN_SPECIAL", Map.of(), List.of(), "special")
        );
        AutomaSimpleModeTurnService service = serviceWithCards(List.of(card));

        var result = service.resolveAndApply(state, state.currentPlayer()).orElseThrow();
        assertThat(result.state().getTurnOrder().getActionsTakenByPlayer().get(capitalistIndex)).isEqualTo(beforeTurns + 1);
        assertThat(result.state().currentPlayer().getPlayerId()).isNotEqualTo("capitalist");
    }

    @Test
    void build_enterprise_selects_target_by_instruction_priority() {
        GameState state = prepareCapitalistSimpleTurnState();
        state.findPlayerById("capitalist").orElseThrow().setMoney(30);
        ensureUnemployedWorkers(state, 4);

        CapitalistAutomaActionCard card = card(
                401,
                List.of(CapitalistAutomaActionSymbol.BUILD_ENTERPRISE),
                List.of(),
                null,
                null
        );
        AutomaSimpleModeTurnService service = serviceWithCards(List.of(card));

        var result = service.resolveAndApply(state, state.currentPlayer()).orElseThrow();
        @SuppressWarnings("unchecked")
        Map<String, Object> resolvedTarget = (Map<String, Object>) result.summary().getAutomaTrace().get("resolvedTarget");
        assertThat(resolvedTarget.get("enterpriseId")).isEqualTo("vegetable_farm");
    }

    @Test
    void build_enterprise_executes_state_changes() {
        GameState state = prepareCapitalistSimpleTurnState();
        state.findPlayerById("capitalist").orElseThrow().setMoney(40);
        ensureUnemployedWorkers(state, 4);
        int beforeCount = (int) state.getEnterprises().stream().filter(enterprise -> enterprise.getOwnerClass() == ClassType.CAPITALIST).count();
        int beforeMoney = state.findPlayerById("capitalist").orElseThrow().getMoney();

        CapitalistAutomaActionCard card = card(
                402,
                List.of(CapitalistAutomaActionSymbol.BUILD_ENTERPRISE),
                List.of(),
                null,
                null
        );
        AutomaSimpleModeTurnService service = serviceWithCards(List.of(card));

        var result = service.resolveAndApply(state, state.currentPlayer()).orElseThrow();
        int afterCount = (int) result.state().getEnterprises().stream().filter(enterprise -> enterprise.getOwnerClass() == ClassType.CAPITALIST).count();
        int afterMoney = result.state().findPlayerById("capitalist").orElseThrow().getMoney();

        assertThat(afterCount).isEqualTo(beforeCount + 1);
        assertThat(afterMoney).isLessThan(beforeMoney);
    }

    @Test
    void sell_enterprise_selects_legal_target_only() {
        GameState state = prepareCapitalistSimpleTurnState();
        var capitalist = state.findPlayerById("capitalist").orElseThrow();
        int beforeMoney = capitalist.getMoney();

        Enterprise sellable = new Enterprise();
        sellable.setId("automa-test-sellable");
        sellable.setName("automa-test-sellable");
        sellable.setOwnerClass(ClassType.CAPITALIST);
        sellable.setCost(40);
        sellable.setSlots(new ArrayList<>(List.of(
                new EnterpriseSlot("automa-test-sellable-slot-1", null, null, null, null)
        )));
        state.getEnterprises().add(sellable);

        CapitalistAutomaActionCard card = card(
                403,
                List.of(CapitalistAutomaActionSymbol.SELL_ENTERPRISE),
                List.of(),
                null,
                null
        );
        AutomaSimpleModeTurnService service = serviceWithCards(List.of(card));

        var result = service.resolveAndApply(state, state.currentPlayer()).orElseThrow();
        @SuppressWarnings("unchecked")
        Map<String, Object> resolvedTarget = (Map<String, Object>) result.summary().getAutomaTrace().get("resolvedTarget");
        assertThat(resolvedTarget.get("enterpriseId")).isEqualTo("automa-test-sellable");
        assertThat(result.state().findEnterprise("automa-test-sellable")).isEmpty();
        assertThat(result.state().findPlayerById("capitalist").orElseThrow().getMoney()).isGreaterThan(beforeMoney);
    }

    @Test
    void sell_to_foreign_market_executes_allowed_export_operations() {
        GameState state = prepareCapitalistSimpleTurnState();
        var capitalist = state.findPlayerById("capitalist").orElseThrow();
        capitalist.setProducedResourceAmount("food", 10);
        capitalist.setProducedResourceAmount("luxury", 10);
        int beforeMoney = capitalist.getMoney();
        int beforeOps = state.getActiveExportCard().getAvailableOperations();

        CapitalistAutomaActionCard card = card(
                404,
                List.of(CapitalistAutomaActionSymbol.SELL_ON_EXTERNAL_MARKET),
                List.of(),
                null,
                null
        );
        AutomaSimpleModeTurnService service = serviceWithCards(List.of(card));

        var result = service.resolveAndApply(state, state.currentPlayer()).orElseThrow();
        int afterMoney = result.state().findPlayerById("capitalist").orElseThrow().getMoney();
        int afterOps = result.state().getActiveExportCard().getAvailableOperations();

        assertThat(afterMoney).isGreaterThan(beforeMoney);
        assertThat(afterOps).isLessThan(beforeOps);
    }

    @Test
    void make_deal_applies_costs_and_storage_rules() {
        GameState state = prepareCapitalistSimpleTurnState();
        var capitalist = state.findPlayerById("capitalist").orElseThrow();
        capitalist.setMoney(100);
        int beforeMoney = capitalist.getMoney();
        String beforeVisibleDeal = state.getBusinessDealDeck().getVisibleCardIds().isEmpty()
                ? ""
                : state.getBusinessDealDeck().getVisibleCardIds().getFirst();

        CapitalistAutomaActionCard card = card(
                405,
                List.of(CapitalistAutomaActionSymbol.LOBBY_INTERESTS),
                List.of(),
                null,
                null
        );
        AutomaSimpleModeTurnService service = serviceWithCards(List.of(card));

        var result = service.resolveAndApply(state, state.currentPlayer()).orElseThrow();
        var afterCapitalist = result.state().findPlayerById("capitalist").orElseThrow();
        assertThat(afterCapitalist.getMoney()).isLessThan(beforeMoney);
        assertThat(afterCapitalist.getProducedResourceAmount("ftz_food")
                + afterCapitalist.getProducedResourceAmount("ftz_luxury")
                + afterCapitalist.getProducedResourceAmount("ftz_healthcare")
                + afterCapitalist.getProducedResourceAmount("ftz_education")).isGreaterThan(0);
        if (!beforeVisibleDeal.isBlank() && !result.state().getBusinessDealDeck().getVisibleCardIds().isEmpty()) {
            assertThat(result.state().getBusinessDealDeck().getVisibleCardIds().getFirst()).isNotEqualTo(beforeVisibleDeal);
        }
    }

    @Test
    void propose_bill_selects_policy_by_card_order() {
        GameState state = prepareCapitalistSimpleTurnState();

        CapitalistAutomaActionCard card = card(
                406,
                List.of(CapitalistAutomaActionSymbol.PROPOSE_BILL),
                List.of(CapitalistAutomaPolicyTag.POLICY_FOREIGN_TRADE, CapitalistAutomaPolicyTag.POLICY_TAX),
                null,
                null
        );
        AutomaSimpleModeTurnService service = serviceWithCards(List.of(card));

        var result = service.resolveAndApply(state, state.currentPlayer()).orElseThrow();
        @SuppressWarnings("unchecked")
        Map<String, Object> resolvedTarget = (Map<String, Object>) result.summary().getAutomaTrace().get("resolvedTarget");
        assertThat(resolvedTarget.get("policyId")).isEqualTo("POLICY_6_FOREIGN_TRADE");
    }

    @Test
    void simple_automa_draws_cards_from_state_deck_without_repeating_until_deck_exhausted() {
        GameState state = prepareCapitalistSimpleTurnState();
        state.findPlayerById("capitalist").orElseThrow().setInfluence(0);
        state.findPlayerById("worker").orElseThrow().setInfluence(0);

        AutomaSimpleModeTurnService service = serviceWithCards(List.of(
                card(
                        601,
                        List.of(CapitalistAutomaActionSymbol.PROPOSE_BILL),
                        List.of(CapitalistAutomaPolicyTag.POLICY_FOREIGN_TRADE),
                        null,
                        null
                ),
                card(
                        602,
                        List.of(CapitalistAutomaActionSymbol.PROPOSE_BILL),
                        List.of(CapitalistAutomaPolicyTag.POLICY_TAX),
                        null,
                        null
                )
        ));

        var first = service.resolveAndApply(state, state.currentPlayer()).orElseThrow();
        String firstDrawn = first.state().getCapitalistAutomaDeck().getVisibleCardIds().getFirst();

        GameState afterFirst = first.state();
        int capitalistIndex = afterFirst.getTurnOrder().getActiveClasses().indexOf(ClassType.CAPITALIST);
        afterFirst.getTurnOrder().setCurrentPlayerIndex(capitalistIndex);

        var second = service.resolveAndApply(afterFirst, afterFirst.currentPlayer()).orElseThrow();
        String secondDrawn = second.state().getCapitalistAutomaDeck().getVisibleCardIds().getFirst();

        assertThat(second.state().getCapitalistAutomaDeck().getRefreshCount()).isEqualTo(2);
        assertThat(second.state().getCapitalistAutomaDeck().getNextCardIndex()).isEqualTo(2);
        assertThat(secondDrawn).isNotEqualTo(firstDrawn);
    }

    @Test
    void propose_bill_triggers_snap_vote_when_conditions_met() {
        GameState state = prepareCapitalistSimpleTurnState();
        state.findPlayerById("capitalist").orElseThrow().setInfluence(10);
        state.findPlayerById("worker").orElseThrow().setInfluence(0);

        CapitalistAutomaActionCard card = card(
                407,
                List.of(CapitalistAutomaActionSymbol.PROPOSE_BILL),
                List.of(CapitalistAutomaPolicyTag.POLICY_FOREIGN_TRADE),
                null,
                null
        );
        AutomaSimpleModeTurnService service = serviceWithCards(List.of(card));

        var result = service.resolveAndApply(state, state.currentPlayer()).orElseThrow();
        @SuppressWarnings("unchecked")
        Map<String, Object> snapVote = (Map<String, Object>) result.summary().getAutomaTrace().get("snapVote");
        assertThat(snapVote.get("triggered")).isEqualTo(true);
        assertThat(result.state().getCurrentPhase()).isEqualTo(RoundPhase.ACTIONS);
        assertThat(result.state().getCurrentVoteState()).isNotNull();
        assertThat(result.state().getCurrentVoteState().isExtraordinary()).isTrue();
    }

    @Test
    void propose_bill_skips_snap_vote_when_conditions_not_met() {
        GameState state = prepareCapitalistSimpleTurnState();
        state.findPlayerById("capitalist").orElseThrow().setInfluence(1);
        state.findPlayerById("worker").orElseThrow().setInfluence(1);

        CapitalistAutomaActionCard card = card(
                408,
                List.of(CapitalistAutomaActionSymbol.PROPOSE_BILL),
                List.of(CapitalistAutomaPolicyTag.POLICY_FOREIGN_TRADE),
                null,
                null
        );
        AutomaSimpleModeTurnService service = serviceWithCards(List.of(card));

        var result = service.resolveAndApply(state, state.currentPlayer()).orElseThrow();
        @SuppressWarnings("unchecked")
        Map<String, Object> snapVote = (Map<String, Object>) result.summary().getAutomaTrace().get("snapVote");
        assertThat(snapVote.get("triggered")).isEqualTo(false);
        assertThat(result.state().getCurrentPhase()).isEqualTo(RoundPhase.ACTIONS);
    }

    private GameState prepareCapitalistSimpleTurnState() {
        GameState state = CoreTestSupport.state(2);
        var capitalist = state.findPlayerById("capitalist").orElseThrow();
        capitalist.setControlMode(PlayerControlMode.BOT);
        capitalist.setBotStrategyMode(BotStrategyMode.CARD_DRIVEN_SIMPLE_AUTOMA);
        int capitalistIndex = state.getTurnOrder().getActiveClasses().indexOf(ClassType.CAPITALIST);
        state.getTurnOrder().setCurrentPlayerIndex(Math.max(0, capitalistIndex));
        return state;
    }

    private CapitalistAutomaActionCard card(
            int no,
            List<CapitalistAutomaActionSymbol> checks,
            List<CapitalistAutomaPolicyTag> policyTags,
            CapitalistAutomaBonus bonus,
            CapitalistAutomaSpecialAction special
    ) {
        return new CapitalistAutomaActionCard(no, checks, policyTags, bonus, special, "raw", "", "test");
    }

    private AutomaSimpleModeTurnService serviceWithCards(List<CapitalistAutomaActionCard> cards) {
        return new AutomaSimpleModeTurnService(
                CoreTestSupport.engine(),
                new TestRegistry(cards, List.of()),
                new SetupSpecLoader("./missing-do-not-use.yaml")
        );
    }

    private void ensureUnemployedWorkers(GameState state, int requiredCount) {
        int current = (int) state.getWorkers().stream()
                .filter(worker -> worker.getLocation() == com.example.hegemony.domain.model.WorkerLocation.UNEMPLOYED)
                .filter(worker -> !worker.isTiedContract())
                .count();
        int toAdd = Math.max(0, requiredCount - current);
        for (int i = 0; i < toAdd; i++) {
            CoreTestSupport.addUnemployedWorker(
                    state,
                    "automa-extra-worker-" + i,
                    ClassType.WORKER,
                    WorkerQualification.UNSKILLED
            );
        }
    }

    private static final class TestRegistry implements CapitalistAutomaCardRegistry {
        private final List<CapitalistAutomaActionCard> cards;
        private final List<CapitalistAutomaInstructionCard> instructions;

        private TestRegistry(List<CapitalistAutomaActionCard> cards, List<CapitalistAutomaInstructionCard> instructions) {
            this.cards = cards;
            this.instructions = instructions;
        }

        @Override
        public boolean isLoaded() {
            return true;
        }

        @Override
        public String datasetStatus() {
            return "TEST";
        }

        @Override
        public List<CapitalistAutomaActionCard> actionCards() {
            return cards;
        }

        @Override
        public List<CapitalistAutomaInstructionCard> instructionCards() {
            if (!instructions.isEmpty()) {
                return instructions;
            }
            return List.of(
                    new CapitalistAutomaInstructionCard(
                            "sell-enterprise-priority",
                            CapitalistAutomaInstructionType.CONDITION_ACTION,
                            CapitalistAutomaActionSymbol.SELL_ENTERPRISE,
                            CapitalistAutomaInstructionMode.BOTH,
                            "",
                            List.of(Map.of(
                                    "kind", "target_selection",
                                    "applies_to", "SELL_ENTERPRISE",
                                    "choose_one_by_priority", List.of("highest_sell_value", "fewest_matching_unemployed_available", "random")
                            )),
                            List.of()
                    )
            );
        }

        @Override
        public Optional<CapitalistAutomaActionCard> findActionCard(int cardNo) {
            return cards.stream().filter(card -> card.cardNo() == cardNo).findFirst();
        }
    }
}
