package com.example.modelgateway.api.model;

import java.util.Map;

public record ChatStreamEvent(
        String id,
        String type,
        String delta,
        TokenUsage usage,
        String provider,
        String model,
        String routeId,
        String traceId,
        Map<String, Object> metadata
) {
    public static ChatStreamEvent delta(String id, String delta, String provider, String model, String routeId, String traceId) {
        return new ChatStreamEvent(id, "delta", delta, null, provider, model, routeId, traceId, Map.of());
    }

    public static ChatStreamEvent done(String id, TokenUsage usage, String provider, String model, String routeId, String traceId) {
        return new ChatStreamEvent(id, "done", null, usage, provider, model, routeId, traceId, Map.of());
    }
}
