package com.example.hegemony.infrastructure.carddata;

import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaActionCard;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaCardRegistry;
import com.example.hegemony.domain.carddata.BotActionCardCatalog;
import com.example.hegemony.domain.carddata.BotActionCardDefinition;
import com.example.hegemony.domain.model.ClassType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class YamlBotActionCardCatalog implements BotActionCardCatalog {
    private final CapitalistAutomaCardRegistry capitalistRegistry;

    public YamlBotActionCardCatalog(CapitalistAutomaCardRegistry capitalistRegistry) {
        this.capitalistRegistry = capitalistRegistry;
    }

    @Override
    public Optional<BotActionCardDefinition> findById(String botCardId) {
        if (botCardId == null || botCardId.isBlank()) {
            return Optional.empty();
        }
        String normalized = botCardId.trim().toLowerCase();
        if (!normalized.startsWith("capitalist-")) {
            return Optional.empty();
        }
        String numericPart = normalized.substring("capitalist-".length());
        try {
            int cardNo = Integer.parseInt(numericPart);
            return capitalistRegistry.findActionCard(cardNo).map(this::toDefinition);
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    @Override
    public List<BotActionCardDefinition> listForClass(ClassType classType) {
        if (classType != ClassType.CAPITALIST) {
            return List.of();
        }
        return capitalistRegistry.actionCards().stream().map(this::toDefinition).toList();
    }

    @Override
    public boolean isSimpleAutomaDatasetInstalled(ClassType classType) {
        return classType == ClassType.CAPITALIST && capitalistRegistry.isLoaded();
    }

    @Override
    public String datasetStatus(ClassType classType) {
        if (classType != ClassType.CAPITALIST) {
            return "CARD_DATA_NOT_INSTALLED";
        }
        return capitalistRegistry.datasetStatus();
    }

    private BotActionCardDefinition toDefinition(CapitalistAutomaActionCard card) {
        return new BotActionCardDefinition(
                "capitalist-" + card.cardNo(),
                ClassType.CAPITALIST,
                card.checks().stream().map(Enum::name).toList(),
                card.bonus() == null ? "" : card.bonus().rawTextRu(),
                card.policyTags().stream().map(Enum::name).toList(),
                card.specialAction() == null ? "" : card.specialAction().type(),
                0,
                "PARTIAL_DATASET_INTERPRETABLE",
                Map.of(
                        "cardNo", card.cardNo(),
                        "rawTextRu", card.rawTextRu(),
                        "transcriptionConfidence", card.transcriptionConfidence()
                )
        );
    }
}
