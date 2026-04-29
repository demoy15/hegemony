package com.example.hegemony.domain.command;

import com.example.hegemony.domain.model.ActionType;

public interface GameCommand {
    ActionType type();

    String moveId();

    String summary();
}
