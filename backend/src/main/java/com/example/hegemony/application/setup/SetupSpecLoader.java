package com.example.hegemony.application.setup;

import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.domain.model.PopulationScale;
import com.example.hegemony.domain.model.PublicServicesState;
import com.example.hegemony.domain.model.VotingBagState;
import com.example.hegemony.domain.model.WorkerQualification;
import com.example.hegemony.domain.model.WorkerSlotColor;
import com.example.hegemony.domain.model.WorkerSector;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class SetupSpecLoader {
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final String setupSpecPath;

    public SetupSpecLoader(@Value("${hegemony.setup-spec-path:../specs/setup-spec.yaml}") String setupSpecPath) {
        this.setupSpecPath = setupSpecPath;
    }

    public SetupSpecModel load() {
        try {
            JsonNode root = readRootNode();
            SetupSpecModel model = new SetupSpecModel();

            parseActiveClasses(root.path("player_count_config"), model);
            parsePolicies(root.path("initial_policy_courses"), model);
            parseBoardCore(root.path("initial_board_state"), model);
            parseCapitalistEnterpriseRegistry(root.path("enterprise_registry"), model);
            parseVotingBag(root.path("initial_voting_bag"), model);
            parsePlayerSetup(root.path("initial_player_state"), model);
            parseEnterprises(root.path("initial_board_state"), model);
            parseWorkerPlacements(root.path("initial_player_state"), model);
            return model;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load setup-spec.yaml", ex);
        }
    }

    private JsonNode readRootNode() throws IOException {
        Path external = Path.of(setupSpecPath).toAbsolutePath().normalize();
        if (Files.exists(external)) {
            try (InputStream in = Files.newInputStream(external)) {
                return yamlMapper.readTree(in);
            }
        }

        Resource resource = new ClassPathResource("specs/setup-spec.yaml");
        if (resource.exists()) {
            try (InputStream in = resource.getInputStream()) {
                return yamlMapper.readTree(in);
            }
        }

        throw new IllegalStateException("setup-spec.yaml not found. Checked: " + external + " and classpath:specs/setup-spec.yaml");
    }

    private void parseActiveClasses(JsonNode node, SetupSpecModel model) {
        for (int playerCount : List.of(2, 3, 4)) {
            JsonNode entry = node.path(String.valueOf(playerCount)).path("active_player_classes");
            List<ClassType> activeClasses = new ArrayList<>();
            for (JsonNode item : entry) {
                activeClasses.add(parseClassType(item.asText()));
            }
            model.getActiveClassesByPlayerCount().put(playerCount, activeClasses);
        }
    }

    private void parsePolicies(JsonNode node, SetupSpecModel model) {
        for (PolicyId policyId : PolicyId.values()) {
            String key = policyId.name().toLowerCase(Locale.ROOT);
            JsonNode valueNode = node.path(key);
            if (valueNode.isMissingNode() || valueNode.isNull()) {
                continue;
            }
            model.getInitialPolicyCourses().put(policyId, PolicyCourse.valueOf(valueNode.asText()));
        }
    }

    private void parseBoardCore(JsonNode boardNode, SetupSpecModel model) {
        model.setTreasury(boardNode.path("state_treasury").asInt(120));
        model.setRoundMarker(boardNode.path("round_marker").asInt(1));
        model.setTaxMultiplier(boardNode.path("tax_multiplier").asInt(5));

        JsonNode publicServicesNode = boardNode.path("starting_public_services_by_player_count");
        model.getPublicServicesByPlayerCount().put(2, parsePublicServices(publicServicesNode.path("2")));
        PublicServicesState ps34 = parsePublicServices(publicServicesNode.path("3_or_4"));
        model.getPublicServicesByPlayerCount().put(3, ps34);
        model.getPublicServicesByPlayerCount().put(4, ps34.copy());
    }

    private PublicServicesState parsePublicServices(JsonNode node) {
        return new PublicServicesState(
                node.path("healthcare").asInt(),
                node.path("education").asInt(),
                node.path("media_influence").asInt()
        );
    }

    private void parseVotingBag(JsonNode node, SetupSpecModel model) {
        JsonNode cubes = node.path("cubes_added_to_bag_at_start");
        model.setVotingBag(new VotingBagState(
                cubes.path("worker").asInt(8),
                cubes.path("middle_class").asInt(8),
                cubes.path("capitalist").asInt(8)
        ));
    }

    private void parsePlayerSetup(JsonNode node, SetupSpecModel model) {
        parsePlayer(node.path("worker"), ClassType.WORKER, model);
        parsePlayer(node.path("middle_class"), ClassType.MIDDLE_CLASS, model);
        parsePlayer(node.path("capitalist"), ClassType.CAPITALIST, model);
        parsePlayer(node.path("state"), ClassType.STATE, model);
    }

    private void parsePlayer(JsonNode node, ClassType classType, SetupSpecModel model) {
        if (node == null || node.isMissingNode()) {
            return;
        }
        SetupSpecModel.PlayerSetup setup = new SetupSpecModel.PlayerSetup();

        int money = node.path("money").asInt(Integer.MIN_VALUE);
        if (money == Integer.MIN_VALUE) {
            money = node.path("revenue").asInt(0);
        }

        setup.setMoney(money);
        setup.setInfluence(node.path("personal_influence").asInt(1));
        setup.setPopulation(populationFromMarkerWorkers(node.path("population_marker_workers").asInt(0)));
        setup.setWelfare(node.path("welfare").asInt(0));
        setup.setLegitimacyWorker(node.path("legitimacy_worker").asInt(0));
        setup.setLegitimacyMiddleClass(node.path("legitimacy_middle_class").asInt(0));
        setup.setLegitimacyCapitalist(node.path("legitimacy_capitalist").asInt(0));
        setup.setProposalTokenCount(node.path("proposal_tokens").path("count").asInt(3));

        JsonNode resourcesNode = node.path("starting_resources");
        Map<String, Integer> resources = new HashMap<>();
        if (resourcesNode.isObject()) {
            resourcesNode.fields().forEachRemaining(entry -> resources.put(entry.getKey(), entry.getValue().asInt()));
        }
        setup.setResources(resources);

        JsonNode pricesNode = node.path("starting_prices");
        Map<String, Integer> prices = new HashMap<>();
        if (pricesNode.isObject()) {
            pricesNode.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                if (key == null) {
                    return;
                }
                String normalized = key.toLowerCase(Locale.ROOT);
                if (normalized.endsWith("_price")) {
                    normalized = normalized.substring(0, normalized.length() - "_price".length());
                }
                prices.put(normalized, entry.getValue().asInt());
            });
        }
        setup.setPrices(prices);
        model.getPlayers().put(classType, setup);
    }

    private int populationFromMarkerWorkers(int markerWorkers) {
        return PopulationScale.fromWorkerCount(markerWorkers);
    }

    private void parseEnterprises(JsonNode boardNode, SetupSpecModel model) {
        JsonNode capitalistWageLevels = boardNode.path("capitalist_starting_enterprises");
        JsonNode capitalistLayouts = boardNode.path("capitalist_starting_layout_by_player_count");

        for (int playerCount : List.of(2, 3, 4)) {
            List<SetupSpecModel.EnterpriseSeed> seeds = new ArrayList<>();

            List<String> capitalistIds = readCapitalistStartingLayoutIds(
                    capitalistLayouts.path(String.valueOf(playerCount)),
                    capitalistWageLevels
            );
            capitalistIds.forEach(enterpriseId -> {
                SetupSpecModel.EnterpriseDefinition definition = model.capitalistEnterpriseDefinition(enterpriseId);
                seeds.add(new SetupSpecModel.EnterpriseSeed(
                        enterpriseId,
                        ClassType.CAPITALIST,
                        definition == null ? inferEnterpriseSector(enterpriseId) : mapSectorFromCategory(definition.getCategory()),
                        capitalistWageLevels.path(enterpriseId).path("salary_level").asInt(2)
                ));
            });

            if (playerCount >= 3) {
                JsonNode middle = boardNode.path("middle_class_starting_enterprises_for_3_or_4_players");
                middle.fields().forEachRemaining(entry -> {
                    seeds.add(new SetupSpecModel.EnterpriseSeed(
                            entry.getKey(),
                            ClassType.MIDDLE_CLASS,
                            inferEnterpriseSector(entry.getKey()),
                            entry.getValue().path("salary_level").asInt(2)
                    ));
                });
            }

            JsonNode stateEntries = boardNode.path("state_enterprises_setup")
                    .path("first_row_starting_state_enterprises_under_public_service_cells");
            int stateWage = boardNode.path("state_enterprises_setup")
                    .path("first_row_starting_state_enterprises_salary_level").asInt(2);
            stateEntries.fields().forEachRemaining(entry -> {
                JsonNode byPlayers = entry.getValue();
                String chosen = playerCount == 2
                        ? byPlayers.path("for_2_players").asText("")
                        : byPlayers.path("for_3_or_4_players").asText("");
                if (!chosen.isBlank() && !"UNKNOWN".equalsIgnoreCase(chosen)) {
                    seeds.add(new SetupSpecModel.EnterpriseSeed(
                            chosen,
                            ClassType.STATE,
                            inferEnterpriseSector(chosen),
                            stateWage
                    ));
                }
            });

            model.getEnterprisesByPlayerCount().put(playerCount, seeds);
        }
    }

    private List<String> readCapitalistStartingLayoutIds(JsonNode layoutNode, JsonNode fallbackMapNode) {
        List<String> ids = new ArrayList<>();
        if (layoutNode != null && layoutNode.isArray()) {
            for (JsonNode item : layoutNode) {
                String id = item.asText("");
                if (!id.isBlank()) {
                    ids.add(id);
                }
            }
            if (!ids.isEmpty()) {
                return ids;
            }
        }

        if (fallbackMapNode != null && fallbackMapNode.isObject()) {
            fallbackMapNode.fields().forEachRemaining(entry -> ids.add(entry.getKey()));
        }
        return ids;
    }

    private void parseCapitalistEnterpriseRegistry(JsonNode registryNode, SetupSpecModel model) {
        JsonNode listNode = registryNode.path("capitalist_enterprises");
        if (!listNode.isArray()) {
            return;
        }

        for (JsonNode entry : listNode) {
            String id = entry.path("id").asText("");
            if (id.isBlank()) {
                continue;
            }
            SetupSpecModel.EnterpriseDefinition definition = new SetupSpecModel.EnterpriseDefinition();
            definition.setId(id);
            definition.setName(entry.path("name").asText(id));
            definition.setCategory(entry.path("category").asText("general"));
            definition.setCost(entry.path("cost").asInt(0));
            definition.setStarting(entry.path("is_starting").asBoolean(false));
            definition.setProduction(parseProduction(entry.path("production")));
            definition.setWorkers(parseWorkers(entry.path("workers")));
            definition.setWages(parseWages(entry.path("wages")));
            definition.setAutomation(parseAutomation(entry.path("automation")));
            model.getCapitalistEnterpriseDefinitions().put(id, definition);
        }
    }

    private SetupSpecModel.ProductionDefinition parseProduction(JsonNode node) {
        SetupSpecModel.ProductionDefinition production = new SetupSpecModel.ProductionDefinition();
        production.setOutput(node.path("output").asText(""));
        production.setAmount(node.path("amount").asInt(0));
        if (node.has("per_workers") && !node.path("per_workers").isNull()) {
            production.setPerWorkers(node.path("per_workers").asInt(0));
        }
        return production;
    }

    private SetupSpecModel.WorkerSpec parseWorkers(JsonNode node) {
        SetupSpecModel.WorkerSpec workers = new SetupSpecModel.WorkerSpec();
        List<SetupSpecModel.WorkerSlotDefinition> slots = new ArrayList<>();
        JsonNode slotsNode = node.path("slots");
        if (slotsNode.isArray()) {
            for (JsonNode slotNode : slotsNode) {
                SetupSpecModel.WorkerSlotDefinition slot = new SetupSpecModel.WorkerSlotDefinition();
                slot.setKind(slotNode.path("kind").asText("worker"));
                slot.setCount(Math.max(0, slotNode.path("count").asInt(0)));
                String qualification = slotNode.path("qualification").asText("unskilled").toUpperCase(Locale.ROOT);
                slot.setQualification("SKILLED".equals(qualification) ? WorkerQualification.SKILLED : WorkerQualification.UNSKILLED);
                slot.setColor(WorkerSlotColor.fromRaw(slotNode.path("color").asText(null)));
                slots.add(slot);
            }
        }
        workers.setSlots(slots);
        return workers;
    }

    private SetupSpecModel.WageTrack parseWages(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        SetupSpecModel.WageTrack wages = new SetupSpecModel.WageTrack();
        if (node.has("low") && !node.path("low").isNull()) {
            wages.setLow(node.path("low").asInt());
        }
        if (node.has("medium") && !node.path("medium").isNull()) {
            wages.setMedium(node.path("medium").asInt());
        }
        if (node.has("high") && !node.path("high").isNull()) {
            wages.setHigh(node.path("high").asInt());
        }
        return wages;
    }

    private SetupSpecModel.AutomationDefinition parseAutomation(JsonNode node) {
        SetupSpecModel.AutomationDefinition automation = new SetupSpecModel.AutomationDefinition();
        if (node == null || node.isMissingNode() || node.isNull()) {
            automation.setAutomated(false);
            return automation;
        }
        automation.setAutomated(node.path("automated").asBoolean(false));
        return automation;
    }

    private void parseWorkerPlacements(JsonNode playersNode, SetupSpecModel model) {
        for (int playerCount : List.of(2, 3, 4)) {
            List<SetupSpecModel.WorkerPlacementSeed> placements = new ArrayList<>();
            JsonNode workerPlacements = playerCount == 2
                    ? playersNode.path("worker").path("starting_worker_placement").path("for_2_players")
                    : playersNode.path("worker").path("starting_worker_placement").path("for_3_or_4_players");
            readEnterprisePlacement(workerPlacements, ClassType.WORKER, placements);

            if (playerCount >= 3) {
                JsonNode middlePlacements = playersNode.path("middle_class").path("starting_worker_placement");
                readEnterprisePlacement(middlePlacements, ClassType.MIDDLE_CLASS, placements);
            }

            // Deterministic unemployment seed derived from setup-spec:
            // first_add + one proxy worker per revealed migrant card (card contents are randomized and not decoded in this slice).
            int workerBase = playersNode.path("worker")
                    .path("unemployment_setup")
                    .path("first_add")
                    .path("gray_worker")
                    .asInt(1);
            int workerMigrantCards = playerCount == 2
                    ? playersNode.path("worker").path("unemployment_setup").path("then").path("for_2_players").path("reveal_migrant_cards").asInt(0)
                    : playersNode.path("worker").path("unemployment_setup").path("then").path("for_3_or_4_players").path("reveal_migrant_cards").asInt(0);
            int workerUnemploymentSeedCount = Math.max(1, workerBase) + Math.max(0, workerMigrantCards);
            placements.add(new SetupSpecModel.WorkerPlacementSeed(
                    ClassType.WORKER,
                    null,
                    WorkerQualification.UNSKILLED,
                    null,
                    workerUnemploymentSeedCount
            ));
            if (playerCount >= 3) {
                int middleBase = playersNode.path("middle_class")
                        .path("unemployment_setup")
                        .path("first_add")
                        .path("any_middle_class_skilled_worker")
                        .asInt(1);
                int middleMigrantCards = playersNode.path("middle_class")
                        .path("unemployment_setup")
                        .path("then")
                        .path("reveal_migrant_cards")
                        .asInt(0);
                int middleUnemploymentSeedCount = Math.max(1, middleBase) + Math.max(0, middleMigrantCards);
                placements.add(new SetupSpecModel.WorkerPlacementSeed(
                        ClassType.MIDDLE_CLASS,
                        null,
                        WorkerQualification.SKILLED,
                        WorkerSector.GENERAL,
                        middleUnemploymentSeedCount
                ));
            }

            model.getWorkerPlacementsByPlayerCount().put(playerCount, placements);
        }
    }

    private void readEnterprisePlacement(
            JsonNode node,
            ClassType classType,
            List<SetupSpecModel.WorkerPlacementSeed> placements
    ) {
        if (!node.isObject()) {
            return;
        }

        node.fields().forEachRemaining(entry -> {
            String enterpriseId = entry.getKey();
            JsonNode placementSpec = entry.getValue();
            if (!placementSpec.isObject() || enterpriseId.startsWith("all_")) {
                return;
            }

            placementSpec.fields().forEachRemaining(countEntry -> {
                String key = countEntry.getKey();
                int count = countEntry.getValue().asInt(0);
                if (count <= 0) {
                    return;
                }

                WorkerQualification qualification = key.contains("unskilled")
                        ? WorkerQualification.UNSKILLED
                        : WorkerQualification.SKILLED;
                WorkerSector sector = qualification == WorkerQualification.UNSKILLED
                        ? null
                        : parseSkilledSectorKey(key);

                placements.add(new SetupSpecModel.WorkerPlacementSeed(
                        classType,
                        enterpriseId,
                        qualification,
                        sector,
                        count
                ));
            });
        });
    }

    private ClassType parseClassType(String raw) {
        return ClassType.valueOf(raw.toUpperCase(Locale.ROOT));
    }

    private WorkerSector parseSkilledSectorKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("green")) {
            return WorkerSector.GREEN;
        }
        if (normalized.startsWith("blue")) {
            return WorkerSector.BLUE;
        }
        if (normalized.startsWith("purple")) {
            return WorkerSector.PURPLE;
        }
        if (normalized.startsWith("red")) {
            return WorkerSector.WHITE;
        }
        if (normalized.startsWith("white")) {
            return WorkerSector.WHITE;
        }
        if (normalized.startsWith("orange")) {
            return WorkerSector.ORANGE;
        }
        return WorkerSector.GENERAL;
    }

    private WorkerSector inferEnterpriseSector(String enterpriseId) {
        String id = enterpriseId.toLowerCase(Locale.ROOT);
        if (id.contains("hospital") || id.contains("clinic")) {
            return WorkerSector.HEALTHCARE;
        }
        if (id.contains("college") || id.contains("university")) {
            return WorkerSector.EDUCATION;
        }
        if (id.contains("media") || id.contains("influence")) {
            return WorkerSector.MEDIA;
        }
        if (id.contains("market") || id.contains("shop")) {
            return WorkerSector.RETAIL;
        }
        return WorkerSector.GENERAL;
    }

    private WorkerSector mapSectorFromCategory(String category) {
        if (category == null || category.isBlank()) {
            return WorkerSector.GENERAL;
        }
        return switch (category.toLowerCase(Locale.ROOT)) {
            case "food" -> WorkerSector.GREEN;
            case "luxury" -> WorkerSector.BLUE;
            case "healthcare" -> WorkerSector.WHITE;
            case "education" -> WorkerSector.ORANGE;
            case "media", "influence" -> WorkerSector.PURPLE;
            default -> WorkerSector.GENERAL;
        };
    }
}
