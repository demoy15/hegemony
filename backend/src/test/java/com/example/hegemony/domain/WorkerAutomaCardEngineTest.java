package com.example.hegemony.domain;

import com.example.hegemony.bot.planning.WorkerAutomaCardPlanner;
import com.example.hegemony.domain.command.AssignWorkersCommand;
import com.example.hegemony.domain.command.GameCommand;
import com.example.hegemony.domain.command.ProposeBillCommand;
import com.example.hegemony.domain.model.BotStrategyMode;
import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.PlayerState;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.infrastructure.carddata.YamlWorkerAutomaCardRegistry;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WorkerAutomaCardEngineTest {
    @Test
    void yamlRegistryLoadsWorkerSimpleDeckAndInstructions() {
        YamlWorkerAutomaCardRegistry registry = new YamlWorkerAutomaCardRegistry(
                "../specs/automa/workers/action-cards.yaml",
                "../specs/automa/workers/instruction-cards.yaml"
        );

        assertThat(registry.isLoaded()).isTrue();
        assertThat(registry.datasetStatus()).isEqualTo("READY_WORKER_DECK");
        assertThat(registry.actionCards()).hasSize(30);
        assertThat(registry.instructionCards()).isNotEmpty();
        assertThat(registry.findActionCard(1)).isPresent();
        assertThat(registry.findActionCard(1).orElseThrow().checks())
                .extracting(Enum::name)
                .containsExactly("ASSIGN_WORKERS", "BUY_GOODS_AND_SERVICES", "PROPOSE_BILL", "PLACE_STRIKES");
    }

    @Test
    void plannerDrawsWorkerCardsFromStateDeckWithoutRepeatingUntilDeckExhausted() {
        YamlWorkerAutomaCardRegistry registry = new YamlWorkerAutomaCardRegistry(
                "../specs/automa/workers/action-cards.yaml",
                "../specs/automa/workers/instruction-cards.yaml"
        );
        WorkerAutomaCardPlanner planner = new WorkerAutomaCardPlanner(registry);
        GameState state = CoreTestSupport.state(2);
        PlayerState worker = state.currentPlayer();
        worker.setBotStrategyMode(BotStrategyMode.CARD_DRIVEN_SIMPLE_AUTOMA);

        List<GameCommand> legalCommands = List.of(
                new ProposeBillCommand(worker.getPlayerId(), PolicyId.POLICY_2_LABOR_MARKET, PolicyCourse.A),
                new AssignWorkersCommand(worker.getPlayerId(), List.of())
        );

        Set<String> drawn = new HashSet<>();
        for (int i = 0; i < registry.actionCards().size(); i++) {
            var planned = planner.plan(state, ClassType.WORKER, legalCommands);

            assertThat(planned).isPresent();
            String visibleCard = state.getWorkerAutomaDeck().getVisibleCardIds().getFirst();
            assertThat(drawn).doesNotContain(visibleCard);
            drawn.add(visibleCard);
        }

        assertThat(drawn).hasSize(30);
        assertThat(state.getWorkerAutomaDeck().getNextCardIndex()).isEqualTo(30);

        var afterExhaustion = planner.plan(state, ClassType.WORKER, legalCommands);

        assertThat(afterExhaustion).isPresent();
        assertThat(state.getWorkerAutomaDeck().getRefreshCount()).isEqualTo(2);
        assertThat(state.getWorkerAutomaDeck().getNextCardIndex()).isEqualTo(1);
    }
}
