package com.example.kling.inference.core;

import com.example.kling.inference.contract.model.KlingGenerationJob;
import com.example.kling.inference.contract.model.KlingGenerationRequest;
import reactor.core.publisher.Mono;

public interface InferenceJobRepository {

    Mono<KlingGenerationJob> save(KlingGenerationJob job, KlingGenerationRequest request);

    Mono<KlingGenerationJob> update(KlingGenerationJob job);

    Mono<KlingGenerationJob> findById(String jobId);

    Mono<KlingGenerationJob> findByIdempotencyKey(String callerId, String idempotencyKey);

    Mono<KlingGenerationJob> findByBackendTaskId(String backendTaskId);
}
