package com.example.hegemony.web;

import com.example.hegemony.application.GameStateRepository;
import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.Enterprise;
import com.example.hegemony.domain.model.EnterpriseSlot;
import com.example.hegemony.domain.model.WorkerQualification;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameApiIntegrationTest {
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
    void getStateAfterResetSetup() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(baseUrl, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.path("gameState").path("policies").isArray()).isTrue();
        assertThat(body.path("gameState").path("policies").size()).isEqualTo(7);
    }

    @Test
    void getStateShowsRoundAndPhase() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(baseUrl, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        JsonNode gameState = response.getBody().path("gameState");
        assertThat(gameState.path("currentRound").asInt()).isEqualTo(1);
        assertThat(gameState.path("currentPhase").asText()).isEqualTo("ACTIONS");
        assertThat(gameState.path("gameStatus").asText()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void postProposeBill() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("actionType", "PROPOSE_BILL");
        payload.put("parameters", Map.of(
                "actorPlayerId", "worker",
                "policyId", "POLICY_3_TAXATION",
                "targetCourse", "B"
        ));

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(baseUrl + "/command", payload, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("accepted").asBoolean()).isTrue();
    }

    @Test
    void postAssignWorkers() {
        String enterpriseId = seedAssignableWorkerEnterprise(2);
        JsonNode state = restTemplate.getForEntity(baseUrl, JsonNode.class).getBody().path("gameState");
        List<String> workerIds = state.path("workers").findValuesAsText("id").stream().toList();

        JsonNode workers = state.path("workers");
        String worker1 = null;
        String worker2 = null;
        for (JsonNode worker : workers) {
            if ("WORKER".equals(worker.path("classType").asText())
                    && "UNEMPLOYED".equals(worker.path("location").asText())
                    && !worker.path("tiedContract").asBoolean()) {
                if (worker1 == null) {
                    worker1 = worker.path("id").asText();
                } else {
                    worker2 = worker.path("id").asText();
                    break;
                }
            }
        }
        assertThat(workerIds).isNotEmpty();
        assertThat(worker1).isNotNull();
        assertThat(worker2).isNotNull();

        String slot1 = enterpriseId + "-slot-1";
        String slot2 = enterpriseId + "-slot-2";

        Map<String, Object> payload = new HashMap<>();
        payload.put("actionType", "ASSIGN_WORKERS");
        payload.put("parameters", Map.of(
                "actorPlayerId", "worker",
                "assignments", List.of(
                        Map.of("workerId", worker1, "targetType", "ENTERPRISE_SLOT", "targetId", enterpriseId + ":" + slot1),
                        Map.of("workerId", worker2, "targetType", "ENTERPRISE_SLOT", "targetId", enterpriseId + ":" + slot2)
                )
        ));

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(baseUrl + "/command", payload, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("accepted").asBoolean()).isTrue();
    }

    @Test
    void getLegalMoves() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(baseUrl + "/legal-moves", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        JsonNode legalMoves = response.getBody().path("legalMoves");
        assertThat(legalMoves.isArray()).isTrue();
        assertThat(legalMoves.toString()).contains("PROPOSE_BILL");
    }

    @Test
    void postDeclareVoteStance() {
        setupPendingVote();

        ResponseEntity<JsonNode> response = postCommand("DECLARE_VOTE_STANCE", Map.of(
                "actorPlayerId", "middle_class",
                "policyId", "POLICY_3_TAXATION",
                "stance", "FOR"
        ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("accepted").asBoolean()).isTrue();
    }

    @Test
    void postCommitVoteInfluence() {
        setupPendingVote();
        postCommand("DECLARE_VOTE_STANCE", Map.of("actorPlayerId", "middle_class", "policyId", "POLICY_3_TAXATION", "stance", "FOR"));
        postCommand("DECLARE_VOTE_STANCE", Map.of("actorPlayerId", "capitalist", "policyId", "POLICY_3_TAXATION", "stance", "AGAINST"));
        postCommand("DECLARE_VOTE_STANCE", Map.of("actorPlayerId", "state", "policyId", "POLICY_3_TAXATION", "stance", "FOR"));

        ResponseEntity<JsonNode> response = postCommand("COMMIT_VOTE_INFLUENCE", Map.of(
                "actorPlayerId", "worker",
                "influenceAmount", 0
        ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("accepted").asBoolean()).isTrue();
    }

    @Test
    void getStateShowsVotingSession() {
        setupPendingVote();

        ResponseEntity<JsonNode> response = restTemplate.getForEntity(baseUrl, JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        JsonNode vote = response.getBody().path("gameState").path("currentVoteState");
        assertThat(vote.isObject()).isTrue();
        assertThat(vote.path("activeProposalPolicyId").asText()).isEqualTo("POLICY_3_TAXATION");
        assertThat(vote.path("proposalAuthorPlayerId").asText()).isEqualTo("worker");
    }

    @Test
    void fullVoteLifecycle_smoke() {
        setupPendingVote();

        postCommand("DECLARE_VOTE_STANCE", Map.of("actorPlayerId", "middle_class", "policyId", "POLICY_3_TAXATION", "stance", "FOR"));
        postCommand("DECLARE_VOTE_STANCE", Map.of("actorPlayerId", "capitalist", "policyId", "POLICY_3_TAXATION", "stance", "AGAINST"));
        postCommand("DECLARE_VOTE_STANCE", Map.of("actorPlayerId", "state", "policyId", "POLICY_3_TAXATION", "stance", "FOR"));

        postCommand("COMMIT_VOTE_INFLUENCE", Map.of("actorPlayerId", "worker", "influenceAmount", 0));
        postCommand("COMMIT_VOTE_INFLUENCE", Map.of("actorPlayerId", "middle_class", "influenceAmount", 0));
        postCommand("COMMIT_VOTE_INFLUENCE", Map.of("actorPlayerId", "capitalist", "influenceAmount", 0));
        ResponseEntity<JsonNode> finalCommit = postCommand("COMMIT_VOTE_INFLUENCE", Map.of("actorPlayerId", "state", "influenceAmount", 0));

        assertThat(finalCommit.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(finalCommit.getBody()).isNotNull();
        assertThat(finalCommit.getBody().path("accepted").asBoolean()).isTrue();
        assertThat(finalCommit.getBody().path("gameState").path("currentVoteState").isNull()).isTrue();
        assertThat(finalCommit.getBody().path("gameState").path("turnOrder").path("phase").asText()).isEqualTo("SCORING");
        assertThat(finalCommit.getBody().path("gameState").path("lastProposalResolution").path("policyId").asText())
                .isEqualTo("POLICY_3_TAXATION");
    }

    @Test
    void resolveScoringEndpointWorks() {
        driveToScoringWithoutPendingVotes();

        ResponseEntity<JsonNode> resolve = postCommand("RESOLVE_SCORING_PHASE", Map.of("actorPlayerId", "worker"));
        assertThat(resolve.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resolve.getBody()).isNotNull();
        assertThat(resolve.getBody().path("accepted").asBoolean()).isTrue();
        assertThat(resolve.getBody().path("gameState").path("lastScoringSummary").path("resolved").asBoolean()).isTrue();
    }

    @Test
    void advanceRoundEndpointWorks() {
        driveToScoringWithoutPendingVotes();
        postCommand("RESOLVE_SCORING_PHASE", Map.of("actorPlayerId", "worker"));

        ResponseEntity<JsonNode> advance = postCommand("ADVANCE_TO_NEXT_ROUND", Map.of("actorPlayerId", "worker"));
        assertThat(advance.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(advance.getBody()).isNotNull();
        assertThat(advance.getBody().path("accepted").asBoolean()).isTrue();
        assertThat(advance.getBody().path("gameState").path("currentRound").asInt()).isEqualTo(2);
    }

    @Test
    void postBuyGoodsAndServicesWorks() {
        ResponseEntity<JsonNode> response = postCommand("BUY_GOODS_AND_SERVICES", Map.of(
                "actorPlayerId", "worker",
                "resourceType", "FOOD",
                "purchases", List.of(
                        Map.of("supplierType", "CAPITALIST", "supplierPlayerId", "capitalist", "quantity", 1)
                )
        ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("accepted").asBoolean()).isTrue();
    }

    @Test
    void postConsumeHealthcareWorks() {
        seedWorkerForConsumption("healthcare");
        ResponseEntity<JsonNode> response = postCommand("CONSUME_HEALTHCARE", Map.of("actorPlayerId", "worker"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("accepted").asBoolean()).isTrue();
    }

    @Test
    void postConsumeEducationWorks() {
        seedWorkerForConsumption("education");
        ResponseEntity<JsonNode> response = postCommand("CONSUME_EDUCATION", Map.of("actorPlayerId", "worker"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("accepted").asBoolean()).isTrue();
    }

    @Test
    void postConsumeLuxuryWorks() {
        seedWorkerForConsumption("luxury");
        ResponseEntity<JsonNode> response = postCommand("CONSUME_LUXURY", Map.of("actorPlayerId", "worker"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("accepted").asBoolean()).isTrue();
    }

    @Test
    void getStateShowsBusinessDealAndExportCardSetup() {
        JsonNode state = restTemplate.getForEntity(baseUrl, JsonNode.class).getBody().path("gameState");
        assertThat(state.path("businessDealDeck").path("visibleCardIds").isArray()).isTrue();
        assertThat(state.path("businessDealDeck").path("visibleCardIds").size()).isEqualTo(1);
        assertThat(state.path("activeExportCard").path("cardId").asText()).startsWith("export-card-");
        assertThat(state.path("activeExportCard").path("offers").isArray()).isTrue();
        assertThat(state.path("activeExportCard").path("offers").size()).isEqualTo(8);
    }

    @Test
    void getStateShowsUpdatedResourcesAndWelfare() {
        seedWorkerForConsumption("healthcare");
        JsonNode before = restTemplate.getForEntity(baseUrl, JsonNode.class).getBody().path("gameState");
        JsonNode workerBefore = findPlayerNode(before, "worker");
        int welfareBefore = workerBefore.path("welfare").asInt();
        postCommand("CONSUME_HEALTHCARE", Map.of("actorPlayerId", "worker"));

        JsonNode after = restTemplate.getForEntity(baseUrl, JsonNode.class).getBody().path("gameState");
        JsonNode worker = findPlayerNode(after, "worker");
        assertThat(worker.path("welfare").asInt()).isEqualTo(welfareBefore + 1);
        assertThat(worker.path("goodsAndServicesArea").isObject()).isTrue();
        assertThat(after.path("publicServicesStorage").isObject()).isTrue();
    }

    @Test
    void finishedGameEndpointStateIsStableAfterReload() {
        reachGameOverWithoutVotes();

        ResponseEntity<JsonNode> save = restTemplate.postForEntity(baseUrl + "/save", Map.of("fileName", "api-finished-state.json"), JsonNode.class);
        assertThat(save.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<JsonNode> load = restTemplate.postForEntity(baseUrl + "/load", Map.of("fileName", "api-finished-state.json"), JsonNode.class);
        assertThat(load.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(load.getBody()).isNotNull();
        assertThat(load.getBody().path("gameState").path("gameStatus").asText()).isEqualTo("FINISHED");
        assertThat(load.getBody().path("gameState").path("currentPhase").asText()).isEqualTo("GAME_OVER");
        assertThat(load.getBody().path("gameState").path("finalResult").isObject()).isTrue();
    }

    private void setupPendingVote() {
        ResponseEntity<JsonNode> propose = postCommand("PROPOSE_BILL", Map.of(
                "actorPlayerId", "worker",
                "policyId", "POLICY_3_TAXATION",
                "targetCourse", "B"
        ));
        assertThat(propose.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(propose.getBody()).isNotNull();
        assertThat(propose.getBody().path("accepted").asBoolean()).isTrue();

        completeActionsPhase();
        ResponseEntity<JsonNode> resolveProduction = postCommand("RESOLVE_PRODUCTION_PHASE", Map.of("actorPlayerId", "worker"));
        assertThat(resolveProduction.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resolveProduction.getBody()).isNotNull();
        assertThat(resolveProduction.getBody().path("accepted").asBoolean()).isTrue();

        ResponseEntity<JsonNode> advance = postCommand("ADVANCE_TO_VOTING", Map.of("actorPlayerId", "worker"));
        assertThat(advance.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(advance.getBody()).isNotNull();
        assertThat(advance.getBody().path("accepted").asBoolean()).isTrue();
    }

    private void driveToScoringWithoutPendingVotes() {
        completeActionsPhase();
        ResponseEntity<JsonNode> resolveProduction = postCommand("RESOLVE_PRODUCTION_PHASE", Map.of("actorPlayerId", "worker"));
        assertThat(resolveProduction.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resolveProduction.getBody()).isNotNull();
        assertThat(resolveProduction.getBody().path("accepted").asBoolean()).isTrue();

        ResponseEntity<JsonNode> toVoting = postCommand("ADVANCE_TO_VOTING", Map.of("actorPlayerId", "worker"));
        assertThat(toVoting.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(toVoting.getBody()).isNotNull();
        assertThat(toVoting.getBody().path("accepted").asBoolean()).isTrue();
        assertThat(toVoting.getBody().path("gameState").path("currentPhase").asText()).isEqualTo("SCORING");
    }

    private void reachGameOverWithoutVotes() {
        int guard = 0;
        while (guard++ < 40) {
            JsonNode gameState = restTemplate.getForEntity(baseUrl, JsonNode.class).getBody().path("gameState");
            String phase = gameState.path("currentPhase").asText();
            int round = gameState.path("currentRound").asInt();
            if ("GAME_OVER".equals(phase)) {
                return;
            }

            if ("PREPARATION".equals(phase)) {
                postCommand("RESOLVE_PREPARATION_PHASE", Map.of("actorPlayerId", "worker"));
                continue;
            }
            if ("ACTIONS".equals(phase)) {
                completeActionsPhase();
                continue;
            }
            if ("PRODUCTION".equals(phase)) {
                postCommand("RESOLVE_PRODUCTION_PHASE", Map.of("actorPlayerId", "worker"));
                postCommand("ADVANCE_TO_VOTING", Map.of("actorPlayerId", "worker"));
                continue;
            }
            if ("SCORING".equals(phase)) {
                postCommand("RESOLVE_SCORING_PHASE", Map.of("actorPlayerId", "worker"));
                if (round < 5) {
                    postCommand("ADVANCE_TO_NEXT_ROUND", Map.of("actorPlayerId", "worker"));
                }
            }
        }
        throw new IllegalStateException("Did not reach GAME_OVER within guard limit.");
    }

    private void completeActionsPhase() {
        JsonNode gameState = restTemplate.getForEntity(baseUrl, JsonNode.class).getBody().path("gameState");
        int playerCount = gameState.path("turnOrder").path("activeClasses").size();
        for (int i = 0; i < playerCount * 5; i++) {
            ResponseEntity<JsonNode> endTurn = postCommand("END_TURN", Map.of("actorPlayerId", "worker"));
            assertThat(endTurn.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(endTurn.getBody()).isNotNull();
            assertThat(endTurn.getBody().path("accepted").asBoolean()).isTrue();
            if (!"ACTIONS".equals(endTurn.getBody().path("gameState").path("currentPhase").asText())) {
                break;
            }
        }
    }

    private ResponseEntity<JsonNode> postCommand(String actionType, Map<String, Object> parameters) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("actionType", actionType);
        payload.put("parameters", parameters);
        return restTemplate.postForEntity(baseUrl + "/command", payload, JsonNode.class);
    }

    private JsonNode findPlayerNode(JsonNode gameState, String playerId) {
        for (JsonNode player : gameState.path("players")) {
            if (playerId.equals(player.path("playerId").asText())) {
                return player;
            }
        }
        throw new IllegalStateException("Player not found in API response: " + playerId);
    }

    private void seedWorkerForConsumption(String resourceId) {
        var state = gameStateRepository.get().copy();
        var worker = state.findPlayerById("worker").orElseThrow();
        worker.setPopulation(1);
        worker.setGoodsAmount(resourceId, 1);
        worker.setMoney(Math.max(worker.getMoney(), 100));
        gameStateRepository.save(state);
    }

    private String seedAssignableWorkerEnterprise(int slotCount) {
        var state = gameStateRepository.get().copy();
        String enterpriseId = "api-test-empty-enterprise";
        Enterprise enterprise = new Enterprise();
        enterprise.setId(enterpriseId);
        enterprise.setName("API Test Empty Enterprise");
        enterprise.setCategory("test");
        enterprise.setOwnerClass(ClassType.CAPITALIST);
        enterprise.setProductionAmount(slotCount);
        enterprise.setProducedResources(Map.of("food", slotCount));
        for (int i = 1; i <= slotCount; i++) {
            enterprise.getSlots().add(new EnterpriseSlot(
                    enterpriseId + "-slot-" + i,
                    WorkerQualification.UNSKILLED,
                    null,
                    null
            ));
        }
        state.getEnterprises().add(enterprise);
        gameStateRepository.save(state);
        return enterpriseId;
    }
}
