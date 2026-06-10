package com.example.kling.inference.core;

import com.example.kling.inference.contract.model.KlingGenerationRequest;
import com.example.kling.inference.contract.model.KlingGenerationPayload;
import reactor.core.publisher.Mono;

public interface InferenceBackendClient {

    Mono<BackendSubmission> submit(KlingGenerationRequest<? extends KlingGenerationPayload> request);

    Mono<Void> cancel(String backendTaskId);

    record BackendSubmission(
            String backendTaskId,
            String backendProvider,
            String traceId
    ) {
    }
}
