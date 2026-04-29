package com.example.hegemony.bot.planning;

import com.example.hegemony.domain.command.GameCommand;
import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.GameState;

import java.util.List;
import java.util.Optional;

public interface BotCardPlanner {
    boolean isReady(ClassType classType);

    Optional<PlannedBotMove> plan(GameState state, ClassType classType, List<GameCommand> legalCommands);
}
