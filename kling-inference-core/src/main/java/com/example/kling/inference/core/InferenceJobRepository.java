package com.example.kling.inference.core;

import com.example.kling.inference.contract.model.VideoGenerationJob;
import com.example.kling.inference.contract.model.VideoGenerationRequest;
import reactor.core.publisher.Mono;

public interface InferenceJobRepository {

    Mono<VideoGenerationJob> save(VideoGenerationJob job, VideoGenerationRequest request);

    Mono<VideoGenerationJob> update(VideoGenerationJob job);

    Mono<VideoGenerationJob> findById(String jobId);

    Mono<VideoGenerationJob> findByIdempotencyKey(String callerId, String idempotencyKey);

    Mono<VideoGenerationJob> findByBackendTaskId(String backendTaskId);
}
