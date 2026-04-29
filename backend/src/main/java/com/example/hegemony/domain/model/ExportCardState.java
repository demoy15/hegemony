package com.example.hegemony.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ExportCardState {
    @Setter
    private String cardId;
    @Setter
    private String title;
    @Setter
    private String description;
    @Setter
    private int availableOperations;
    @Setter
    private int activatedRound;
    @Setter
    private boolean placeholder;
    @Setter
    private int sequence;
    private List<ExportCardOffer> offers = new ArrayList<>();
    private String sourceImageRef;

    public ExportCardState() {
    }

    public ExportCardState(
            String cardId,
            String title,
            String description,
            int availableOperations,
            int activatedRound,
            boolean placeholder,
            int sequence,
            List<ExportCardOffer> offers,
            String sourceImageRef
    ) {
        this.cardId = cardId;
        this.title = title;
        this.description = description;
        this.availableOperations = availableOperations;
        this.activatedRound = activatedRound;
        this.placeholder = placeholder;
        this.sequence = sequence;
        setOffers(offers);
        this.sourceImageRef = sourceImageRef;
    }

    public ExportCardState copy() {
        return new ExportCardState(
                cardId,
                title,
                description,
                availableOperations,
                activatedRound,
                placeholder,
                sequence,
                offers.stream().map(ExportCardOffer::copy).toList(),
                sourceImageRef
        );
    }

    public void setOffers(List<ExportCardOffer> offers) {
        this.offers = offers == null ? new ArrayList<>() : offers.stream().map(ExportCardOffer::copy).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public void setSourceImageRef(String sourceImageRef) {
        this.sourceImageRef = sourceImageRef;
    }
}
