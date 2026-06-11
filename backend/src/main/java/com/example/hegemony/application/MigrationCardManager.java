package com.example.hegemony.application;

import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.MigrationCardEntry;
import com.example.hegemony.domain.model.MigrationCardState;
import com.example.hegemony.domain.model.OrderedCardDeckState;
import com.example.hegemony.domain.model.PlayerState;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.PolicyState;
import com.example.hegemony.domain.model.PopulationScale;
import com.example.hegemony.domain.model.Worker;
import com.example.hegemony.domain.model.WorkerLocation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class MigrationCardManager {
    private static final String DECK_ID = "migration-cards";

    private final List<MigrationCardState> catalog;

    public MigrationCardManager(ObjectMapper objectMapper) {
        this.catalog = loadCatalog(objectMapper);
    }

    public void ensureInitialized(GameState state) {
        if (state == null) {
            return;
        }

        List<MigrationCardState> cards = state.getMigrationCards();
        if (cards == null || cards.isEmpty()) {
            state.setMigrationCards(catalog);
        } else {
            state.setMigrationCards(cards.stream()
                    .sorted(Comparator.comparingInt(MigrationCardState::getSequence))
                    .map(MigrationCardState::copy)
                    .toList());
        }

        OrderedCardDeckState deck = state.getMigrationDeck();
        if (deck == null || deck.getOrderedCardIds().isEmpty()) {
            OrderedCardDeckState initialized = new OrderedCardDeckState();
            initialized.setDeckId(DECK_ID);
            initialized.setVisibleWindowSize(0);
            initialized.setOrderedCardIds(shuffledCardIds(state.getMigrationCards().stream().map(MigrationCardState::getCardId).toList()));
            initialized.setVisibleCardIds(List.of());
            initialized.setNextCardIndex(0);
            initialized.setRefreshCount(0);
            state.setMigrationDeck(initialized);
            deck = initialized;
        }

        if (deck.getDeckId() == null || deck.getDeckId().isBlank()) {
            deck.setDeckId(DECK_ID);
        }
    }

    public MigrationResolution resolveForPreparation(GameState state) {
        ensureInitialized(state);
        OrderedCardDeckState deck = state.getMigrationDeck();
        deck.setVisibleCardIds(List.of());
        deck.setLastRefreshedRound(state.getCurrentRound());
        deck.setLastRefreshReason("ROUND_PREPARATION");

        int cardsPerEligibleClass = cardsPerEligibleClass(state);
        if (cardsPerEligibleClass <= 0) {
            deck.setRefreshCount(deck.getRefreshCount() + 1);
            state.setMigrationDeck(deck);
            return new MigrationResolution(Map.of(), Map.of(), 0);
        }

        Map<ClassType, List<String>> drawnCardIdsByClass = new EnumMap<>(ClassType.class);
        Map<ClassType, List<String>> arrivalsByClass = new EnumMap<>(ClassType.class);
        int totalAddedPopulation = 0;

        for (ClassType classType : List.of(ClassType.WORKER, ClassType.MIDDLE_CLASS)) {
            PlayerState player = state.getPlayers().stream()
                    .filter(candidate -> candidate.getClassType() == classType)
                    .findFirst()
                    .orElse(null);
            if (player == null) {
                continue;
            }

            List<String> drawnCardIds = new ArrayList<>();
            List<String> arrivals = new ArrayList<>();
            for (int i = 0; i < cardsPerEligibleClass; i++) {
                MigrationCardState card = drawNextCard(state);
                if (card.getCardId() == null || card.getCardId().isBlank()) {
                    continue;
                }
                drawnCardIds.add(card.getCardId());
                MigrationCardEntry entry = classType == ClassType.WORKER ? card.getWorkerEntry() : card.getMiddleClassEntry();
                Worker worker = createMigratedWorker(state, classType, entry);
                state.getWorkers().add(worker);
                player.setPopulation(populationForClass(state, classType));
                totalAddedPopulation++;
                arrivals.add(describeEntry(entry));
            }
            if (!drawnCardIds.isEmpty()) {
                drawnCardIdsByClass.put(classType, drawnCardIds);
                arrivalsByClass.put(classType, arrivals);
            }
        }

        state.refreshLegacyPlayerSnapshots();
        deck.setRefreshCount(deck.getRefreshCount() + 1);
        state.setMigrationDeck(deck);
        return new MigrationResolution(drawnCardIdsByClass, arrivalsByClass, totalAddedPopulation);
    }

    private MigrationCardState drawNextCard(GameState state) {
        OrderedCardDeckState deck = state.getMigrationDeck();
        List<String> order = deck.getOrderedCardIds();
        if (order.isEmpty()) {
            return new MigrationCardState();
        }

        int startIndex = Math.floorMod(deck.getNextCardIndex(), order.size());
        String cardId = order.get(startIndex);
        deck.setNextCardIndex((startIndex + 1) % order.size());
        List<String> visible = new ArrayList<>(deck.getVisibleCardIds() == null ? List.of() : deck.getVisibleCardIds());
        visible.add(cardId);
        deck.setVisibleCardIds(visible);
        state.setMigrationDeck(deck);

        return state.getMigrationCards().stream()
                .filter(card -> cardId.equals(card.getCardId()))
                .findFirst()
                .map(MigrationCardState::copy)
                .orElseGet(MigrationCardState::new);
    }

    private int cardsPerEligibleClass(GameState state) {
        if (state.getCurrentRound() <= 1) {
            return 0;
        }
        PolicyCourse course = state.findPolicy(PolicyId.POLICY_7_IMMIGRATION)
                .map(PolicyState::getCurrentCourse)
                .orElse(PolicyCourse.B);
        return switch (course) {
            case A -> 0;
            case B -> 1;
            case C -> 2;
        };
    }

    private Worker createMigratedWorker(GameState state, ClassType classType, MigrationCardEntry entry) {
        Worker worker = new Worker();
        worker.setId(nextWorkerId(state, classType));
        worker.setClassType(classType);
        worker.setQualificationType(entry.getQualificationType());
        worker.setSector(entry.getSector());
        worker.setLocation(WorkerLocation.UNEMPLOYED);
        worker.setTiedContract(false);
        worker.setEnterpriseId(null);
        worker.setSlotId(null);
        return worker;
    }

    private int populationForClass(GameState state, ClassType classType) {
        int workerCount = (int) state.getWorkers().stream()
                .filter(worker -> worker.getClassType() == classType)
                .count();
        return PopulationScale.fromWorkerCount(workerCount);
    }

    private String nextWorkerId(GameState state, ClassType classType) {
        String prefix = classType.playerId() + "-worker-";
        int nextIndex = 1;
        for (Worker existing : state.getWorkers()) {
            String id = existing.getId();
            if (id == null || !id.startsWith(prefix)) {
                continue;
            }
            String suffix = id.substring(prefix.length());
            try {
                nextIndex = Math.max(nextIndex, Integer.parseInt(suffix) + 1);
            } catch (NumberFormatException ignored) {
                // Ignore non-standard ids and keep searching.
            }
        }
        return prefix + nextIndex;
    }

    private String describeEntry(MigrationCardEntry entry) {
        if (entry == null || entry.getQualificationType() == null) {
            return "UNKNOWN";
        }
        if (entry.getQualificationType().name().equals("UNSKILLED")) {
            return "UNSKILLED/GRAY";
        }
        return "SKILLED/" + (entry.getSector() == null ? "GENERAL" : entry.getSector().name());
    }

    private List<String> shuffledCardIds(List<String> cardIds) {
        List<String> shuffled = new ArrayList<>(cardIds == null ? List.of() : cardIds);
        Collections.shuffle(shuffled);
        return shuffled;
    }

    private List<MigrationCardState> loadCatalog(ObjectMapper objectMapper) {
        try {
            Resource resource = new ClassPathResource("cards/migration-cards.json");
            try (InputStream stream = resource.getInputStream()) {
                MigrationCardCatalogDocument document = objectMapper.readValue(stream, MigrationCardCatalogDocument.class);
                return document.cards().stream()
                        .sorted(Comparator.comparingInt(MigrationCardState::getSequence))
                        .map(MigrationCardState::copy)
                        .toList();
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load cards/migration-cards.json", ex);
        }
    }

    public record MigrationResolution(
            Map<ClassType, List<String>> drawnCardIdsByClass,
            Map<ClassType, List<String>> arrivalsByClass,
            int totalAddedPopulation
    ) {
    }

    private record MigrationCardCatalogDocument(List<MigrationCardState> cards) {
        private MigrationCardCatalogDocument {
            cards = cards == null ? new ArrayList<>() : cards;
        }
    }
}
