package com.example.kling.inference.client;

import com.example.kling.inference.contract.model.CancelJobRequest;
import com.example.kling.inference.contract.model.VideoGenerationEvent;
import com.example.kling.inference.contract.model.VideoGenerationJob;
import com.example.kling.inference.contract.model.VideoGenerationRequest;
import com.example.kling.inference.contract.model.VideoGenerationResult;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface KlingInferenceClient {

    Mono<VideoGenerationJob> submit(VideoGenerationRequest request);

    Mono<VideoGenerationJob> getJob(String jobId);

    Mono<VideoGenerationJob> waitJob(String jobId, Duration timeout);

    Flux<VideoGenerationEvent> watchJob(String jobId);

    Mono<VideoGenerationJob> cancelJob(String jobId, CancelJobRequest request);

    CompletableFuture<VideoGenerationResult> generateAsync(VideoGenerationRequest request);
}
