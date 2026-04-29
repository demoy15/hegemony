package com.example.hegemony.web;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class ResetGameRequest {
    private Integer playerCount = 4;
    private Map<String, String> controlModes = new HashMap<>();
    private Map<String, String> botStrategyModes = new HashMap<>();

    public void setControlModes(Map<String, String> controlModes) {
        this.controlModes = controlModes == null ? new HashMap<>() : new HashMap<>(controlModes);
    }

    public void setBotStrategyModes(Map<String, String> botStrategyModes) {
        this.botStrategyModes = botStrategyModes == null ? new HashMap<>() : new HashMap<>(botStrategyModes);
    }
}
