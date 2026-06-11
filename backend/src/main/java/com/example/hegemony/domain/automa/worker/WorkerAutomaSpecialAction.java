package com.example.hegemony.domain.automa.worker;

import java.util.List;
import java.util.Map;

public record WorkerAutomaSpecialAction(
        String type,
        Map<String, Object> params,
        List<String> conditions,
        String rawTextRu
) {
}
