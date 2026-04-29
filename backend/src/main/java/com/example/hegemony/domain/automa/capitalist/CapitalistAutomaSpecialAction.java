package com.example.hegemony.domain.automa.capitalist;

import java.util.List;
import java.util.Map;

public record CapitalistAutomaSpecialAction(
        String type,
        Map<String, Object> params,
        List<String> conditions,
        String rawTextRu
) {
}
