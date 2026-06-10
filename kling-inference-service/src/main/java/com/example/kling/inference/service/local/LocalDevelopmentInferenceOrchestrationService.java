package com.example.kling.inference.service.local;

import com.example.kling.inference.contract.enums.InferenceEventType;
import com.example.kling.inference.contract.enums.InferenceJobStatus;
import com.example.kling.inference.contract.model.CancelJobRequest;
import com.example.kling.inference.contract.model.KlingGenerationEvent;
import com.example.kling.inference.contract.model.KlingGenerationJob;
import com.example.kling.inference.contract.model.KlingGenerationPayload;
import com.example.kling.inference.contract.model.KlingGenerationRequest;
import com.example.kling.inference.core.InferenceOrchestrationService;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Service
@Profile("local-dev")
public class LocalDevelopmentInferenceOrchestrationService implements InferenceOrchestrationService {

    private final ConcurrentMap<String, KlingGenerationJob> jobs = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Sinks.Many<KlingGenerationEvent>> eventSinks = new ConcurrentHashMap<>();

    @Override
    public Mono<KlingGenerationJob> submit(KlingGenerationRequest<? extends KlingGenerationPayload> request) {
        String jobId = "kg_" + UUID.randomUUID().toString().replace("-", "");
        Instant now = Instant.now();
        KlingGenerationJob job = new KlingGenerationJob(
                jobId,
                request.requestId(),
                request.idempotencyKey(),
                request.caller() == null ? "anonymous" : request.caller().callerId(),
                request.generationType(),
                InferenceJobStatus.QUEUED,
                0,
                null,
                null,
                UUID.randomUUID().toString(),
                now,
                now,
                now.plus(Duration.ofDays(7)),
                60,
                null,
                null,
                Map.of(
                        "scenario", request.scenario() == null ? "" : request.scenario(),
                        "backendMode", "local-development"
                )
        );
        jobs.put(jobId, job);
        publish(event(job, InferenceEventType.JOB_CREATED));
        return Mono.just(job);
    }

    @Override
    public Mono<KlingGenerationJob> getJob(String jobId) {
        KlingGenerationJob job = jobs.get(jobId);
        if (job == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Video generation job not found: " + jobId));
        }
        return Mono.just(job);
    }

    @Override
    public Mono<KlingGenerationJob> waitJob(String jobId, Duration timeout) {
        return getJob(jobId).flatMap(job -> {
            if (job.status().isTerminal()) {
                return Mono.just(job);
            }
            return watchJob(jobId)
                    .next()
                    .map(event -> jobs.get(jobId))
                    .timeout(timeout)
                    .onErrorResume(TimeoutException.class, ignored -> getJob(jobId));
        });
    }

    @Override
    public Flux<KlingGenerationEvent> watchJob(String jobId) {
        KlingGenerationJob current = jobs.get(jobId);
        if (current == null) {
            return Flux.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Video generation job not found: " + jobId));
        }
        return Flux.concat(
                Flux.just(event(current, InferenceEventType.STATUS_CHANGED)),
                sink(jobId).asFlux()
        );
    }

    @Override
    public Mono<KlingGenerationJob> cancelJob(String jobId, CancelJobRequest request) {
        return getJob(jobId).map(current -> {
            if (current.status().isTerminal()) {
                return current;
            }
            Instant now = Instant.now();
            KlingGenerationJob cancelled = new KlingGenerationJob(
                    current.jobId(),
                    current.requestId(),
                    current.idempotencyKey(),
                    current.callerId(),
                    current.generationType(),
                    InferenceJobStatus.CANCELLED,
                    current.progress(),
                    current.backendTaskId(),
                    current.backendProvider(),
                    current.traceId(),
                    current.createdAt(),
                    now,
                    current.expiresAt(),
                    current.estimatedWaitSeconds(),
                    current.result(),
                    current.error(),
                    current.metadata()
            );
            jobs.put(jobId, cancelled);
            publish(event(cancelled, InferenceEventType.CANCELLED));
            return cancelled;
        });
    }

    private Sinks.Many<KlingGenerationEvent> sink(String jobId) {
        return eventSinks.computeIfAbsent(jobId, ignored -> Sinks.many().multicast().directBestEffort());
    }

    private void publish(KlingGenerationEvent event) {
        sink(event.jobId()).tryEmitNext(event);
    }

    private KlingGenerationEvent event(KlingGenerationJob job, InferenceEventType type) {
        return new KlingGenerationEvent(
                "evt_" + UUID.randomUUID().toString().replace("-", ""),
                job.jobId(),
                type,
                job.status(),
                job.progress(),
                job.result(),
                job.error(),
                Instant.now(),
                Map.of()
        );
    }
}
