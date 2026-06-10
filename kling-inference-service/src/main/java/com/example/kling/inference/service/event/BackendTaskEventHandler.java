package com.example.kling.inference.service.event;

import com.example.kling.inference.contract.enums.InferenceEventType;
import com.example.kling.inference.contract.enums.InferenceJobStatus;
import com.example.kling.inference.contract.model.BackendTaskEvent;
import com.example.kling.inference.contract.model.VideoGenerationEvent;
import com.example.kling.inference.contract.model.VideoGenerationJob;
import com.example.kling.inference.core.InferenceEventPublisher;
import com.example.kling.inference.core.InferenceJobRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class BackendTaskEventHandler {

    private final InferenceJobRepository jobRepository;
    private final InferenceEventPublisher eventPublisher;

    public Mono<VideoGenerationJob> handle(BackendTaskEvent event) {
        return jobRepository.findByBackendTaskId(event.backendTaskId())
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Kling inference job not found by backend task: " + event.backendTaskId())))
                .flatMap(job -> {
                    if (job.status().isTerminal()) {
                        return Mono.just(job);
                    }
                    VideoGenerationJob updated = update(job, event);
                    return jobRepository.update(updated)
                            .flatMap(saved -> eventPublisher.publish(toGenerationEvent(saved, event)).thenReturn(saved));
                });
    }

    private VideoGenerationJob update(VideoGenerationJob job, BackendTaskEvent event) {
        Instant now = event.occurredAt() == null ? Instant.now() : event.occurredAt();
        return new VideoGenerationJob(
                job.jobId(),
                job.requestId(),
                job.idempotencyKey(),
                job.callerId(),
                job.generationType(),
                event.status(),
                event.progress() == null ? job.progress() : event.progress(),
                job.backendTaskId(),
                job.backendProvider(),
                job.traceId(),
                job.createdAt(),
                now,
                job.expiresAt(),
                job.estimatedWaitSeconds(),
                event.result() == null ? job.result() : event.result(),
                event.error() == null ? job.error() : event.error(),
                job.metadata()
        );
    }

    private VideoGenerationEvent toGenerationEvent(VideoGenerationJob job, BackendTaskEvent backendEvent) {
        return new VideoGenerationEvent(
                "evt_" + UUID.randomUUID().toString().replace("-", ""),
                job.jobId(),
                eventType(job.status()),
                job.status(),
                job.progress(),
                job.result(),
                job.error(),
                backendEvent.occurredAt() == null ? Instant.now() : backendEvent.occurredAt(),
                backendEvent.metadata() == null ? Map.of() : backendEvent.metadata()
        );
    }

    private InferenceEventType eventType(InferenceJobStatus status) {
        if (status == InferenceJobStatus.SUCCEEDED) {
            return InferenceEventType.COMPLETED;
        }
        if (status == InferenceJobStatus.FAILED) {
            return InferenceEventType.FAILED;
        }
        if (status == InferenceJobStatus.CANCELLED) {
            return InferenceEventType.CANCELLED;
        }
        return InferenceEventType.PROGRESS;
    }
}
