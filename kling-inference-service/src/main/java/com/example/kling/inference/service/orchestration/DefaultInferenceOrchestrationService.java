package com.example.kling.inference.service.orchestration;

import com.example.kling.inference.contract.enums.InferenceEventType;
import com.example.kling.inference.contract.enums.InferenceJobStatus;
import com.example.kling.inference.contract.model.CancelJobRequest;
import com.example.kling.inference.contract.model.InferenceCaller;
import com.example.kling.inference.contract.model.InferenceError;
import com.example.kling.inference.contract.model.KlingGenerationEvent;
import com.example.kling.inference.contract.model.KlingGenerationJob;
import com.example.kling.inference.contract.model.KlingGenerationPayload;
import com.example.kling.inference.contract.model.KlingGenerationRequest;
import com.example.kling.inference.contract.model.KlingGenerationResult;
import com.example.kling.inference.core.InferenceBackendClient;
import com.example.kling.inference.core.InferenceEventPublisher;
import com.example.kling.inference.core.InferenceJobRepository;
import com.example.kling.inference.core.InferenceOrchestrationService;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DefaultInferenceOrchestrationService implements InferenceOrchestrationService {

    private static final Duration DEFAULT_JOB_TTL = Duration.ofDays(7);

    private final InferenceJobRepository jobRepository;
    private final InferenceEventPublisher eventPublisher;
    private final InferenceBackendClient backendClient;

    @Override
    public Mono<KlingGenerationJob> submit(KlingGenerationRequest<? extends KlingGenerationPayload> request) {
        String callerId = callerId(request);
        if (!isBlank(request.idempotencyKey())) {
            return jobRepository.findByIdempotencyKey(callerId, request.idempotencyKey())
                    .switchIfEmpty(Mono.defer(() -> createAndSubmit(request, callerId)));
        }
        return createAndSubmit(request, callerId);
    }

    @Override
    public Mono<KlingGenerationJob> getJob(String jobId) {
        return jobRepository.findById(jobId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Kling generation job not found: " + jobId)));
    }

    @Override
    public Mono<KlingGenerationResult> getResult(String jobId) {
        return getJob(jobId).flatMap(job -> {
            if (job.status() == InferenceJobStatus.SUCCEEDED && job.result() != null) {
                return Mono.just(job.result());
            }
            if (!job.status().isTerminal()) {
                return Mono.error(new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Kling generation job is not completed yet: " + job.status()));
            }
            return Mono.error(new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Kling generation job ended without successful result: " + job.status()));
        });
    }

    @Override
    public Mono<KlingGenerationJob> waitJob(String jobId, Duration timeout) {
        return getJob(jobId).flatMap(job -> {
            if (job.status().isTerminal()) {
                return Mono.just(job);
            }
            return eventPublisher.watch(jobId)
                    .next()
                    .flatMap(ignored -> getJob(jobId))
                    .timeout(timeout)
                    .onErrorResume(TimeoutException.class, ignored -> getJob(jobId));
        });
    }

    @Override
    public Flux<KlingGenerationEvent> watchJob(String jobId) {
        return getJob(jobId).flatMapMany(job -> Flux.concat(
                Flux.just(event(job, InferenceEventType.STATUS_CHANGED)),
                eventPublisher.watch(jobId)
        ));
    }

    @Override
    public Mono<KlingGenerationJob> cancelJob(String jobId, CancelJobRequest request) {
        return getJob(jobId).flatMap(job -> {
            if (job.status().isTerminal()) {
                return Mono.just(job);
            }
            Mono<Void> backendCancel = isBlank(job.backendTaskId()) ? Mono.empty() : backendClient.cancel(job.backendTaskId());
            return backendCancel.then(updateStatus(job, InferenceJobStatus.CANCELLED, job.progress(), null, null)
                    .flatMap(cancelled -> publish(cancelled, InferenceEventType.CANCELLED).thenReturn(cancelled)));
        });
    }

    private Mono<KlingGenerationJob> createAndSubmit(KlingGenerationRequest<? extends KlingGenerationPayload> request, String callerId) {
        Instant now = Instant.now();
        KlingGenerationJob created = new KlingGenerationJob(
                "kg_" + UUID.randomUUID().toString().replace("-", ""),
                request.requestId(),
                request.idempotencyKey(),
                callerId,
                request.generationType(),
                InferenceJobStatus.CREATED,
                0,
                null,
                null,
                UUID.randomUUID().toString(),
                now,
                now,
                now.plus(DEFAULT_JOB_TTL),
                60,
                null,
                null,
                metadata(request)
        );

        return jobRepository.save(created, request)
                .flatMap(job -> publish(job, InferenceEventType.JOB_CREATED).thenReturn(job))
                .flatMap(job -> backendClient.submit(request)
                        .flatMap(submission -> {
                            KlingGenerationJob submitted = withBackendSubmission(job, submission);
                            return jobRepository.update(submitted)
                                    .flatMap(updated -> publish(updated, InferenceEventType.STATUS_CHANGED).thenReturn(updated));
                        })
                        .onErrorResume(ex -> failSubmit(job, ex)));
    }

    private Mono<KlingGenerationJob> failSubmit(KlingGenerationJob job, Throwable ex) {
        InferenceError error = new InferenceError(
                "BACKEND_SUBMIT_FAILED",
                ex.getMessage() == null ? "Failed to submit backend inference task" : ex.getMessage(),
                true,
                Map.of()
        );
        return updateStatus(job, InferenceJobStatus.FAILED, job.progress(), null, error)
                .flatMap(failed -> publish(failed, InferenceEventType.FAILED).thenReturn(failed));
    }

    private KlingGenerationJob withBackendSubmission(
            KlingGenerationJob job,
            InferenceBackendClient.BackendSubmission submission
    ) {
        Instant now = Instant.now();
        return new KlingGenerationJob(
                job.jobId(),
                job.requestId(),
                job.idempotencyKey(),
                job.callerId(),
                job.generationType(),
                InferenceJobStatus.QUEUED,
                job.progress(),
                submission.backendTaskId(),
                submission.backendProvider(),
                isBlank(submission.traceId()) ? job.traceId() : submission.traceId(),
                job.createdAt(),
                now,
                job.expiresAt(),
                job.estimatedWaitSeconds(),
                job.result(),
                job.error(),
                job.metadata()
        );
    }

    private Mono<KlingGenerationJob> updateStatus(
            KlingGenerationJob job,
            InferenceJobStatus status,
            Integer progress,
            com.example.kling.inference.contract.model.KlingGenerationResult result,
            InferenceError error
    ) {
        KlingGenerationJob updated = new KlingGenerationJob(
                job.jobId(),
                job.requestId(),
                job.idempotencyKey(),
                job.callerId(),
                job.generationType(),
                status,
                progress == null ? job.progress() : progress,
                job.backendTaskId(),
                job.backendProvider(),
                job.traceId(),
                job.createdAt(),
                Instant.now(),
                job.expiresAt(),
                job.estimatedWaitSeconds(),
                result == null ? job.result() : result,
                error == null ? job.error() : error,
                job.metadata()
        );
        return jobRepository.update(updated);
    }

    private Mono<Void> publish(KlingGenerationJob job, InferenceEventType type) {
        return eventPublisher.publish(event(job, type));
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

    private String callerId(KlingGenerationRequest<? extends KlingGenerationPayload> request) {
        InferenceCaller caller = request.caller();
        if (caller == null || isBlank(caller.callerId())) {
            return "anonymous";
        }
        return caller.callerId();
    }

    private Map<String, Object> metadata(KlingGenerationRequest<? extends KlingGenerationPayload> request) {
        String scenario = request.scenario() == null ? "" : request.scenario();
        String model = request.model() == null ? "" : request.model();
        return Map.of(
                "scenario", scenario,
                "model", model
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
