package com.example.kling.inference.core;

import com.example.kling.inference.contract.model.VideoGenerationRequest;
import reactor.core.publisher.Mono;

public interface InferenceBackendClient {

    Mono<BackendSubmission> submit(VideoGenerationRequest request);

    Mono<Void> cancel(String backendTaskId);

    record BackendSubmission(
            String backendTaskId,
            String backendProvider,
            String traceId
    ) {
    }
}
