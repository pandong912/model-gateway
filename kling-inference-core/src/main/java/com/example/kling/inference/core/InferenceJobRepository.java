package com.example.kling.inference.core;

import com.example.kling.inference.contract.model.VideoGenerationJob;
import reactor.core.publisher.Mono;

public interface InferenceJobRepository {

    Mono<VideoGenerationJob> save(VideoGenerationJob job);

    Mono<VideoGenerationJob> findById(String jobId);
}
