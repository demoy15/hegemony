package com.example.hegemony.application.composer;

import java.util.List;

public record ComposerMetadata(
        String actorPlayerId,
        List<ComposerActionTemplate> actionTemplates,
        boolean modifierAvailable,
        String modifierAvailabilityNote,
        List<String> unavailableActionNotes
) {
}
