package com.example.hegemony.web;

import com.example.hegemony.application.GameStateRepository;
import com.example.hegemony.domain.model.ClassType;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BotControlAndPreviewIntegrationTest {
    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    GameStateRepository gameStateRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/game";
        restTemplate.postForEntity(baseUrl + "/reset", null, JsonNode.class);
    }

    @Test
    void gameCanBeConfiguredWithHumanAndBotClasses() {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(baseUrl + "/setup", Map.of(
                "playerCount", 4,
                "controlModes", Map.of(
                        "WORKER", "HUMAN",
                        "MIDDLE_CLASS", "BOT",
                        "CAPITALIST", "BOT",
                        "STATE", "BOT"
                ),
                "botStrategyModes", Map.of(
                        "CAPITALIST", "CARD_DRIVEN_SIMPLE_AUTOMA",
                        "MIDDLE_CLASS", "HEURISTIC_FALLBACK",
                        "STATE", "HEURISTIC_FALLBACK"
                )
        ), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode players = response.getBody().path("gameState").path("players");
        assertThat(findPlayer(players, "worker").path("controlMode").asText()).isEqualTo("HUMAN");
        assertThat(findPlayer(players, "middle_class").path("controlMode").asText()).isEqualTo("BOT");
        assertThat(findPlayer(players, "capitalist").path("botStrategyMode").asText()).isEqualTo("CARD_DRIVEN_SIMPLE_AUTOMA");
    }

    @Test
    void saveLoadPreservesControlModes() {
        restTemplate.postForEntity(baseUrl + "/setup", Map.of(
                "playerCount", 4,
                "controlModes", Map.of(
                        "WORKER", "BOT",
                        "MIDDLE_CLASS", "HUMAN",
                        "CAPITALIST", "BOT",
                        "STATE", "BOT"
                ),
                "botStrategyModes", Map.of(
                        "WORKER", "HEURISTIC_FALLBACK",
                        "CAPITALIST", "CARD_DRIVEN_SIMPLE_AUTOMA"
                )
        ), JsonNode.class);

        ResponseEntity<JsonNode> save = restTemplate.postForEntity(baseUrl + "/save", Map.of("fileName", "control-modes.json"), JsonNode.class);
        assertThat(save.getStatusCode()).isEqualTo(HttpStatus.OK);

        restTemplate.postForEntity(baseUrl + "/reset", null, JsonNode.class);
        ResponseEntity<JsonNode> load = restTemplate.postForEntity(baseUrl + "/load", Map.of("fileName", "control-modes.json"), JsonNode.class);
        assertThat(load.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode players = load.getBody().path("gameState").path("players");
        assertThat(findPlayer(players, "worker").path("controlMode").asText()).isEqualTo("BOT");
        assertThat(findPlayer(players, "capitalist").path("botStrategyMode").asText()).isEqualTo("CARD_DRIVEN_SIMPLE_AUTOMA");
    }

    @Test
    void currentTurnRejectsBotTurnCommandForHumanActorIfApplicable() {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(baseUrl + "/play-bot-turn", null, JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void postPlayBotTurnWorks() {
        restTemplate.postForEntity(baseUrl + "/setup", Map.of(
                "playerCount", 4,
                "controlModes", Map.of(
                        "WORKER", "BOT",
                        "MIDDLE_CLASS", "HUMAN",
                        "CAPITALIST", "BOT",
                        "STATE", "BOT"
                ),
                "botStrategyModes", Map.of(
                        "WORKER", "HEURISTIC_FALLBACK"
                )
        ), JsonNode.class);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(baseUrl + "/play-bot-turn", null, JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("summary").path("selectedMoveId").asText()).isNotBlank();
        assertThat(response.getBody().path("summary").path("legalOptionsConsidered").asInt()).isGreaterThan(0);
    }

    @Test
    void botUsesOnlyLegalMoves() {
        restTemplate.postForEntity(baseUrl + "/setup", Map.of(
                "playerCount", 4,
                "controlModes", Map.of(
                        "WORKER", "BOT",
                        "MIDDLE_CLASS", "HUMAN",
                        "CAPITALIST", "HUMAN",
                        "STATE", "HUMAN"
                ),
                "botStrategyModes", Map.of()
        ), JsonNode.class);

        JsonNode legal = restTemplate.getForEntity(baseUrl + "/legal-moves", JsonNode.class).getBody().path("legalMoves");
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(baseUrl + "/play-bot-turn", null, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String selectedAction = response.getBody().path("summary").path("selectedAction").asText();
        assertThat(legal.toString()).contains(selectedAction);
    }

    @Test
    void postPlayBotUntilHumanWorks() {
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(baseUrl + "/play-bot-until-human", null, JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("stoppedAtHumanDecisionPoint").asBoolean()).isTrue();
    }

    @Test
    void endTurnThenPlayUntilHumanHandsControlBackToHuman() {
        restTemplate.postForEntity(baseUrl + "/setup", Map.of(
                "playerCount", 2,
                "controlModes", Map.of(
                        "WORKER", "HUMAN",
                        "CAPITALIST", "BOT"
                ),
                "botStrategyModes", Map.of(
                        "CAPITALIST", "HEURISTIC_FALLBACK"
                )
        ), JsonNode.class);

        ResponseEntity<JsonNode> endTurn = restTemplate.postForEntity(baseUrl + "/command", Map.of(
                "actionType", "END_TURN",
                "parameters", Map.of("actorPlayerId", "worker")
        ), JsonNode.class);
        assertThat(endTurn.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(endTurn.getBody().path("accepted").asBoolean()).isTrue();
        assertThat(endTurn.getBody().path("gameState").path("players")
                .get(endTurn.getBody().path("gameState").path("turnOrder").path("currentPlayerIndex").asInt())
                .path("controlMode").asText()).isEqualTo("BOT");

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(baseUrl + "/play-bot-until-human", null, JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("executedSteps").asInt()).isGreaterThan(0);
        assertThat(response.getBody().path("stoppedAtHumanDecisionPoint").asBoolean()).isTrue();
        assertThat(response.getBody().path("gameState").path("players")
                .get(response.getBody().path("gameState").path("turnOrder").path("currentPlayerIndex").asInt())
                .path("playerId").asText()).isEqualTo("worker");
    }

    @Test
    void stateShowsControlModesAndBotReadiness() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(baseUrl, JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = response.getBody();
        assertThat(body.path("gameState").path("players").get(0).path("controlMode").asText()).isNotBlank();
        assertThat(body.path("gameState").path("cardReadiness").path("enterpriseCardDatasetInstalled").isBoolean()).isTrue();
        assertThat(body.path("composerMetadata").isObject()).isTrue();
        assertThat(body.path("marketCandidates").isArray()).isTrue();
    }

    @Test
    void previewDoesNotMutateState() {
        JsonNode before = restTemplate.getForEntity(baseUrl, JsonNode.class).getBody().path("gameState");
        int beforeLogSize = before.path("eventLog").size();
        String beforeProposalOwner = before.path("policies").get(2).path("occupyingProposalToken").path("ownerPlayerId").asText("");

        ResponseEntity<JsonNode> preview = restTemplate.postForEntity(baseUrl + "/preview", Map.of(
                "actionType", "PROPOSE_BILL",
                "parameters", Map.of(
                        "actorPlayerId", "worker",
                        "policyId", "POLICY_3_TAXATION",
                        "targetCourse", "B"
                )
        ), JsonNode.class);
        assertThat(preview.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(preview.getBody().path("accepted").asBoolean()).isTrue();

        JsonNode after = restTemplate.getForEntity(baseUrl, JsonNode.class).getBody().path("gameState");
        assertThat(after.path("eventLog").size()).isEqualTo(beforeLogSize);
        assertThat(after.path("policies").get(2).path("occupyingProposalToken").path("ownerPlayerId").asText(""))
                .isEqualTo(beforeProposalOwner);
    }

    @Test
    void previewReturnsProjectedDeltaForSupportedAction() {
        var state = gameStateRepository.get().copy();
        var worker = state.findPlayerById("worker").orElseThrow();
        worker.setPopulation(1);
        worker.setGoodsAmount("healthcare", 1);
        gameStateRepository.save(state);

        ResponseEntity<JsonNode> preview = restTemplate.postForEntity(baseUrl + "/preview", Map.of(
                "actionType", "CONSUME_HEALTHCARE",
                "parameters", Map.of("actorPlayerId", "worker")
        ), JsonNode.class);

        assertThat(preview.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(preview.getBody().path("accepted").asBoolean()).isTrue();
        assertThat(preview.getBody().path("delta").path("welfareDeltaByPlayer").path("worker").asInt()).isEqualTo(1);
    }

    @Test
    void previewRejectsIllegalActionWithValidationReason() {
        ResponseEntity<JsonNode> preview = restTemplate.postForEntity(baseUrl + "/preview", Map.of(
                "actionType", "PROPOSE_BILL",
                "parameters", Map.of(
                        "actorPlayerId", "worker",
                        "policyId", "POLICY_3_TAXATION",
                        "targetCourse", "C"
                )
        ), JsonNode.class);

        assertThat(preview.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(preview.getBody().path("accepted").asBoolean()).isFalse();
        assertThat(preview.getBody().path("reasonCodes").toString()).contains("TARGET_COURSE_NOT_ADJACENT");
    }

    @Test
    void previewShowsCardDataUnavailableNoteWhenRelevant() {
        ResponseEntity<JsonNode> preview = restTemplate.postForEntity(baseUrl + "/preview", Map.of(
                "actionType", "PROPOSE_BILL",
                "optionalModifier", "some-modifier",
                "parameters", Map.of(
                        "actorPlayerId", "worker",
                        "policyId", "POLICY_3_TAXATION",
                        "targetCourse", "B"
                )
        ), JsonNode.class);

        assertThat(preview.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(preview.getBody().path("supportNotes").toString()).contains("CARD_DATA_NOT_AVAILABLE_FOR_ACTION_MODIFIER");
    }

    @Test
    void capitalistCardDrivenModeProvidesStructuredTrace() {
        restTemplate.postForEntity(baseUrl + "/setup", Map.of(
                "playerCount", 4,
                "controlModes", Map.of(
                        "WORKER", "HUMAN",
                        "MIDDLE_CLASS", "HUMAN",
                        "CAPITALIST", "BOT",
                        "STATE", "HUMAN"
                ),
                "botStrategyModes", Map.of(
                        "CAPITALIST", "CARD_DRIVEN_SIMPLE_AUTOMA"
                )
        ), JsonNode.class);

        var state = gameStateRepository.get().copy();
        int capitalistIndex = state.getTurnOrder().getActiveClasses().indexOf(ClassType.CAPITALIST);
        state.getTurnOrder().setCurrentPlayerIndex(Math.max(0, capitalistIndex));
        gameStateRepository.save(state);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(baseUrl + "/play-bot-turn", null, JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("summary").path("plannerId").asText()).contains("card-engine");
        assertThat(response.getBody().path("summary").path("automaTrace").isObject()).isTrue();
        assertThat(response.getBody().path("summary").path("automaTrace").path("currentCardNo").asInt()).isGreaterThan(0);
        assertThat(response.getBody().path("summary").path("automaTrace").path("checksTrace").isArray()).isTrue();
    }

    @Test
    void capitalistComplexModeBranchIsReflectedInTrace() {
        restTemplate.postForEntity(baseUrl + "/setup", Map.of(
                "playerCount", 4,
                "controlModes", Map.of(
                        "WORKER", "HUMAN",
                        "MIDDLE_CLASS", "HUMAN",
                        "CAPITALIST", "BOT",
                        "STATE", "HUMAN"
                ),
                "botStrategyModes", Map.of(
                        "CAPITALIST", "CARD_DRIVEN_COMPLEX_AUTOMA"
                )
        ), JsonNode.class);

        var state = gameStateRepository.get().copy();
        int capitalistIndex = state.getTurnOrder().getActiveClasses().indexOf(ClassType.CAPITALIST);
        state.getTurnOrder().setCurrentPlayerIndex(Math.max(0, capitalistIndex));
        gameStateRepository.save(state);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(baseUrl + "/play-bot-turn", null, JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("summary").path("strategyModeUsed").asText()).isEqualTo("CARD_DRIVEN_COMPLEX_AUTOMA");
        assertThat(response.getBody().path("summary").path("automaTrace").path("mode").asText()).isEqualTo("COMPLEX");
    }

    @Test
    void capitalistSimpleModeFullTurnChangesState() {
        setupTwoPlayerWorkerHumanCapitalistSimpleBot();

        ResponseEntity<JsonNode> endWorkerTurn = restTemplate.postForEntity(baseUrl + "/command", Map.of(
                "actionType", "END_TURN",
                "parameters", Map.of("actorPlayerId", "worker")
        ), JsonNode.class);
        assertThat(endWorkerTurn.getStatusCode()).isEqualTo(HttpStatus.OK);
        int beforeLogSize = endWorkerTurn.getBody().path("gameState").path("eventLog").size();

        ResponseEntity<JsonNode> botTurn = restTemplate.postForEntity(baseUrl + "/play-bot-turn", null, JsonNode.class);
        assertThat(botTurn.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(botTurn.getBody().path("summary").path("automaTrace").isObject()).isTrue();
        assertThat(botTurn.getBody().path("gameState").path("eventLog").size()).isGreaterThan(beforeLogSize);
    }

    @Test
    void capitalistSimpleModeTurnIncrementsCounterAndMovesQueue() {
        setupTwoPlayerWorkerHumanCapitalistSimpleBot();

        ResponseEntity<JsonNode> endWorkerTurn = restTemplate.postForEntity(baseUrl + "/command", Map.of(
                "actionType", "END_TURN",
                "parameters", Map.of("actorPlayerId", "worker")
        ), JsonNode.class);
        JsonNode beforeState = endWorkerTurn.getBody().path("gameState");
        JsonNode activeClasses = beforeState.path("turnOrder").path("activeClasses");
        int capitalistIndex = -1;
        for (int i = 0; i < activeClasses.size(); i++) {
            if ("CAPITALIST".equals(activeClasses.get(i).asText())) {
                capitalistIndex = i;
                break;
            }
        }
        assertThat(capitalistIndex).isGreaterThanOrEqualTo(0);
        int beforeTurns = beforeState.path("turnOrder").path("actionsTakenByPlayer").get(capitalistIndex).asInt();

        ResponseEntity<JsonNode> botTurn = restTemplate.postForEntity(baseUrl + "/play-bot-turn", null, JsonNode.class);
        assertThat(botTurn.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode afterState = botTurn.getBody().path("gameState");
        int afterTurns = afterState.path("turnOrder").path("actionsTakenByPlayer").get(capitalistIndex).asInt();
        assertThat(afterTurns).isEqualTo(beforeTurns + 1);
        assertThat(afterState.path("players")
                .get(afterState.path("turnOrder").path("currentPlayerIndex").asInt())
                .path("playerId").asText()).isEqualTo("worker");
    }

    @Test
    void advanceToVotingStaysBlockedUntilFiveTurnsPerPlayer() {
        setupTwoPlayerWorkerHumanCapitalistSimpleBot();

        ResponseEntity<JsonNode> blockedAtStart = restTemplate.postForEntity(baseUrl + "/command", Map.of(
                "actionType", "ADVANCE_TO_VOTING",
                "parameters", Map.of("actorPlayerId", "worker")
        ), JsonNode.class);
        assertThat(blockedAtStart.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(blockedAtStart.getBody().path("accepted").asBoolean()).isFalse();

        restTemplate.postForEntity(baseUrl + "/command", Map.of(
                "actionType", "END_TURN",
                "parameters", Map.of("actorPlayerId", "worker")
        ), JsonNode.class);
        restTemplate.postForEntity(baseUrl + "/play-bot-turn", null, JsonNode.class);

        ResponseEntity<JsonNode> blockedAfterOneCycle = restTemplate.postForEntity(baseUrl + "/command", Map.of(
                "actionType", "ADVANCE_TO_VOTING",
                "parameters", Map.of("actorPlayerId", "worker")
        ), JsonNode.class);
        assertThat(blockedAfterOneCycle.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(blockedAfterOneCycle.getBody().path("accepted").asBoolean()).isFalse();
    }

    @Test
    void capitalistCannotTakeSixthTurnInActionsRound() {
        setupTwoPlayerWorkerHumanCapitalistSimpleBot();

        for (int i = 0; i < 5; i++) {
            ResponseEntity<JsonNode> endWorkerTurn = restTemplate.postForEntity(baseUrl + "/command", Map.of(
                    "actionType", "END_TURN",
                    "parameters", Map.of("actorPlayerId", "worker")
            ), JsonNode.class);
            assertThat(endWorkerTurn.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(endWorkerTurn.getBody().path("accepted").asBoolean()).isTrue();

            ResponseEntity<JsonNode> botTurn = restTemplate.postForEntity(baseUrl + "/play-bot-turn", null, JsonNode.class);
            assertThat(botTurn.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        JsonNode afterFiveCycles = restTemplate.getForEntity(baseUrl, JsonNode.class).getBody().path("gameState");
        assertThat(afterFiveCycles.path("currentPhase").asText()).isNotEqualTo("ACTIONS");

        ResponseEntity<JsonNode> sixthAttempt = restTemplate.postForEntity(baseUrl + "/play-bot-turn", null, JsonNode.class);
        assertThat(sixthAttempt.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void lastAutomaTurnSummaryIsAvailableViaApi() {
        setupTwoPlayerWorkerHumanCapitalistSimpleBot();

        restTemplate.postForEntity(baseUrl + "/command", Map.of(
                "actionType", "END_TURN",
                "parameters", Map.of("actorPlayerId", "worker")
        ), JsonNode.class);
        ResponseEntity<JsonNode> botTurn = restTemplate.postForEntity(baseUrl + "/play-bot-turn", null, JsonNode.class);
        assertThat(botTurn.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<JsonNode> game = restTemplate.getForEntity(baseUrl, JsonNode.class);
        assertThat(game.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode summary = game.getBody().path("gameState").path("lastBotTurnSummary");
        assertThat(summary.isObject()).isTrue();
        assertThat(summary.path("automaTrace").path("cardId").asText()).isNotBlank();
        assertThat(summary.path("automaTrace").path("decisionText").asText()).isNotBlank();
    }

    @Test
    void simpleModeSummaryIncludesResolvedTargetFromApi() {
        setupTwoPlayerWorkerHumanCapitalistSimpleBot();

        var prepared = gameStateRepository.get().copy();
        var capitalist = prepared.findPlayerById("capitalist").orElseThrow();
        capitalist.setMoney(80);
        capitalist.setProducedResourceAmount("food", 8);
        capitalist.setProducedResourceAmount("luxury", 6);
        gameStateRepository.save(prepared);

        restTemplate.postForEntity(baseUrl + "/command", Map.of(
                "actionType", "END_TURN",
                "parameters", Map.of("actorPlayerId", "worker")
        ), JsonNode.class);

        ResponseEntity<JsonNode> botTurn = restTemplate.postForEntity(baseUrl + "/play-bot-turn", null, JsonNode.class);
        assertThat(botTurn.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(botTurn.getBody().path("summary").path("automaTrace").path("resolvedTarget").isObject()).isTrue();
        assertThat(botTurn.getBody().path("summary").path("chosenTargets").path("resolvedTarget").isObject()).isTrue();
    }

    @Test
    void capitalistSimpleModeFiveTurnsUseConcreteActionsAndRarelyFallbackToPressure() {
        setupTwoPlayerWorkerHumanCapitalistSimpleBot();

        var prepared = gameStateRepository.get().copy();
        var capitalist = prepared.findPlayerById("capitalist").orElseThrow();
        capitalist.setMoney(120);
        capitalist.setProducedResourceAmount("food", 20);
        capitalist.setProducedResourceAmount("luxury", 20);
        capitalist.setProducedResourceAmount("education", 20);
        capitalist.setProducedResourceAmount("healthcare", 20);
        prepared.findPlayerById("worker").orElseThrow().setInfluence(2);
        capitalist.setInfluence(1);
        gameStateRepository.save(prepared);

        int pressureFallbacks = 0;
        int concreteActions = 0;
        for (int i = 0; i < 5; i++) {
            ResponseEntity<JsonNode> endWorkerTurn = restTemplate.postForEntity(baseUrl + "/command", Map.of(
                    "actionType", "END_TURN",
                    "parameters", Map.of("actorPlayerId", "worker")
            ), JsonNode.class);
            assertThat(endWorkerTurn.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(endWorkerTurn.getBody().path("accepted").asBoolean()).isTrue();

            ResponseEntity<JsonNode> botTurn = restTemplate.postForEntity(baseUrl + "/play-bot-turn", null, JsonNode.class);
            assertThat(botTurn.getStatusCode()).isEqualTo(HttpStatus.OK);

            String selectedAutomaAction = botTurn.getBody().path("summary").path("automaTrace").path("selectedAutomaAction").asText("");
            if ("APPLY_POLITICAL_PRESSURE".equals(selectedAutomaAction)) {
                pressureFallbacks++;
            }
            if (selectedAutomaAction.equals("BUILD_ENTERPRISE")
                    || selectedAutomaAction.equals("SELL_ENTERPRISE")
                    || selectedAutomaAction.equals("SELL_TO_FOREIGN_MARKET")
                    || selectedAutomaAction.equals("MAKE_DEAL")
                    || selectedAutomaAction.equals("PROPOSE_BILL")) {
                concreteActions++;
            }
        }

        assertThat(concreteActions).isGreaterThan(0);
        assertThat(pressureFallbacks).isLessThan(3);
    }

    private void setupTwoPlayerWorkerHumanCapitalistSimpleBot() {
        restTemplate.postForEntity(baseUrl + "/setup", Map.of(
                "playerCount", 2,
                "controlModes", Map.of(
                        "WORKER", "HUMAN",
                        "CAPITALIST", "BOT"
                ),
                "botStrategyModes", Map.of(
                        "CAPITALIST", "CARD_DRIVEN_SIMPLE_AUTOMA"
                )
        ), JsonNode.class);
    }

    private JsonNode findPlayer(JsonNode players, String playerId) {
        for (JsonNode player : players) {
            if (playerId.equals(player.path("playerId").asText())) {
                return player;
            }
        }
        throw new IllegalStateException("Player not found: " + playerId);
    }
}
