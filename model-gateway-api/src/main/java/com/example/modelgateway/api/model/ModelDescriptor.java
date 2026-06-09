package com.example.modelgateway.api.model;

import com.example.modelgateway.api.enums.ModelCapability;
import java.util.List;
import java.util.Map;

public record ModelDescriptor(
        String id,
        String provider,
        String model,
        List<ModelCapability> capabilities,
        boolean streaming,
        boolean enabled,
        Map<String, Object> metadata
) {
}
