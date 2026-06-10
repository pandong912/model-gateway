package com.example.kling.inference.contract.model;

import com.example.kling.inference.contract.enums.InferenceEventType;
import com.example.kling.inference.contract.enums.InferenceJobStatus;
import java.time.Instant;
import java.util.Map;

public record KlingGenerationEvent(
        String eventId,
        String jobId,
        InferenceEventType type,
        InferenceJobStatus status,
        Integer progress,
        KlingGenerationResult result,
        InferenceError error,
        Instant occurredAt,
        Map<String, Object> metadata
) {
}
