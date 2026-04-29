package com.example.hegemony.bot.planning;

import com.example.hegemony.domain.command.GameCommand;
import com.example.hegemony.domain.model.ClassType;
import com.example.hegemony.domain.model.GameState;

import java.util.List;
import java.util.Optional;

public interface ClassBotPlanner {
    ClassType supportedClass();

    Optional<PlannedBotMove> plan(GameState state, List<GameCommand> legalCommands);
}
