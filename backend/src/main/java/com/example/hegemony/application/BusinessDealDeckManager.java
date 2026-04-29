package com.example.hegemony.application;

import com.example.hegemony.domain.carddata.BusinessDealCardCatalog;
import com.example.hegemony.domain.model.BusinessDealCard;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.OrderedCardDeckState;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.PolicyState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class BusinessDealDeckManager {
    private static final String DECK_ID = "business-deals";
    private static final int DEFAULT_VISIBLE_WINDOW = 1;

    private final BusinessDealCardCatalog catalog;

    public BusinessDealDeckManager(BusinessDealCardCatalog catalog) {
        this.catalog = catalog;
    }

    public void ensureInitialized(GameState state) {
        if (state == null) {
            return;
        }

        List<BusinessDealCard> cards = state.getBusinessDealCards();
        if (cards == null || cards.isEmpty()) {
            state.setBusinessDealCards(catalog.listAll());
        } else {
            state.setBusinessDealCards(cards.stream()
                    .sorted(Comparator.comparingInt(BusinessDealCard::getSequence))
                    .map(BusinessDealCard::copy)
                    .toList());
        }

        OrderedCardDeckState deck = state.getBusinessDealDeck();
        if (deck == null || deck.getOrderedCardIds().isEmpty()) {
            OrderedCardDeckState initialized = new OrderedCardDeckState();
            initialized.setDeckId(DECK_ID);
            initialized.setVisibleWindowSize(DEFAULT_VISIBLE_WINDOW);
            initialized.setOrderedCardIds(state.getBusinessDealCards().stream().map(BusinessDealCard::getId).toList());
            initialized.setVisibleCardIds(List.of());
            initialized.setNextCardIndex(0);
            initialized.setRefreshCount(0);
            state.setBusinessDealDeck(initialized);
            deck = initialized;
        }

        if (deck.getDeckId() == null || deck.getDeckId().isBlank()) {
            deck.setDeckId(DECK_ID);
        }
        if (deck.getVisibleWindowSize() < 0) {
            deck.setVisibleWindowSize(DEFAULT_VISIBLE_WINDOW);
        }

        if (deck.getVisibleCardIds().isEmpty() && deck.getRefreshCount() == 0) {
            deck.setVisibleWindowSize(visibleWindowForCurrentForeignTradePolicy(state));
            drawVisibleDeals(state, "INITIAL_SETUP", state.getCurrentRound());
        }
    }

    public List<BusinessDealCard> refreshVisibleDeals(GameState state, String reason, Integer round) {
        ensureInitialized(state);

        return drawVisibleDeals(state, reason, round);
    }

    public List<BusinessDealCard> refreshVisibleDealsForPreparation(GameState state, Integer round) {
        ensureInitialized(state);
        OrderedCardDeckState deck = state.getBusinessDealDeck();
        deck.setVisibleWindowSize(visibleWindowForCurrentForeignTradePolicy(state));
        state.setBusinessDealDeck(deck);
        return drawVisibleDeals(state, "ROUND_PREPARATION", round);
    }

    private List<BusinessDealCard> drawVisibleDeals(GameState state, String reason, Integer round) {
        if (state == null) {
            return List.of();
        }

        OrderedCardDeckState deck = state.getBusinessDealDeck();
        List<String> order = deck.getOrderedCardIds();
        if (order.isEmpty()) {
            deck.setVisibleCardIds(List.of());
            return List.of();
        }

        int window = Math.max(0, Math.min(deck.getVisibleWindowSize(), order.size()));
        if (window == 0) {
            deck.setVisibleCardIds(List.of());
            deck.setLastRefreshedRound(round);
            deck.setLastRefreshReason(reason);
            deck.setRefreshCount(deck.getRefreshCount() + 1);
            state.setBusinessDealDeck(deck);
            return List.of();
        }
        int startIndex = Math.floorMod(deck.getNextCardIndex(), order.size());

        List<String> visibleIds = new ArrayList<>();
        for (int i = 0; i < window; i++) {
            visibleIds.add(order.get((startIndex + i) % order.size()));
        }

        deck.setVisibleCardIds(visibleIds);
        deck.setNextCardIndex((startIndex + window) % order.size());
        deck.setRefreshCount(deck.getRefreshCount() + 1);
        deck.setLastRefreshedRound(round);
        deck.setLastRefreshReason(reason);
        state.setBusinessDealDeck(deck);

        return currentVisibleDeals(state);
    }

    private int visibleWindowForCurrentForeignTradePolicy(GameState state) {
        PolicyCourse course = state.findPolicy(PolicyId.POLICY_6_FOREIGN_TRADE)
                .map(PolicyState::getCurrentCourse)
                .orElse(PolicyCourse.B);
        return switch (course) {
            case A -> 0;
            case B -> 1;
            case C -> 2;
        };
    }

    public List<BusinessDealCard> currentVisibleDeals(GameState state) {
        if (state == null || state.getBusinessDealDeck() == null || state.getBusinessDealDeck().getVisibleCardIds().isEmpty()) {
            return List.of();
        }
        List<BusinessDealCard> cards = state.getBusinessDealCards();
        return state.getBusinessDealDeck().getVisibleCardIds().stream()
                .map(id -> cards.stream().filter(card -> id.equals(card.getId())).findFirst().orElse(null))
                .filter(card -> card != null)
                .map(BusinessDealCard::copy)
                .toList();
    }
}
