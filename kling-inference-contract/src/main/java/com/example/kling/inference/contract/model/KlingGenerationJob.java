package com.example.kling.inference.contract.model;

import com.example.kling.inference.contract.enums.GenerationType;
import com.example.kling.inference.contract.enums.InferenceJobStatus;
import java.time.Instant;
import java.util.Map;

public record KlingGenerationJob(
        String jobId,
        String requestId,
        String idempotencyKey,
        String callerId,
        GenerationType generationType,
        InferenceJobStatus status,
        Integer progress,
        String backendTaskId,
        String backendProvider,
        String traceId,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt,
        Integer estimatedWaitSeconds,
        KlingGenerationResult result,
        InferenceError error,
        Map<String, Object> metadata
) {
}
