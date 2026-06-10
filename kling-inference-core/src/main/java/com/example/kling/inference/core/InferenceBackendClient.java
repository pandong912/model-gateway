package com.example.kling.inference.core;

import com.example.kling.inference.contract.model.KlingGenerationRequest;
import reactor.core.publisher.Mono;

public interface InferenceBackendClient {

    Mono<BackendSubmission> submit(KlingGenerationRequest request);

    Mono<Void> cancel(String backendTaskId);

    record BackendSubmission(
            String backendTaskId,
            String backendProvider,
            String traceId
    ) {
    }
}
