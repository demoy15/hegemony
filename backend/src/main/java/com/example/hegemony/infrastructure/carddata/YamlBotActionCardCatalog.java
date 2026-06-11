package com.example.hegemony.infrastructure.carddata;

import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaActionCard;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaCardRegistry;
import com.example.hegemony.domain.automa.worker.WorkerAutomaActionCard;
import com.example.hegemony.domain.automa.worker.WorkerAutomaCardRegistry;
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
    private final WorkerAutomaCardRegistry workerRegistry;

    public YamlBotActionCardCatalog(
            CapitalistAutomaCardRegistry capitalistRegistry,
            WorkerAutomaCardRegistry workerRegistry
    ) {
        this.capitalistRegistry = capitalistRegistry;
        this.workerRegistry = workerRegistry;
    }

    @Override
    public Optional<BotActionCardDefinition> findById(String botCardId) {
        if (botCardId == null || botCardId.isBlank()) {
            return Optional.empty();
        }
        String normalized = botCardId.trim().toLowerCase();
        try {
            if (normalized.startsWith("capitalist-")) {
                int cardNo = Integer.parseInt(normalized.substring("capitalist-".length()));
                return capitalistRegistry.findActionCard(cardNo).map(this::toDefinition);
            }
            if (normalized.startsWith("worker-")) {
                int cardNo = Integer.parseInt(normalized.substring("worker-".length()));
                return workerRegistry.findActionCard(cardNo).map(this::toDefinition);
            }
            return Optional.empty();
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    @Override
    public List<BotActionCardDefinition> listForClass(ClassType classType) {
        return switch (classType) {
            case CAPITALIST -> capitalistRegistry.actionCards().stream().map(this::toDefinition).toList();
            case WORKER -> workerRegistry.actionCards().stream().map(this::toDefinition).toList();
            default -> List.of();
        };
    }

    @Override
    public boolean isSimpleAutomaDatasetInstalled(ClassType classType) {
        return switch (classType) {
            case CAPITALIST -> capitalistRegistry.isLoaded();
            case WORKER -> workerRegistry.isLoaded();
            default -> false;
        };
    }

    @Override
    public String datasetStatus(ClassType classType) {
        return switch (classType) {
            case CAPITALIST -> capitalistRegistry.datasetStatus();
            case WORKER -> workerRegistry.datasetStatus();
            default -> "CARD_DATA_NOT_INSTALLED";
        };
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

    private BotActionCardDefinition toDefinition(WorkerAutomaActionCard card) {
        return new BotActionCardDefinition(
                "worker-" + card.cardNo(),
                ClassType.WORKER,
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
