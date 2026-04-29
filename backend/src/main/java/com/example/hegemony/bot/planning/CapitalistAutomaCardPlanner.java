package com.example.hegemony.bot.planning;

import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaActionCard;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaCardRegistry;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaExecutionMode;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaInterpretationResult;
import com.example.hegemony.domain.automa.capitalist.CapitalistAutomaInterpreter;
import com.example.hegemony.domain.command.GameCommand;
import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.PlayerState;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class CapitalistAutomaCardPlanner implements BotCardPlanner {
    private final CapitalistAutomaCardRegistry cardRegistry;
    private final CapitalistAutomaInterpreter interpreter = new CapitalistAutomaInterpreter();

    public CapitalistAutomaCardPlanner(CapitalistAutomaCardRegistry cardRegistry) {
        this.cardRegistry = cardRegistry;
    }

    @Override
    public boolean isReady(ClassType classType) {
        return classType == ClassType.CAPITALIST && cardRegistry.isLoaded() && !cardRegistry.actionCards().isEmpty();
    }

    @Override
    public Optional<PlannedBotMove> plan(GameState state, ClassType classType, List<GameCommand> legalCommands) {
        if (classType != ClassType.CAPITALIST || legalCommands == null || legalCommands.isEmpty() || !isReady(classType)) {
            return Optional.empty();
        }

        PlayerState actor = state.currentPlayer();
        if (actor == null || actor.getClassType() != ClassType.CAPITALIST) {
            return Optional.empty();
        }

        List<CapitalistAutomaActionCard> actionCards = cardRegistry.actionCards();
        int cardIndex = Math.floorMod(state.getEventLog().size() + state.getCurrentRound(), actionCards.size());
        CapitalistAutomaActionCard currentCard = actionCards.get(cardIndex);
        CapitalistAutomaExecutionMode mode = CapitalistAutomaExecutionMode.fromStrategy(actor.getBotStrategyMode());

        CapitalistAutomaInterpretationResult interpretation = interpreter.interpret(
                state,
                mode,
                currentCard,
                cardRegistry.instructionCards(),
                legalCommands
        );

        ActionPlan plan = new ActionPlan(
                interpretation.selectedCommand(),
                interpretation.cardDrivenSelection() ? ActionPlanSupportStatus.SUPPORTED : ActionPlanSupportStatus.FALLBACK_HEURISTIC,
                interpretation.cardDrivenSelection()
                        ? "DATA_DRIVEN_AUTOMA_CARD_ENGINE"
                        : "DATA_DRIVEN_AUTOMA_FALLBACK: no interpreted check produced a legal command."
        );
        plan.setOptionalCardReference("capitalist-" + currentCard.cardNo());
        if (interpretation.bonusApplied() && currentCard.bonus() != null) {
            plan.setOptionalModifier(currentCard.bonus().effectType());
        }

        Map<String, Object> debugTrace = new HashMap<>(interpretation.trace());
        debugTrace.put("automa", "CAPITALISTS");
        debugTrace.put("datasetStatus", cardRegistry.datasetStatus());
        debugTrace.put("cardReference", "capitalist-" + currentCard.cardNo());
        debugTrace.put("specialActionUsed", interpretation.usedSpecialActionFallback());
        debugTrace.put("selectedMoveId", interpretation.selectedCommand().moveId());
        debugTrace.put("selectedActionType", interpretation.selectedCommand().type().name());
        debugTrace.put("legalActionTypes", legalCommands.stream().map(command -> command.type().name()).toList());

        String plannerId = "capitalist-automa-card-engine";
        boolean fallback = !interpretation.cardDrivenSelection();
        String rationale = interpretation.rationale();
        if (fallback) {
            rationale = rationale + " Fallback respects legal-only command boundary.";
        }

        return Optional.of(new PlannedBotMove(
                plan,
                plannerId,
                rationale,
                fallback,
                legalCommands.size(),
                debugTrace
        ));
    }
}
