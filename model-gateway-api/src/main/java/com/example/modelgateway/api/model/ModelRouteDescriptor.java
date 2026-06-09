package com.example.modelgateway.api.model;

import com.example.modelgateway.api.enums.ModelCapability;
import java.util.List;
import java.util.Map;

public record ModelRouteDescriptor(
        String id,
        String provider,
        String model,
        List<ModelCapability> capabilities,
        List<String> scenarios,
        int priority,
        boolean enabled,
        List<String> fallbackRouteIds,
        Map<String, Object> metadata
) {
}
