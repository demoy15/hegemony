package com.example.hegemony.infrastructure.carddata;

import com.example.hegemony.domain.automa.worker.WorkerAutomaActionCard;
import com.example.hegemony.domain.automa.worker.WorkerAutomaActionSymbol;
import com.example.hegemony.domain.automa.worker.WorkerAutomaBonus;
import com.example.hegemony.domain.automa.worker.WorkerAutomaCardRegistry;
import com.example.hegemony.domain.automa.worker.WorkerAutomaInstructionCard;
import com.example.hegemony.domain.automa.worker.WorkerAutomaPolicyTag;
import com.example.hegemony.domain.automa.worker.WorkerAutomaSpecialAction;
import com.example.hegemony.domain.automa.worker.WorkerAutomaSymbolMap;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class YamlWorkerAutomaCardRegistry implements WorkerAutomaCardRegistry {
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final WorkerAutomaSymbolMap symbolMap = new WorkerAutomaSymbolMap();
    private final List<WorkerAutomaActionCard> actionCards;
    private final List<WorkerAutomaInstructionCard> instructionCards;
    private final String datasetStatus;
    private final boolean loaded;

    public YamlWorkerAutomaCardRegistry(
            @Value("${hegemony.automa.worker.action-cards-path:../specs/automa/workers/action-cards.yaml}") String actionCardsPath,
            @Value("${hegemony.automa.worker.instruction-cards-path:../specs/automa/workers/instruction-cards.yaml}") String instructionCardsPath
    ) {
        List<WorkerAutomaActionCard> parsedActionCards = new ArrayList<>();
        List<WorkerAutomaInstructionCard> parsedInstructionCards = new ArrayList<>();
        String status = "READY";
        boolean dataLoaded = true;

        try {
            JsonNode actionRoot = readRootNode(actionCardsPath, "specs/automa/workers/action-cards.yaml");
            parsedActionCards.addAll(parseActionCards(actionRoot));
        } catch (Exception ex) {
            status = "ACTION_CARDS_NOT_AVAILABLE";
            dataLoaded = false;
        }

        try {
            JsonNode instructionRoot = readRootNode(instructionCardsPath, "specs/automa/workers/instruction-cards.yaml");
            parsedInstructionCards.addAll(parseInstructionCards(instructionRoot));
        } catch (Exception ex) {
            if (dataLoaded) {
                status = "READY_PARTIAL_DECK_NO_INSTRUCTIONS";
            }
        }

        if (dataLoaded && parsedActionCards.isEmpty()) {
            status = "ACTION_CARDS_EMPTY";
            dataLoaded = false;
        }

        this.actionCards = List.copyOf(parsedActionCards);
        this.instructionCards = List.copyOf(parsedInstructionCards);
        this.datasetStatus = status;
        this.loaded = dataLoaded;
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public String datasetStatus() {
        if (!loaded) {
            return datasetStatus;
        }
        if (instructionCards.isEmpty()) {
            return "READY_WORKER_DECK_NO_INSTRUCTIONS";
        }
        return "READY_WORKER_DECK";
    }

    @Override
    public List<WorkerAutomaActionCard> actionCards() {
        return actionCards;
    }

    @Override
    public List<WorkerAutomaInstructionCard> instructionCards() {
        return instructionCards;
    }

    @Override
    public Optional<WorkerAutomaActionCard> findActionCard(int cardNo) {
        return actionCards.stream().filter(card -> card.cardNo() == cardNo).findFirst();
    }

    private JsonNode readRootNode(String externalPath, String classpathLocation) throws IOException {
        Path external = Path.of(externalPath).toAbsolutePath().normalize();
        if (Files.exists(external)) {
            try (InputStream in = Files.newInputStream(external)) {
                return yamlMapper.readTree(in);
            }
        }

        Resource resource = new ClassPathResource(classpathLocation);
        if (resource.exists()) {
            try (InputStream in = resource.getInputStream()) {
                return yamlMapper.readTree(in);
            }
        }
        throw new IOException("Worker automa card data file not found: " + external + " or classpath:" + classpathLocation);
    }

    private List<WorkerAutomaActionCard> parseActionCards(JsonNode root) {
        JsonNode cardsNode = root.path("automa_action_cards_partial");
        List<WorkerAutomaActionCard> cards = new ArrayList<>();
        if (!cardsNode.isArray()) {
            return cards;
        }

        for (JsonNode cardNode : cardsNode) {
            int cardNo = cardNode.path("card_no").asInt(-1);
            if (cardNo <= 0) {
                continue;
            }
            List<WorkerAutomaActionSymbol> checks = new ArrayList<>();
            for (JsonNode checkNode : cardNode.path("checks")) {
                symbolMap.parseActionCheck(checkNode.asText()).ifPresent(checks::add);
            }

            List<WorkerAutomaPolicyTag> policyTags = new ArrayList<>();
            for (JsonNode policyNode : cardNode.path("policy_tags")) {
                symbolMap.parsePolicyTag(policyNode.asText()).ifPresent(policyTags::add);
            }

            WorkerAutomaBonus bonus = parseBonus(cardNode.path("bonus"));
            WorkerAutomaSpecialAction specialAction = parseSpecialAction(cardNode.path("special_action"));
            JsonNode sourceNode = cardNode.path("source");
            cards.add(new WorkerAutomaActionCard(
                    cardNo,
                    checks,
                    policyTags,
                    bonus,
                    specialAction,
                    cardNode.path("raw_text_ru").asText(""),
                    sourceNode.path("image_ref").asText(""),
                    sourceNode.path("transcription_confidence").asText("unknown")
            ));
        }

        return cards;
    }

    private WorkerAutomaBonus parseBonus(JsonNode bonusNode) {
        if (!bonusNode.isObject()) {
            return null;
        }
        JsonNode effectNode = bonusNode.path("effect");
        Map<String, Object> effectParams = effectNode.isObject()
                ? yamlMapper.convertValue(effectNode, new TypeReference<Map<String, Object>>() {
                })
                : Map.of();
        return new WorkerAutomaBonus(
                bonusNode.path("trigger").asText(""),
                effectNode.path("type").asText(""),
                effectParams,
                bonusNode.path("raw_text_ru").asText("")
        );
    }

    private WorkerAutomaSpecialAction parseSpecialAction(JsonNode specialActionNode) {
        if (!specialActionNode.isObject()) {
            return null;
        }
        Map<String, Object> params = specialActionNode.path("params").isObject()
                ? yamlMapper.convertValue(specialActionNode.path("params"), new TypeReference<Map<String, Object>>() {
                })
                : Map.of();
        List<String> conditions = specialActionNode.path("conditions").isArray()
                ? yamlMapper.convertValue(specialActionNode.path("conditions"), new TypeReference<List<String>>() {
                })
                : List.of();
        return new WorkerAutomaSpecialAction(
                specialActionNode.path("type").asText(""),
                params,
                conditions,
                specialActionNode.path("raw_text_ru").asText("")
        );
    }

    private List<WorkerAutomaInstructionCard> parseInstructionCards(JsonNode root) {
        JsonNode cardsNode = root.path("instruction_cards");
        List<WorkerAutomaInstructionCard> cards = new ArrayList<>();
        if (!cardsNode.isArray()) {
            return cards;
        }

        for (JsonNode cardNode : cardsNode) {
            String id = cardNode.path("id").asText("");
            if (id.isBlank()) {
                continue;
            }
            List<Map<String, Object>> structuredRules = cardNode.path("structured_rules").isArray()
                    ? yamlMapper.convertValue(cardNode.path("structured_rules"), new TypeReference<List<Map<String, Object>>>() {
                    })
                    : List.of();
            List<String> notes = cardNode.path("notes").isArray()
                    ? yamlMapper.convertValue(cardNode.path("notes"), new TypeReference<List<String>>() {
                    })
                    : List.of();
            cards.add(new WorkerAutomaInstructionCard(
                    id,
                    cardNode.path("type").asText("CHECK"),
                    symbolMap.parseActionCheck(cardNode.path("applies_to").asText("")).orElse(null),
                    cardNode.path("mode").asText("SIMPLE"),
                    cardNode.path("raw_text_ru").asText(""),
                    structuredRules,
                    notes
            ));
        }
        return cards;
    }
}
