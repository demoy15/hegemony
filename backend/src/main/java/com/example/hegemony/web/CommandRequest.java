package com.example.hegemony.web;

import com.example.hegemony.domain.model.ActionType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class CommandRequest {
    @NotNull
    private ActionType actionType;
    private Map<String, Object> parameters = new HashMap<>();

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters == null ? new HashMap<>() : parameters;
    }
}
