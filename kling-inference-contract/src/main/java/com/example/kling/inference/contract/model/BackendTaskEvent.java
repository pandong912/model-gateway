package com.example.kling.inference.contract.model;

import com.example.kling.inference.contract.enums.InferenceJobStatus;
import java.time.Instant;
import java.util.Map;

public record BackendTaskEvent(
        String backendTaskId,
        InferenceJobStatus status,
        Integer progress,
        VideoGenerationResult result,
        InferenceError error,
        Instant occurredAt,
        Map<String, Object> metadata
) {
}
