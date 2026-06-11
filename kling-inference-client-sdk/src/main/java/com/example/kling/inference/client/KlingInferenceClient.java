package com.example.kling.inference.client;

import com.example.kling.inference.contract.model.CancelJobRequest;
import com.example.kling.inference.contract.model.KlingGenerationEvent;
import com.example.kling.inference.contract.model.KlingGenerationJob;
import com.example.kling.inference.contract.model.KlingGenerationPayload;
import com.example.kling.inference.contract.model.KlingGenerationRequest;
import com.example.kling.inference.contract.model.KlingGenerationResult;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface KlingInferenceClient {

    Mono<KlingGenerationJob> submit(KlingGenerationRequest<? extends KlingGenerationPayload> request);

    Mono<KlingGenerationJob> getJob(String jobId);

    Mono<KlingGenerationResult> getResult(String jobId);

    Mono<KlingGenerationJob> waitJob(String jobId, Duration timeout);

    Flux<KlingGenerationEvent> watchJob(String jobId);

    Mono<KlingGenerationJob> cancelJob(String jobId, CancelJobRequest request);

    CompletableFuture<KlingGenerationResult> generateAsync(KlingGenerationRequest<? extends KlingGenerationPayload> request);
}
