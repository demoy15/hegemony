package com.example.hegemony.infrastructure;

import com.example.hegemony.domain.card.CardCatalog;
import com.example.hegemony.domain.card.CardDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JsonCardCatalog implements CardCatalog {
    private final Map<String, CardDefinition> cardsById;

    public JsonCardCatalog(ObjectMapper objectMapper) {
        this.cardsById = loadCards(objectMapper);
    }

    @Override
    public Optional<CardDefinition> findById(String cardId) {
        return Optional.ofNullable(cardsById.get(cardId));
    }

    @Override
    public List<CardDefinition> listAll() {
        return new ArrayList<>(cardsById.values());
    }

    private Map<String, CardDefinition> loadCards(ObjectMapper objectMapper) {
        try {
            Resource resource = new org.springframework.core.io.ClassPathResource("cards/sample-cards.json");
            try (InputStream stream = resource.getInputStream()) {
                CardCatalogDocument document = objectMapper.readValue(stream, CardCatalogDocument.class);
                Map<String, CardDefinition> result = new ConcurrentHashMap<>();
                for (CardDefinition card : document.cards()) {
                    result.put(card.getId(), card);
                }
                return result;
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load cards/sample-cards.json", ex);
        }
    }

    private record CardCatalogDocument(List<CardDefinition> cards) {
    }
}
