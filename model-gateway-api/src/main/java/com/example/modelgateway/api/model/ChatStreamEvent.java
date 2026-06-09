package com.example.modelgateway.api.model;

import java.util.Map;

public record ChatStreamEvent(
        String id,
        String type,
        String delta,
        java.util.List<GeneratedMedia> mediaOutputs,
        TokenUsage usage,
        String provider,
        String model,
        String routeId,
        String traceId,
        Map<String, Object> metadata
) {
    public static ChatStreamEvent delta(String id, String delta, String provider, String model, String routeId, String traceId) {
        return new ChatStreamEvent(id, "delta", delta, java.util.List.of(), null, provider, model, routeId, traceId, Map.of());
    }

    public static ChatStreamEvent media(String id, java.util.List<GeneratedMedia> mediaOutputs, String provider, String model, String routeId, String traceId) {
        return new ChatStreamEvent(id, "media", null, mediaOutputs, null, provider, model, routeId, traceId, Map.of());
    }

    public static ChatStreamEvent done(String id, TokenUsage usage, String provider, String model, String routeId, String traceId) {
        return new ChatStreamEvent(id, "done", null, java.util.List.of(), usage, provider, model, routeId, traceId, Map.of());
    }
}
