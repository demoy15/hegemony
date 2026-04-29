package com.example.hegemony.infrastructure;

import com.example.hegemony.application.GameStateStorage;
import com.example.hegemony.domain.model.GameState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class JsonGameStateStorage implements GameStateStorage {
    private final ObjectMapper objectMapper;
    private final Path saveDirectory;

    public JsonGameStateStorage(
            ObjectMapper objectMapper,
            @Value("${hegemony.save-dir:./data/saves}") String saveDirectory
    ) {
        this.objectMapper = objectMapper;
        this.saveDirectory = Path.of(saveDirectory).toAbsolutePath().normalize();
    }

    @Override
    public String save(GameState state, String fileName) {
        ensureSaveDirectory();
        String safeFileName = sanitize(fileName);
        Path target = saveDirectory.resolve(safeFileName).normalize();
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), state);
            return target.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to save game state JSON", ex);
        }
    }

    @Override
    public GameState load(String fileName) {
        ensureSaveDirectory();
        Path target = saveDirectory.resolve(sanitize(fileName)).normalize();
        if (!Files.exists(target)) {
            throw new IllegalStateException("Save file not found: " + target);
        }
        try {
            return objectMapper.readValue(target.toFile(), GameState.class);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load game state JSON", ex);
        }
    }

    private void ensureSaveDirectory() {
        try {
            Files.createDirectories(saveDirectory);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create save directory " + saveDirectory, ex);
        }
    }

    private String sanitize(String fileName) {
        String trimmed = fileName.trim();
        if (!trimmed.endsWith(".json")) {
            return trimmed + ".json";
        }
        return trimmed;
    }
}
