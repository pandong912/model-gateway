package com.example.modelgateway.core.model;

import com.example.modelgateway.api.enums.ModelCapability;
import com.example.modelgateway.api.model.ModelRouteDescriptor;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public record ModelRoute(
        String id,
        String provider,
        String model,
        List<ModelCapability> capabilities,
        List<String> scenarios,
        int priority,
        boolean enabled,
        List<String> fallbackRouteIds,
        Duration timeout,
        Map<String, Object> metadata
) {
    public ModelRouteDescriptor toDescriptor() {
        return new ModelRouteDescriptor(id, provider, model, capabilities, scenarios, priority, enabled, fallbackRouteIds, metadata);
    }
}
