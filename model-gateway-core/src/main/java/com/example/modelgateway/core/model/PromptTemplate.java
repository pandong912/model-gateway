package com.example.modelgateway.core.model;

import com.example.modelgateway.api.enums.MessageRole;
import java.util.Map;

public record PromptTemplate(
        String key,
        String version,
        String scenario,
        String locale,
        MessageRole role,
        String content,
        boolean enabled,
        boolean defaultForScenario,
        Map<String, Object> metadata
) {
}
