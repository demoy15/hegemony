package com.example.hegemony.domain;

import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaActionCard;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaActionSymbol;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaBonus;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaExecutionMode;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaInstructionCard;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaInstructionMode;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaInstructionType;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaInterpreter;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaInterpretationResult;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaPolicyTag;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaSpecialAction;
import com.example.hegemony.domain.command.AdvanceGameFlowCommand;
import com.example.hegemony.domain.command.AssignWorkersCommand;
import com.example.hegemony.domain.command.GameCommand;
import com.example.hegemony.domain.command.ProposeBillCommand;
import com.example.hegemony.domain.model.PolicyCourse;
import com.example.hegemony.domain.model.PolicyId;
import com.example.hegemony.infrastructure.carddata.YamlCapitalistAutomaCardRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CapitalistAutomaCardEngineTest {
    @Test
    void yamlRegistryLoadsPartialDeckWithoutCrash() {
        YamlCapitalistAutomaCardRegistry registry = new YamlCapitalistAutomaCardRegistry(
                "../specs/automa/capitalists/action-cards.yaml",
                "../specs/automa/capitalists/instruction-cards.yaml"
        );

        assertThat(registry.isLoaded()).isTrue();
        assertThat(registry.datasetStatus()).contains("READY_PARTIAL_DECK");
        assertThat(registry.actionCards()).hasSize(20);
        assertThat(registry.instructionCards()).isNotEmpty();
        assertThat(registry.findActionCard(1)).isPresent();
        assertThat(registry.findActionCard(1).orElseThrow().policyTags())
                .contains(CapitalistAutomaPolicyTag.POLICY_FISCAL);
    }

    @Test
    void interpreterChoosesFirstLegalMappedCheck() {
        CapitalistAutomaActionCard card = new CapitalistAutomaActionCard(
                13,
                List.of(CapitalistAutomaActionSymbol.BUILD_ENTERPRISE, CapitalistAutomaActionSymbol.PROPOSE_BILL),
                List.of(CapitalistAutomaPolicyTag.POLICY_TAX),
                new CapitalistAutomaBonus("built_enterprise_this_turn", "money_discount", Map.of("amount", 10), "bonus"),
                new CapitalistAutomaSpecialAction("SPECIAL_FALLBACK", Map.of(), List.of(), "special"),
                "raw",
                "",
                "medium"
        );
        List<CapitalistAutomaInstructionCard> instructions = List.of(new CapitalistAutomaInstructionCard(
                "capitalist_condition_propose_bill",
                CapitalistAutomaInstructionType.CONDITION_ACTION,
                CapitalistAutomaActionSymbol.PROPOSE_BILL,
                CapitalistAutomaInstructionMode.BOTH,
                "raw",
                List.of(Map.of("kind", "simple_mode_precondition")),
                List.of()
        ));
        List<GameCommand> legalCommands = List.of(
                new AssignWorkersCommand("capitalist", List.of()),
                new ProposeBillCommand("capitalist", PolicyId.POLICY_3_TAXATION, PolicyCourse.B)
        );

        CapitalistAutomaInterpreter interpreter = new CapitalistAutomaInterpreter();
        CapitalistAutomaInterpretationResult result = interpreter.interpret(
                CoreTestSupport.state(2),
                CapitalistAutomaExecutionMode.SIMPLE,
                card,
                instructions,
                legalCommands
        );

        assertThat(result.selectedCommand().type().name()).isEqualTo("PROPOSE_BILL");
        assertThat(result.cardDrivenSelection()).isTrue();
        assertThat(result.trace()).containsEntry("currentCardNo", 13);
        assertThat(((List<?>) result.trace().get("checksTrace")).size()).isEqualTo(2);
    }

    @Test
    void interpreterUsesSpecialFallbackWhenChecksUnsupported() {
        CapitalistAutomaActionCard card = new CapitalistAutomaActionCard(
                22,
                List.of(CapitalistAutomaActionSymbol.BUILD_ENTERPRISE, CapitalistAutomaActionSymbol.SELL_ON_EXTERNAL_MARKET),
                List.of(CapitalistAutomaPolicyTag.POLICY_FOREIGN_TRADE),
                null,
                new CapitalistAutomaSpecialAction("SPECIAL_FALLBACK", Map.of(), List.of(), "special"),
                "raw",
                "",
                "low"
        );
        List<GameCommand> legalCommands = List.of(new AdvanceGameFlowCommand("capitalist"));

        CapitalistAutomaInterpreter interpreter = new CapitalistAutomaInterpreter();
        CapitalistAutomaInterpretationResult result = interpreter.interpret(
                CoreTestSupport.state(2),
                CapitalistAutomaExecutionMode.COMPLEX,
                card,
                List.of(),
                legalCommands
        );

        assertThat(result.selectedCommand().type().name()).isEqualTo("ADVANCE_GAME_FLOW");
        assertThat(result.usedSpecialActionFallback()).isTrue();
        assertThat(result.trace()).containsEntry("specialActionType", "SPECIAL_FALLBACK");
    }
}
