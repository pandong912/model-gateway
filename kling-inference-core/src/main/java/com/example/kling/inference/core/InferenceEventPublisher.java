package com.example.kling.inference.core;

import com.example.kling.inference.contract.model.VideoGenerationEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface InferenceEventPublisher {

    Mono<Void> publish(VideoGenerationEvent event);

    Flux<VideoGenerationEvent> watch(String jobId);
}
