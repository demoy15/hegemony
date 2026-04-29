package com.example.hegemony.domain.card;

import java.util.List;
import java.util.Optional;

public interface CardCatalog {
    Optional<CardDefinition> findById(String cardId);

    List<CardDefinition> listAll();
}
