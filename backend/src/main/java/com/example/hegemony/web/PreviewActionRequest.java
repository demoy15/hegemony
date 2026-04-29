package com.example.hegemony.web;

import com.example.hegemony.domain.model.ActionType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class PreviewActionRequest {
    @NotNull
    private ActionType actionType;
    private Map<String, Object> parameters = new HashMap<>();
    private String optionalModifier;
    private String optionalCardReference;

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters == null ? new HashMap<>() : parameters;
    }
}
