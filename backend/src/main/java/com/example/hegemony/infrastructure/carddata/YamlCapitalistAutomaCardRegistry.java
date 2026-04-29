package com.example.hegemony.infrastructure.carddata;

import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaActionCard;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaActionSymbol;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaBonus;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaCardRegistry;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaInstructionCard;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaInstructionMode;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaInstructionType;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaPolicyTag;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaSpecialAction;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaSymbolMap;
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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
public class YamlCapitalistAutomaCardRegistry implements CapitalistAutomaCardRegistry {
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final CapitalistAutomaSymbolMap symbolMap = new CapitalistAutomaSymbolMap();
    private final List<CapitalistAutomaActionCard> actionCards;
    private final List<CapitalistAutomaInstructionCard> instructionCards;
    private final String datasetStatus;
    private final boolean loaded;

    public YamlCapitalistAutomaCardRegistry(
            @Value("${hegemony.automa.capitalist.action-cards-path:../specs/automa/capitalists/action-cards.yaml}") String actionCardsPath,
            @Value("${hegemony.automa.capitalist.instruction-cards-path:../specs/automa/capitalists/instruction-cards.yaml}") String instructionCardsPath
    ) {
        List<CapitalistAutomaActionCard> parsedActionCards = new ArrayList<>();
        List<CapitalistAutomaInstructionCard> parsedInstructionCards = new ArrayList<>();
        String status = "READY";
        boolean dataLoaded = true;

        try {
            JsonNode actionRoot = readRootNode(actionCardsPath, "specs/automa/capitalists/action-cards.yaml");
            parsedActionCards.addAll(parseActionCards(actionRoot));
        } catch (Exception ex) {
            status = "ACTION_CARDS_NOT_AVAILABLE";
            dataLoaded = false;
        }

        try {
            JsonNode instructionRoot = readRootNode(instructionCardsPath, "specs/automa/capitalists/instruction-cards.yaml");
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
            return "READY_PARTIAL_DECK_NO_INSTRUCTIONS";
        }
        return "READY_PARTIAL_DECK";
    }

    @Override
    public List<CapitalistAutomaActionCard> actionCards() {
        return actionCards;
    }

    @Override
    public List<CapitalistAutomaInstructionCard> instructionCards() {
        return instructionCards;
    }

    @Override
    public Optional<CapitalistAutomaActionCard> findActionCard(int cardNo) {
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
        throw new IOException("Automa card data file not found: " + external + " or classpath:" + classpathLocation);
    }

    private List<CapitalistAutomaActionCard> parseActionCards(JsonNode root) {
        JsonNode cardsNode = root.path("automa_action_cards_partial");
        List<CapitalistAutomaActionCard> cards = new ArrayList<>();
        if (!cardsNode.isArray()) {
            return cards;
        }

        for (JsonNode cardNode : cardsNode) {
            int cardNo = cardNode.path("card_no").asInt(-1);
            if (cardNo <= 0) {
                continue;
            }
            List<CapitalistAutomaActionSymbol> checks = new ArrayList<>();
            for (JsonNode checkNode : cardNode.path("checks")) {
                symbolMap.parseActionCheck(checkNode.asText()).ifPresent(checks::add);
            }

            List<CapitalistAutomaPolicyTag> policyTags = new ArrayList<>();
            for (JsonNode policyNode : cardNode.path("policy_tags")) {
                symbolMap.parsePolicyTag(policyNode.asText()).ifPresent(policyTags::add);
            }

            JsonNode bonusNode = cardNode.path("bonus");
            CapitalistAutomaBonus bonus = null;
            if (bonusNode.isObject()) {
                Map<String, Object> effectParams = Map.of();
                JsonNode effectNode = bonusNode.path("effect");
                if (effectNode.isObject()) {
                    effectParams = yamlMapper.convertValue(effectNode, new TypeReference<Map<String, Object>>() {
                    });
                }
                bonus = new CapitalistAutomaBonus(
                        bonusNode.path("trigger").asText(""),
                        effectNode.path("type").asText(""),
                        effectParams,
                        bonusNode.path("raw_text_ru").asText("")
                );
            }

            JsonNode specialActionNode = cardNode.path("special_action");
            CapitalistAutomaSpecialAction specialAction = null;
            if (specialActionNode.isObject()) {
                Map<String, Object> params = specialActionNode.path("params").isObject()
                        ? yamlMapper.convertValue(specialActionNode.path("params"), new TypeReference<Map<String, Object>>() {
                        })
                        : Map.of();
                List<String> conditions = specialActionNode.path("conditions").isArray()
                        ? yamlMapper.convertValue(specialActionNode.path("conditions"), new TypeReference<List<String>>() {
                        })
                        : List.of();
                specialAction = new CapitalistAutomaSpecialAction(
                        specialActionNode.path("type").asText(""),
                        params,
                        conditions,
                        specialActionNode.path("raw_text_ru").asText("")
                );
            }

            JsonNode sourceNode = cardNode.path("source");
            cards.add(new CapitalistAutomaActionCard(
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

    private List<CapitalistAutomaInstructionCard> parseInstructionCards(JsonNode root) {
        JsonNode cardsNode = root.path("instruction_cards");
        List<CapitalistAutomaInstructionCard> cards = new ArrayList<>();
        if (!cardsNode.isArray()) {
            return cards;
        }

        for (JsonNode cardNode : cardsNode) {
            String id = cardNode.path("id").asText("");
            if (id.isBlank()) {
                continue;
            }
            CapitalistAutomaInstructionType type = parseInstructionType(cardNode.path("type").asText("CHECK"));
            CapitalistAutomaActionSymbol appliesTo = symbolMap.parseActionCheck(cardNode.path("applies_to").asText("")).orElse(null);
            CapitalistAutomaInstructionMode mode = parseInstructionMode(cardNode.path("mode").asText("BOTH"));
            List<Map<String, Object>> structuredRules = cardNode.path("structured_rules").isArray()
                    ? yamlMapper.convertValue(cardNode.path("structured_rules"), new TypeReference<List<Map<String, Object>>>() {
                    })
                    : List.of();
            List<String> notes = cardNode.path("notes").isArray()
                    ? yamlMapper.convertValue(cardNode.path("notes"), new TypeReference<List<String>>() {
                    })
                    : List.of();

            cards.add(new CapitalistAutomaInstructionCard(
                    id,
                    type,
                    appliesTo,
                    mode,
                    cardNode.path("raw_text_ru").asText(""),
                    structuredRules,
                    notes
            ));
        }
        return cards;
    }

    private CapitalistAutomaInstructionType parseInstructionType(String raw) {
        try {
            return CapitalistAutomaInstructionType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return CapitalistAutomaInstructionType.CHECK;
        }
    }

    private CapitalistAutomaInstructionMode parseInstructionMode(String raw) {
        try {
            return CapitalistAutomaInstructionMode.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return CapitalistAutomaInstructionMode.BOTH;
        }
    }
}
