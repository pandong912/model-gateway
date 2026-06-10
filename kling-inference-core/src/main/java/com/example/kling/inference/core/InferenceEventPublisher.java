package com.example.kling.inference.core;

import com.example.kling.inference.contract.model.KlingGenerationEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface InferenceEventPublisher {

    Mono<Void> publish(KlingGenerationEvent event);

    Flux<KlingGenerationEvent> watch(String jobId);
}
