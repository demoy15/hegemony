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
                .hasSize(1);
        assertThat(state.getBusinessDealCards())
                .extracting(card -> card.getId())
                .contains(state.getBusinessDealDeck().getVisibleCardIds().getFirst());
        assertThat(state.getExportCards()).hasSize(16);
        assertThat(state.getExportCards())
                .extracting(card -> card.getCardId())
                .contains(state.getActiveExportCard().getCardId(), "export-card-16");
        assertThat(state.getExportCards().stream()
                .filter(card -> "export-card-16".equals(card.getCardId()))
                .findFirst()
                .orElseThrow()
                .getOffers())
                .anySatisfy(offer -> {
                    assertThat(offer.getResourceId()).isEqualTo("food");
                    assertThat(offer.getQuantity()).isEqualTo(4);
                    assertThat(offer.getRevenue()).isEqualTo(40);
                })
                .anySatisfy(offer -> {
                    assertThat(offer.getResourceId()).isEqualTo("healthcare");
                    assertThat(offer.getQuantity()).isEqualTo(7);
                    assertThat(offer.getRevenue()).isEqualTo(50);
                });
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
        String initialVisibleDeal = state.getBusinessDealDeck().getVisibleCardIds().getFirst();
        String initialExportCard = state.getActiveExportCard().getCardId();

        ApplyCommandResult refreshed = engine.apply(state, new ResolvePreparationPhaseCommand("worker"));

        assertThat(refreshed.validation().isValid()).isTrue();
        assertThat(refreshed.resultingState().getBusinessDealDeck().getVisibleCardIds())
                .hasSize(1)
                .doesNotContain(initialVisibleDeal);
        assertThat(refreshed.resultingState().getLastPreparationSummary().getExecutedSubsteps())
                .contains("business_deals_refreshed", "export_card_refreshed");
        assertThat(refreshed.resultingState().getActiveExportCard().getActivatedRound()).isEqualTo(2);
        assertThat(refreshed.resultingState().getActiveExportCard().getCardId()).isNotEqualTo(initialExportCard);
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
                .hasSize(2);
    }
}
