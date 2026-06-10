package com.example.kling.inference.contract.model;

import com.example.kling.inference.contract.enums.GenerationType;
import com.example.kling.inference.contract.enums.InferenceJobStatus;
import java.time.Instant;
import java.util.Map;

public record VideoGenerationJob(
        String jobId,
        String requestId,
        GenerationType generationType,
        InferenceJobStatus status,
        Integer progress,
        String traceId,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt,
        Integer estimatedWaitSeconds,
        VideoGenerationResult result,
        InferenceError error,
        Map<String, Object> metadata
) {
}
