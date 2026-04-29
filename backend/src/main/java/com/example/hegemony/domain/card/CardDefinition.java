package com.example.hegemony.domain.card;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CardDefinition {
    private String id;
    private String name;
    private String description;
    private List<CardEffectDefinition> effects = new ArrayList<>();
    private String customResolver;

    public CardDefinition() {
    }

    public CardDefinition(String id, String name, String description, List<CardEffectDefinition> effects, String customResolver) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.effects = new ArrayList<>(effects);
        this.customResolver = customResolver;
    }

    public void setEffects(List<CardEffectDefinition> effects) {
        this.effects = new ArrayList<>(effects);
    }
}
