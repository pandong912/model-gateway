package com.example.kling.inference.client;

import com.example.kling.inference.contract.model.CancelJobRequest;
import com.example.kling.inference.contract.model.VideoGenerationEvent;
import com.example.kling.inference.contract.model.VideoGenerationJob;
import com.example.kling.inference.contract.model.VideoGenerationRequest;
import com.example.kling.inference.contract.model.VideoGenerationResult;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class WebClientKlingInferenceClient implements KlingInferenceClient {

    private static final String VIDEO_GENERATIONS_PATH = "/internal/v1/video-generations";

    private final WebClient webClient;
    private final KlingInferenceClientOptions options;

    public WebClientKlingInferenceClient(KlingInferenceClientOptions options) {
        this(WebClient.builder(), options);
    }

    public WebClientKlingInferenceClient(WebClient.Builder webClientBuilder, KlingInferenceClientOptions options) {
        this.options = options;
        WebClient.Builder builder = webClientBuilder.baseUrl(options.baseUrl());
        if (options.apiKey() != null && !options.apiKey().isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + options.apiKey());
        }
        this.webClient = builder.build();
    }

    @Override
    public Mono<VideoGenerationJob> submit(VideoGenerationRequest request) {
        return webClient.post()
                .uri(VIDEO_GENERATIONS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(VideoGenerationJob.class);
    }

    @Override
    public Mono<VideoGenerationJob> getJob(String jobId) {
        return webClient.get()
                .uri(VIDEO_GENERATIONS_PATH + "/{jobId}", jobId)
                .retrieve()
                .bodyToMono(VideoGenerationJob.class);
    }

    @Override
    public Mono<VideoGenerationJob> waitJob(String jobId, Duration timeout) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(VIDEO_GENERATIONS_PATH + "/{jobId}:wait")
                        .queryParam("timeoutSeconds", timeout.toSeconds())
                        .build(jobId))
                .retrieve()
                .bodyToMono(VideoGenerationJob.class);
    }

    @Override
    public Flux<VideoGenerationEvent> watchJob(String jobId) {
        return webClient.get()
                .uri(VIDEO_GENERATIONS_PATH + "/{jobId}/events", jobId)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(VideoGenerationEvent.class);
    }

    @Override
    public Mono<VideoGenerationJob> cancelJob(String jobId, CancelJobRequest request) {
        return webClient.post()
                .uri(VIDEO_GENERATIONS_PATH + "/{jobId}:cancel", jobId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request == null ? new CancelJobRequest(null, null, null) : request)
                .retrieve()
                .bodyToMono(VideoGenerationJob.class);
    }

    @Override
    public CompletableFuture<VideoGenerationResult> generateAsync(VideoGenerationRequest request) {
        return submit(request)
                .flatMap(job -> waitUntilTerminal(job.jobId()))
                .map(VideoGenerationJob::result)
                .toFuture();
    }

    private Mono<VideoGenerationJob> waitUntilTerminal(String jobId) {
        return waitJob(jobId, options.waitTimeout())
                .flatMap(job -> {
                    if (job.status().isTerminal()) {
                        return Mono.just(job);
                    }
                    return Mono.delay(options.pollInterval()).then(waitUntilTerminal(jobId));
                });
    }
}
