package com.example.modelgateway.api.model;

import com.example.modelgateway.api.enums.FinishReason;
import java.util.List;
import java.util.Map;

public record ChatCompletionResponse(
        String id,
        String content,
        List<ToolCall> toolCalls,
        List<GeneratedMedia> mediaOutputs,
        FinishReason finishReason,
        TokenUsage usage,
        String provider,
        String model,
        String routeId,
        String traceId,
        long latencyMs,
        Map<String, Object> metadata
) {
}
