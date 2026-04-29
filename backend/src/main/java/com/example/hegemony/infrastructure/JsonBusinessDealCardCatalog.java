package com.example.hegemony.infrastructure;

import com.example.hegemony.domain.carddata.BusinessDealCardCatalog;
import com.example.hegemony.domain.model.BusinessDealCard;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JsonBusinessDealCardCatalog implements BusinessDealCardCatalog {
    private final Map<String, BusinessDealCard> cardsById;

    public JsonBusinessDealCardCatalog(ObjectMapper objectMapper) {
        this.cardsById = loadCards(objectMapper);
    }

    @Override
    public List<BusinessDealCard> listAll() {
        return cardsById.values().stream()
                .sorted(Comparator.comparingInt(BusinessDealCard::getSequence))
                .map(BusinessDealCard::copy)
                .toList();
    }

    @Override
    public Optional<BusinessDealCard> findById(String cardId) {
        BusinessDealCard card = cardsById.get(cardId);
        return card == null ? Optional.empty() : Optional.of(card.copy());
    }

    private Map<String, BusinessDealCard> loadCards(ObjectMapper objectMapper) {
        try {
            Resource resource = new ClassPathResource("cards/business-deals.json");
            try (InputStream stream = resource.getInputStream()) {
                BusinessDealCatalogDocument document = objectMapper.readValue(stream, BusinessDealCatalogDocument.class);
                Map<String, BusinessDealCard> result = new ConcurrentHashMap<>();
                for (BusinessDealCard card : document.cards()) {
                    result.put(card.getId(), card);
                }
                return result;
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load cards/business-deals.json", ex);
        }
    }

    private record BusinessDealCatalogDocument(List<BusinessDealCard> cards) {
        private BusinessDealCatalogDocument {
            cards = cards == null ? new ArrayList<>() : cards;
        }
    }
}
