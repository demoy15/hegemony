package com.example.hegemony.domain;

import com.example.hegemony.bot.LegalMoveBot;
import com.example.hegemony.domain.command.AdvanceToNextRoundCommand;
import com.example.hegemony.domain.command.AdvanceToScoringCommand;
import com.example.hegemony.domain.command.BuyGoodsAndServicesCommand;
import com.example.hegemony.domain.command.CommitVoteInfluenceCommand;
import com.example.hegemony.domain.command.ConsumeHealthcareCommand;
import com.example.hegemony.domain.command.ConsumeLuxuryCommand;
import com.example.hegemony.domain.command.DeclareVoteStanceCommand;
import com.example.hegemony.domain.command.ProposeBillCommand;
import com.example.hegemony.domain.command.PurchaseItem;
import com.example.hegemony.domain.command.ResolvePreparationPhaseCommand;
import com.example.hegemony.domain.command.ResolveProductionPhaseCommand;
import com.example.hegemony.domain.command.ResolveScoringPhaseCommand;
import com.example.hegemony.domain.engine.ApplyCommandResult;
import com.example.hegemony.domain.engine.GameRulesEngine;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.SupplierType;
import com.example.hegemony.domain.model.VoteStance;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConsumerEconomyScenarioTest {
    @Test
    void produceThenBuyThenConsumeThenWelfareChanges() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateInProductionPhase(4);

        int capitalistFoodBefore = state.findPlayerById("capitalist").orElseThrow().getProducedResourceAmount("food");
        state = apply(engine, state, new ResolveProductionPhaseCommand("worker"));
        int capitalistFoodAfterProduction = state.findPlayerById("capitalist").orElseThrow().getProducedResourceAmount("food");
        assertThat(capitalistFoodAfterProduction).isLessThanOrEqualTo(8);
        assertThat(state.getProductionPhaseState().getWorkerFoodConsumed())
                .isEqualTo(state.findPlayerById("worker").orElseThrow().getPopulation());

        state = apply(engine, state, new com.example.hegemony.domain.command.AdvanceToVotingCommand("worker"));
        state = apply(engine, state, new ResolveScoringPhaseCommand("worker"));
        state = apply(engine, state, new AdvanceToNextRoundCommand("worker"));
        state = apply(engine, state, new ResolvePreparationPhaseCommand("worker"));
        state.findPlayerById("worker").orElseThrow().setPopulation(1);

        int welfareBefore = state.findPlayerById("worker").orElseThrow().getWelfare();
        state = apply(engine, state, new BuyGoodsAndServicesCommand(
                "worker",
                "HEALTHCARE",
                List.of(new PurchaseItem(SupplierType.STATE, null, state.findPlayerById("worker").orElseThrow().getPopulation()))
        ));
        state = apply(engine, state, new ConsumeHealthcareCommand("worker"));

        assertThat(state.findPlayerById("worker").orElseThrow().getWelfare()).isEqualTo(welfareBefore + 1);
    }

    @Test
    void productionAppliesStateAndCapitalistStorageLimits() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.stateInProductionPhase(2);
        state.findPlayerById("worker").orElseThrow().setPopulation(0);
        state.findPlayerById("capitalist").orElseThrow().setProducedResourceAmount("food", 7);

        int stateServicesBefore = totalStateServices(state);
        state = apply(engine, state, new ResolveProductionPhaseCommand("worker"));

        assertThat(totalStateServices(state) - stateServicesBefore).isEqualTo(6);
        assertThat(state.findPlayerById("capitalist").orElseThrow().getProducedResourceAmount("food")).isEqualTo(8);
    }

    @Test
    void proposeBillChangesPolicyThenServicePriceUsesNewPolicy() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);

        state = apply(engine, state, new ProposeBillCommand("worker", PolicyId.POLICY_4_HEALTHCARE_AND_BENEFITS, PolicyCourse.A));
        state = CoreTestSupport.exhaustActionPhase(state, engine);
        state = apply(engine, state, new ResolveProductionPhaseCommand("worker"));
        state = apply(engine, state, new com.example.hegemony.domain.command.AdvanceToVotingCommand("worker"));
        state = apply(engine, state, new DeclareVoteStanceCommand("middle_class", PolicyId.POLICY_4_HEALTHCARE_AND_BENEFITS, VoteStance.FOR.name()));
        state = apply(engine, state, new DeclareVoteStanceCommand("capitalist", PolicyId.POLICY_4_HEALTHCARE_AND_BENEFITS, VoteStance.FOR.name()));
        state = apply(engine, state, new DeclareVoteStanceCommand("state", PolicyId.POLICY_4_HEALTHCARE_AND_BENEFITS, VoteStance.FOR.name()));
        state = apply(engine, state, new CommitVoteInfluenceCommand("worker", 0));
        state = apply(engine, state, new CommitVoteInfluenceCommand("middle_class", 0));
        state = apply(engine, state, new CommitVoteInfluenceCommand("capitalist", 0));
        state = apply(engine, state, new CommitVoteInfluenceCommand("state", 0));
        state = apply(engine, state, new ResolveScoringPhaseCommand("worker"));
        state = apply(engine, state, new AdvanceToNextRoundCommand("worker"));
        state = apply(engine, state, new ResolvePreparationPhaseCommand("worker"));
        state.findPlayerById("worker").orElseThrow().setPopulation(1);

        int moneyBeforeBuy = state.findPlayerById("worker").orElseThrow().getMoney();
        state = apply(engine, state, new BuyGoodsAndServicesCommand(
                "worker",
                "HEALTHCARE",
                List.of(new PurchaseItem(SupplierType.STATE, null, 1))
        ));

        assertThat(state.findPolicy(PolicyId.POLICY_4_HEALTHCARE_AND_BENEFITS).orElseThrow().getCurrentCourse()).isEqualTo(PolicyCourse.A);
        assertThat(moneyBeforeBuy - state.findPlayerById("worker").orElseThrow().getMoney()).isEqualTo(0);
    }

    @Test
    void saveLoadPreservesResourcesPricesAndWelfare() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);
        state.findPlayerById("worker").orElseThrow().setGoodsAmount("luxury", state.findPlayerById("worker").orElseThrow().getPopulation());
        state = apply(engine, state, new ConsumeLuxuryCommand("worker"));

        String json = mapper.writeValueAsString(state);
        GameState loaded = mapper.readValue(json, GameState.class);

        assertThat(loaded.findPlayerById("worker").orElseThrow().getGoodsAmount("luxury"))
                .isEqualTo(state.findPlayerById("worker").orElseThrow().getGoodsAmount("luxury"));
        assertThat(loaded.findPlayerById("middle_class").orElseThrow().getPrices())
                .isEqualTo(state.findPlayerById("middle_class").orElseThrow().getPrices());
        assertThat(loaded.findPlayerById("worker").orElseThrow().getWelfare())
                .isEqualTo(state.findPlayerById("worker").orElseThrow().getWelfare());
    }

    @Test
    void botUsesOnlyLegalConsumerEconomyMoves() {
        GameRulesEngine engine = CoreTestSupport.engine();
        LegalMoveBot bot = new LegalMoveBot();
        GameState state = CoreTestSupport.state(4);
        state.findPlayerById("worker").orElseThrow().setGoodsAmount("healthcare", state.findPlayerById("worker").orElseThrow().getPopulation());

        var decision = bot.chooseMove(state, engine);
        var legal = engine.generateLegalCommands(state).stream().map(cmd -> cmd.moveId()).toList();

        assertThat(legal).contains(decision.selectedCommand().moveId());
        assertThat(engine.validate(state, decision.selectedCommand()).isValid()).isTrue();
    }

    private GameState apply(GameRulesEngine engine, GameState state, com.example.hegemony.domain.command.GameCommand command) {
        ApplyCommandResult result = engine.apply(state, command);
        assertThat(result.validation().isValid()).as(result.validation().getErrors().toString()).isTrue();
        return result.resultingState();
    }

    private int totalStateServices(GameState state) {
        return state.getPublicServiceAmount("healthcare")
                + state.getPublicServiceAmount("education")
                + state.getPublicServiceAmount("media_influence");
    }
}
