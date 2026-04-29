package com.example.hegemony.domain.model;

import java.util.ArrayList;
import java.util.List;

public class OrderedCardDeckState {
    private String deckId;
    private List<String> orderedCardIds = new ArrayList<>();
    private List<String> visibleCardIds = new ArrayList<>();
    private int visibleWindowSize;
    private int nextCardIndex;
    private int refreshCount;
    private Integer lastRefreshedRound;
    private String lastRefreshReason;

    public OrderedCardDeckState() {
    }

    public OrderedCardDeckState(
            String deckId,
            List<String> orderedCardIds,
            List<String> visibleCardIds,
            int visibleWindowSize,
            int nextCardIndex,
            int refreshCount,
            Integer lastRefreshedRound,
            String lastRefreshReason
    ) {
        this.deckId = deckId;
        this.orderedCardIds = new ArrayList<>(orderedCardIds == null ? List.of() : orderedCardIds);
        this.visibleCardIds = new ArrayList<>(visibleCardIds == null ? List.of() : visibleCardIds);
        this.visibleWindowSize = visibleWindowSize;
        this.nextCardIndex = nextCardIndex;
        this.refreshCount = refreshCount;
        this.lastRefreshedRound = lastRefreshedRound;
        this.lastRefreshReason = lastRefreshReason;
    }

    public OrderedCardDeckState copy() {
        return new OrderedCardDeckState(
                deckId,
                orderedCardIds,
                visibleCardIds,
                visibleWindowSize,
                nextCardIndex,
                refreshCount,
                lastRefreshedRound,
                lastRefreshReason
        );
    }

    public String getDeckId() {
        return deckId;
    }

    public void setDeckId(String deckId) {
        this.deckId = deckId;
    }

    public List<String> getOrderedCardIds() {
        return orderedCardIds;
    }

    public void setOrderedCardIds(List<String> orderedCardIds) {
        this.orderedCardIds = orderedCardIds == null ? new ArrayList<>() : new ArrayList<>(orderedCardIds);
    }

    public List<String> getVisibleCardIds() {
        return visibleCardIds;
    }

    public void setVisibleCardIds(List<String> visibleCardIds) {
        this.visibleCardIds = visibleCardIds == null ? new ArrayList<>() : new ArrayList<>(visibleCardIds);
    }

    public int getVisibleWindowSize() {
        return visibleWindowSize;
    }

    public void setVisibleWindowSize(int visibleWindowSize) {
        this.visibleWindowSize = visibleWindowSize;
    }

    public int getNextCardIndex() {
        return nextCardIndex;
    }

    public void setNextCardIndex(int nextCardIndex) {
        this.nextCardIndex = nextCardIndex;
    }

    public int getRefreshCount() {
        return refreshCount;
    }

    public void setRefreshCount(int refreshCount) {
        this.refreshCount = refreshCount;
    }

    public Integer getLastRefreshedRound() {
        return lastRefreshedRound;
    }

    public void setLastRefreshedRound(Integer lastRefreshedRound) {
        this.lastRefreshedRound = lastRefreshedRound;
    }

    public String getLastRefreshReason() {
        return lastRefreshReason;
    }

    public void setLastRefreshReason(String lastRefreshReason) {
        this.lastRefreshReason = lastRefreshReason;
    }
}
