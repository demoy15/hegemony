package com.example.hegemony.application;

import com.example.hegemony.domain.model.ExportCardState;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.OrderedCardDeckState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class ExportCardManager {
    private static final String DECK_ID = "export-cards";
    private static final int VISIBLE_WINDOW = 1;

    private final List<ExportCardState> catalog;

    public ExportCardManager(ObjectMapper objectMapper) {
        this.catalog = loadCatalog(objectMapper);
    }

    public void ensureInitialized(GameState state) {
        if (state == null) {
            return;
        }

        List<ExportCardState> cards = state.getExportCards();
        if (cards == null || cards.isEmpty()) {
            state.setExportCards(catalog);
        } else {
            state.setExportCards(cards.stream()
                    .sorted(Comparator.comparingInt(ExportCardState::getSequence))
                    .map(ExportCardState::copy)
                    .toList());
        }

        OrderedCardDeckState deck = state.getExportCardDeck();
        if (deck == null || deck.getOrderedCardIds().isEmpty()) {
            OrderedCardDeckState initialized = new OrderedCardDeckState();
            initialized.setDeckId(DECK_ID);
            initialized.setVisibleWindowSize(VISIBLE_WINDOW);
            initialized.setOrderedCardIds(state.getExportCards().stream().map(ExportCardState::getCardId).toList());
            initialized.setVisibleCardIds(List.of());
            initialized.setNextCardIndex(0);
            initialized.setRefreshCount(0);
            state.setExportCardDeck(initialized);
            deck = initialized;
        }

        if (deck.getDeckId() == null || deck.getDeckId().isBlank()) {
            deck.setDeckId(DECK_ID);
        }
        if (deck.getVisibleWindowSize() <= 0) {
            deck.setVisibleWindowSize(VISIBLE_WINDOW);
        }

        if ((state.getActiveExportCard() == null || state.getActiveExportCard().getCardId() == null || state.getActiveExportCard().getCardId().isBlank())
                && deck.getRefreshCount() == 0) {
            drawNextCard(state, state.getCurrentRound(), "INITIAL_SETUP");
        }
    }

    public ExportCardState refreshForRound(GameState state, int round) {
        ensureInitialized(state);
        return drawNextCard(state, round, "ROUND_PREPARATION");
    }

    private ExportCardState drawNextCard(GameState state, int round, String reason) {
        OrderedCardDeckState deck = state.getExportCardDeck();
        List<String> order = deck.getOrderedCardIds();
        if (order.isEmpty()) {
            state.setActiveExportCard(new ExportCardState());
            deck.setVisibleCardIds(List.of());
            state.setExportCardDeck(deck);
            return new ExportCardState();
        }

        int startIndex = Math.floorMod(deck.getNextCardIndex(), order.size());
        String cardId = order.get(startIndex);
        ExportCardState active = state.getExportCards().stream()
                .filter(card -> cardId.equals(card.getCardId()))
                .findFirst()
                .map(ExportCardState::copy)
                .orElseGet(ExportCardState::new);
        active.setActivatedRound(Math.max(1, round));
        active.setPlaceholder(false);

        deck.setVisibleCardIds(List.of(cardId));
        deck.setNextCardIndex((startIndex + 1) % order.size());
        deck.setRefreshCount(deck.getRefreshCount() + 1);
        deck.setLastRefreshedRound(round);
        deck.setLastRefreshReason(reason);

        state.setExportCardDeck(deck);
        state.setActiveExportCard(active);
        return active.copy();
    }

    private List<ExportCardState> loadCatalog(ObjectMapper objectMapper) {
        try {
            Resource resource = new ClassPathResource("cards/export-cards.json");
            try (InputStream stream = resource.getInputStream()) {
                ExportCardCatalogDocument document = objectMapper.readValue(stream, ExportCardCatalogDocument.class);
                return document.cards().stream()
                        .sorted(Comparator.comparingInt(ExportCardState::getSequence))
                        .map(ExportCardState::copy)
                        .toList();
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load cards/export-cards.json", ex);
        }
    }

    private record ExportCardCatalogDocument(List<ExportCardState> cards) {
        private ExportCardCatalogDocument {
            cards = cards == null ? new ArrayList<>() : cards;
        }
    }
}
