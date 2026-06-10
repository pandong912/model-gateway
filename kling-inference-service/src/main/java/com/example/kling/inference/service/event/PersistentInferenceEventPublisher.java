package com.example.kling.inference.service.event;

import com.example.kling.inference.contract.model.VideoGenerationEvent;
import com.example.kling.inference.core.InferenceEventPublisher;
import com.example.kling.inference.service.persistence.KlingInferenceEventRepository;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Component
@RequiredArgsConstructor
public class PersistentInferenceEventPublisher implements InferenceEventPublisher {

    private final KlingInferenceEventRepository eventRepository;
    private final ConcurrentMap<String, Sinks.Many<VideoGenerationEvent>> sinks = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> publish(VideoGenerationEvent event) {
        return eventRepository.save(event)
                .then(Mono.fromRunnable(() -> sink(event.jobId()).tryEmitNext(event)));
    }

    @Override
    public Flux<VideoGenerationEvent> watch(String jobId) {
        return sink(jobId).asFlux();
    }

    private Sinks.Many<VideoGenerationEvent> sink(String jobId) {
        return sinks.computeIfAbsent(jobId, ignored -> Sinks.many().multicast().directBestEffort());
    }
}
