package com.example.hegemony.domain.model;

public class MigrationCardState {
    private String cardId;
    private int sequence;
    private MigrationCardEntry workerEntry = new MigrationCardEntry();
    private MigrationCardEntry middleClassEntry = new MigrationCardEntry();

    public MigrationCardState() {
    }

    public MigrationCardState(
            String cardId,
            int sequence,
            MigrationCardEntry workerEntry,
            MigrationCardEntry middleClassEntry
    ) {
        this.cardId = cardId;
        this.sequence = sequence;
        this.workerEntry = workerEntry == null ? new MigrationCardEntry() : workerEntry.copy();
        this.middleClassEntry = middleClassEntry == null ? new MigrationCardEntry() : middleClassEntry.copy();
    }

    public MigrationCardState copy() {
        return new MigrationCardState(
                cardId,
                sequence,
                workerEntry == null ? new MigrationCardEntry() : workerEntry.copy(),
                middleClassEntry == null ? new MigrationCardEntry() : middleClassEntry.copy()
        );
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public MigrationCardEntry getWorkerEntry() {
        return workerEntry;
    }

    public void setWorkerEntry(MigrationCardEntry workerEntry) {
        this.workerEntry = workerEntry == null ? new MigrationCardEntry() : workerEntry;
    }

    public MigrationCardEntry getMiddleClassEntry() {
        return middleClassEntry;
    }

    public void setMiddleClassEntry(MigrationCardEntry middleClassEntry) {
        this.middleClassEntry = middleClassEntry == null ? new MigrationCardEntry() : middleClassEntry;
    }
}
