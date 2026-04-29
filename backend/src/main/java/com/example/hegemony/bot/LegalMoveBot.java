package com.example.hegemony.bot;

import com.example.hegemony.domain.command.GameCommand;
import com.example.hegemony.domain.engine.ApplyCommandResult;
import com.example.hegemony.domain.engine.GameRulesEngine;
import com.example.hegemony.domain.model.ActionType;
import com.example.hegemony.domain.model.GameState;

import java.util.Comparator;
import java.util.List;

public class LegalMoveBot {
    public BotDecision chooseMove(GameState state, GameRulesEngine engine) {
        List<GameCommand> legalMoves = engine.generateLegalCommands(state);
        if (legalMoves.isEmpty()) {
            throw new IllegalStateException("No legal moves available for bot.");
        }

        GameCommand selected = legalMoves.stream()
                .max(Comparator.comparingInt(this::score).thenComparing(GameCommand::moveId))
                .orElseThrow();

        ApplyCommandResult preview = engine.apply(state, selected);
        String explanation = explain(selected, legalMoves.size());
        return new BotDecision(selected, explanation, legalMoves.size(), preview.producedEvents());
    }

    private int score(GameCommand command) {
        return switch (command.type()) {
            case CONSUME_HEALTHCARE, CONSUME_EDUCATION, CONSUME_LUXURY -> 230;
            case BUY_GOODS_AND_SERVICES -> 220;
            case ADVANCE_GAME_FLOW -> 210;
            case RESOLVE_PREPARATION_PHASE -> 205;
            case RESOLVE_SCORING_PHASE -> 200;
            case ADVANCE_TO_NEXT_ROUND -> 195;
            case ADVANCE_TO_SCORING -> 190;
            case RESOLVE_PRODUCTION_PHASE -> 150;
            case ADVANCE_ROUND -> 145;
            case DECLARE_VOTE_STANCE -> 130;
            case COMMIT_VOTE_INFLUENCE -> 120;
            case ADVANCE_TO_PRODUCTION -> 115;
            case ADVANCE_TO_VOTING -> 110;
            case PROPOSE_BILL -> 100;
            case PLACE_STRIKES, PLACE_DEMONSTRATION -> 98;
            case ASSIGN_WORKERS -> 95;
            case ADD_VOTING_CUBES, CALL_EXTRAORDINARY_VOTE -> 90;
            case REFRESH_BUSINESS_DEALS -> 1;
            case START_TURN -> 30;
            case END_TURN -> 20;
            case HIRE_WORKER, PRODUCE_GOODS, SELL_GOODS, ADJUST_POLICY, PLAY_CARD -> 5;
        };
    }

    private String explain(GameCommand command, int legalCount) {
        ActionType action = command.type();
        String reason = switch (action) {
            case BUY_GOODS_AND_SERVICES -> "The bot uses a legal consumer-economy purchase to improve resource coverage for needs and welfare actions.";
            case CONSUME_HEALTHCARE -> "The bot executes legal healthcare consumption to increase welfare for the acting class.";
            case CONSUME_EDUCATION -> "The bot executes legal education consumption to increase welfare for the acting class.";
            case CONSUME_LUXURY -> "The bot executes legal luxury consumption to increase welfare for the acting class.";
            case ADVANCE_GAME_FLOW -> "The bot executes the next legal lifecycle command for current phase progression.";
            case RESOLVE_PREPARATION_PHASE -> "The bot resolves supported preparation steps to enter ACTIONS phase.";
            case RESOLVE_SCORING_PHASE -> "The bot resolves scoring and records supported VP breakdown for the round.";
            case ADVANCE_TO_NEXT_ROUND -> "The bot advances to the next round after scoring is complete.";
            case ADVANCE_TO_SCORING -> "The bot transitions from production to scoring once production is resolved.";
            case RESOLVE_PRODUCTION_PHASE -> "The bot resolves the production economy loop using a legal orchestrated command.";
            case ADVANCE_ROUND -> "The bot advances to the next round after legal production resolution.";
            case DECLARE_VOTE_STANCE -> "The bot submits a legal voting stance for the active proposal.";
            case COMMIT_VOTE_INFLUENCE -> "The bot commits legal influence to progress vote resolution.";
            case ADVANCE_TO_PRODUCTION -> "The bot transitions from voting to production phase.";
            case ADVANCE_TO_VOTING -> "The bot advances gameplay from ACTIONS to VOTING.";
            case PROPOSE_BILL -> "The bot pushes a legal adjacent policy proposal.";
            case PLACE_STRIKES -> "The bot applies legal worker-class strike pressure to eligible enterprises.";
            case PLACE_DEMONSTRATION -> "The bot applies legal worker-class demonstration pressure while unemployment surplus is high.";
            case ASSIGN_WORKERS -> "The bot executes a legal worker assignment without partial staffing.";
            case ADD_VOTING_CUBES -> "The bot adds voting cubes as a legal political action.";
            case CALL_EXTRAORDINARY_VOTE -> "The bot resolves a pending proposal with a legal extraordinary vote.";
            case REFRESH_BUSINESS_DEALS -> "The bot refreshes visible business deals from the ordered deck.";
            case START_TURN -> "The bot advances phase progression.";
            case END_TURN -> "No stronger legal action is available in this slice.";
            case HIRE_WORKER, PRODUCE_GOODS, SELL_GOODS, ADJUST_POLICY, PLAY_CARD -> "Legacy demo move selected as a fallback.";
        };
        return reason + " Selected from " + legalCount + " legal actions.";
    }
}
