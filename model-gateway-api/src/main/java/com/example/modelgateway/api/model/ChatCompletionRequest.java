package com.example.modelgateway.api.model;

import com.example.modelgateway.api.enums.ModelCapability;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

public record ChatCompletionRequest(
        String tenantId,
        String projectId,
        String scenario,
        ModelCapability capability,
        String modelHint,
        @NotEmpty List<@Valid ChatMessage> messages,
        Map<String, Object> parameters,
        Boolean stream,
        Map<String, Object> metadata
) {
    public ModelCapability resolvedCapability() {
        return capability == null ? ModelCapability.TEXT_GENERATION : capability;
    }

    public boolean isStream() {
        return Boolean.TRUE.equals(stream);
    }

    public Map<String, Object> safeParameters() {
        return parameters == null ? Map.of() : parameters;
    }

    public Map<String, Object> safeMetadata() {
        return metadata == null ? Map.of() : metadata;
    }
}
