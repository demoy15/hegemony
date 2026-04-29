package com.example.hegemony.domain;

import com.example.hegemony.domain.command.ResolvePreparationPhaseCommand;
import com.example.hegemony.domain.engine.ApplyCommandResult;
import com.example.hegemony.domain.engine.GameRulesEngine;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.RoundPhase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessDealDeckLifecycleTest {
    @Test
    void initialStateShowsFirstVisibleDeals() {
        GameState state = CoreTestSupport.state(4);

        assertThat(state.getBusinessDealCards()).hasSize(10);
        assertThat(state.getBusinessDealDeck().getVisibleCardIds())
                .containsExactly("business-deal-01");
        assertThat(state.getExportCards()).hasSize(12);
        assertThat(state.getActiveExportCard().getCardId()).isEqualTo("export-card-01");
        assertThat(state.getActiveExportCard().getOffers()).hasSize(8);
    }

    @Test
    void manualRefreshIsRejectedOutsidePreparationFlow() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);

        ApplyCommandResult refreshed = engine.apply(state, new com.example.hegemony.domain.command.RefreshBusinessDealsCommand("worker"));

        assertThat(refreshed.validation().isValid()).isFalse();
    }

    @Test
    void preparationRefreshesDealsForNewRound() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);
        state.setCurrentRound(2);
        state.getTurnOrder().setRound(2);
        state.setRoundMarker(2);
        state.setCurrentPhase(RoundPhase.PREPARATION);
        state.getTurnOrder().setPhase(RoundPhase.PREPARATION);
        state.getTurnOrder().setCurrentPlayerIndex(0);

        ApplyCommandResult refreshed = engine.apply(state, new ResolvePreparationPhaseCommand("worker"));

        assertThat(refreshed.validation().isValid()).isTrue();
        assertThat(refreshed.resultingState().getBusinessDealDeck().getVisibleCardIds())
                .containsExactly("business-deal-02");
        assertThat(refreshed.resultingState().getLastPreparationSummary().getExecutedSubsteps())
                .contains("business_deals_refreshed", "export_card_refreshed");
        assertThat(refreshed.resultingState().getActiveExportCard().getActivatedRound()).isEqualTo(2);
        assertThat(refreshed.resultingState().getActiveExportCard().getCardId()).isEqualTo("export-card-02");
    }

    @Test
    void foreignTradePolicyControlsVisibleDealCountOnlyAtPreparation() {
        GameRulesEngine engine = CoreTestSupport.engine();
        GameState state = CoreTestSupport.state(4);
        state.findPolicy(PolicyId.POLICY_6_FOREIGN_TRADE).orElseThrow().setCurrentCourse(PolicyCourse.C);
        state.setCurrentRound(2);
        state.getTurnOrder().setRound(2);
        state.setRoundMarker(2);
        state.setCurrentPhase(RoundPhase.PREPARATION);
        state.getTurnOrder().setPhase(RoundPhase.PREPARATION);

        ApplyCommandResult refreshed = engine.apply(state, new ResolvePreparationPhaseCommand("worker"));

        assertThat(refreshed.validation().isValid()).isTrue();
        assertThat(refreshed.resultingState().getBusinessDealDeck().getVisibleCardIds())
                .containsExactly("business-deal-02", "business-deal-03");
    }
}
