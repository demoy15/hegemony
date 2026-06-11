package com.example.hegemony.bot.planning;

import com.example.hegemony.domain.command.GameCommand;
import com.example.hegemony.domain.model.ActionType;
import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.GameState;
import com.example.hegemony.domain.model.PlayerState;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class CapitalistBotPlanner implements ClassBotPlanner {
    private final MarketCandidateProvider marketCandidateProvider;

    public CapitalistBotPlanner(MarketCandidateProvider marketCandidateProvider) {
        this.marketCandidateProvider = marketCandidateProvider;
    }

    @Override
    public ClassType supportedClass() {
        return ClassType.CAPITALIST;
    }

    @Override
    public Optional<PlannedBotMove> plan(GameState state, List<GameCommand> legalCommands) {
        if (legalCommands == null || legalCommands.isEmpty()) {
            return Optional.empty();
        }

        PlayerState actor = state.currentPlayer();
        if (actor == null || actor.getClassType() != ClassType.CAPITALIST) {
            return Optional.empty();
        }

        List<MarketCandidate> marketCandidates = marketCandidateProvider.listCandidates(state, ClassType.CAPITALIST);

        GameCommand selected = legalCommands.stream()
                .max(Comparator.<GameCommand>comparingInt(command -> score(actor, command, marketCandidates))
                        .thenComparing(GameCommand::moveId))
                .orElse(null);

        if (selected == null) {
            return Optional.empty();
        }

        ActionPlan plan = new ActionPlan(selected, ActionPlanSupportStatus.FALLBACK_HEURISTIC,
                "CARD_DATA_NOT_INSTALLED: capitalist planner used fallback_market_view and legal move heuristics.");

        String rationale = "Capitalist fallback planner prioritized legal action '" + selected.type()
                + "' using current money/resources/policies and " + marketCandidates.size()
                + " fallback market candidates.";

        return Optional.of(new PlannedBotMove(
                plan,
                "capitalist-fallback-planner",
                rationale,
                true,
                legalCommands.size(),
                Map.of(
                        "automa", "CAPITALISTS",
                        "mode", "HEURISTIC_FALLBACK",
                        "selectedAction", selected.type().name()
                )
        ));
    }

    private int score(PlayerState actor, GameCommand command, List<MarketCandidate> marketCandidates) {
        int base = switch (command.type()) {
            case BUY_GOODS_AND_SERVICES -> 260;
            case ASSIGN_WORKERS -> 240;
            case PROPOSE_BILL -> 220;
            case BUILD_ENTERPRISE,
                 SELL_ENTERPRISE,
                 SELL_ON_EXTERNAL_MARKET,
                 MAKE_BUSINESS_DEAL,
                 LOBBY_INTERESTS,
                 CHANGE_PRICES,
                 CHANGE_WAGES,
                 PAY_BONUS,
                 BUY_STORAGE,
                 TAKE_STATE_BENEFITS,
                 REPAY_LOAN,
                 RESPOND_TO_EVENT,
                 MEET_DEPUTIES,
                 INTRODUCE_EXTRA_TAX,
                 RUN_CAMPAIGN -> 210;
            case ADVANCE_GAME_FLOW -> 180;
            case ADVANCE_TO_VOTING -> 160;
            case ADVANCE_TO_PRODUCTION -> 150;
            case RESOLVE_PRODUCTION_PHASE -> 140;
            case ADVANCE_TO_SCORING -> 130;
            case RESOLVE_SCORING_PHASE -> 120;
            case ADVANCE_TO_NEXT_ROUND -> 110;
            case DECLARE_VOTE_STANCE -> 100;
            case DRAW_VOTING_CUBES -> 95;
            case COMMIT_VOTE_INFLUENCE -> 90;
            case ADD_VOTING_CUBES, CALL_EXTRAORDINARY_VOTE -> 85;
            case PLACE_STRIKES, PLACE_DEMONSTRATION -> 82;
            case CONSUME_HEALTHCARE, CONSUME_EDUCATION, CONSUME_LUXURY -> 80;
            case REFRESH_BUSINESS_DEALS -> 1;
            case RESOLVE_PREPARATION_PHASE, ADVANCE_ROUND -> 70;
            case START_TURN, HIRE_WORKER, PRODUCE_GOODS, SELL_GOODS, ADJUST_POLICY, PLAY_CARD, END_TURN -> 5;
        };

        if (command.type() == ActionType.BUY_GOODS_AND_SERVICES && actor.getMoney() > 40) {
            base += 15;
        }
        if (command.type() == ActionType.ASSIGN_WORKERS && marketCandidates.stream().anyMatch(candidate -> candidate.freeSlots() > 0)) {
            base += 12;
        }
        if (command.type() == ActionType.PROPOSE_BILL && actor.availableProposalTokens() > 0) {
            base += 8;
        }
        return base;
    }
}
