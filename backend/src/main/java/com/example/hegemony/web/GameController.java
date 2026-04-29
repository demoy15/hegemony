package com.example.hegemony.web;

import com.example.hegemony.application.BotMoveExecutionResult;
import com.example.hegemony.application.BotTurnExecutionResult;
import com.example.hegemony.application.BotUntilHumanExecutionResult;
import com.example.hegemony.application.CommandExecutionResult;
import com.example.hegemony.application.GameService;
import com.example.hegemony.application.composer.ActionPreviewResult;
import com.example.hegemony.domain.event.DomainEvent;
import com.example.hegemony.domain.model.GameState;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/game")
public class GameController {
    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    public GameResponse game() {
        return new GameResponse(
                gameService.getCurrentGame(),
                gameService.getLegalMoves(),
                gameService.getComposerMetadata(),
                gameService.getCurrentMarketCandidates()
        );
    }

    @PostMapping("/reset")
    public GameResponse reset(@RequestBody(required = false) ResetGameRequest request) {
        int playerCount = request == null || request.getPlayerCount() == null ? 4 : request.getPlayerCount();
        GameState state = gameService.resetGame(
                playerCount,
                request == null ? Map.of() : request.getControlModes(),
                request == null ? Map.of() : request.getBotStrategyModes()
        );
        return new GameResponse(state, gameService.getLegalMoves(), gameService.getComposerMetadata(), gameService.getCurrentMarketCandidates());
    }

    @PostMapping("/setup")
    public GameResponse setup(@RequestBody @Valid ResetGameRequest request) {
        int playerCount = request.getPlayerCount() == null ? 4 : request.getPlayerCount();
        GameState state = gameService.resetGame(playerCount, request.getControlModes(), request.getBotStrategyModes());
        return new GameResponse(state, gameService.getLegalMoves(), gameService.getComposerMetadata(), gameService.getCurrentMarketCandidates());
    }

    @PostMapping("/command")
    public CommandResponse command(@Valid @RequestBody CommandRequest request) {
        CommandExecutionResult result = gameService.executeCommand(request.getActionType(), request.getParameters());
        return new CommandResponse(result.accepted(), result.errors(), result.reasonCodes(), toEventViews(result.events()), result.gameState());
    }

    @GetMapping("/legal-moves")
    public Map<String, Object> legalMoves() {
        return Map.of("legalMoves", gameService.getLegalMoves());
    }

    @PostMapping("/bot-move")
    public BotMoveResponse botMove() {
        BotMoveExecutionResult result = gameService.executeBotMove();
        return new BotMoveResponse(
                result.selectedMoveId(),
                result.actionType(),
                result.explanation(),
                result.legalActionCount(),
                toEventViews(result.events()),
                result.gameState()
        );
    }

    @PostMapping("/play-bot-turn")
    public BotTurnResponse playBotTurn() {
        BotTurnExecutionResult result = gameService.playBotTurn();
        return new BotTurnResponse(result.summary(), toEventViews(result.events()), result.gameState());
    }

    @PostMapping("/play-bot-until-human")
    public BotUntilHumanResponse playBotUntilHuman() {
        BotUntilHumanExecutionResult result = gameService.playBotUntilHuman();
        return new BotUntilHumanResponse(
                result.turnSummaries(),
                result.executedSteps(),
                result.stoppedAtHumanDecisionPoint(),
                result.gameOver(),
                result.gameState()
        );
    }

    @PostMapping("/preview")
    public PreviewActionResponse preview(@Valid @RequestBody PreviewActionRequest request) {
        ActionPreviewResult result = gameService.previewAction(
                request.getActionType(),
                request.getParameters(),
                request.getOptionalModifier(),
                request.getOptionalCardReference()
        );
        return new PreviewActionResponse(
                result.accepted(),
                result.errors(),
                result.reasonCodes(),
                result.delta(),
                result.supportNotes()
        );
    }

    @PostMapping("/save")
    public SaveLoadResponse save(@RequestBody(required = false) SaveLoadRequest request) {
        String filePath = gameService.saveToJson(request == null ? "demo-save.json" : request.getFileName());
        return new SaveLoadResponse(filePath, gameService.getCurrentGame());
    }

    @PostMapping("/load")
    public SaveLoadResponse load(@RequestBody(required = false) SaveLoadRequest request) {
        String fileName = request == null ? "demo-save.json" : request.getFileName();
        GameState loaded = gameService.loadFromJson(fileName);
        return new SaveLoadResponse(fileName, loaded);
    }

    private List<EventView> toEventViews(List<DomainEvent> events) {
        return events.stream().map(EventView::from).toList();
    }
}
