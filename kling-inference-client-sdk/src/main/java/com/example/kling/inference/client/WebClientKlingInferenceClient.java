package com.example.kling.inference.client;

import com.example.kling.inference.client.exception.KlingInferenceException;
import com.example.kling.inference.client.exception.KlingInferenceJobException;
import com.example.kling.inference.contract.enums.InferenceJobStatus;
import com.example.kling.inference.contract.model.CancelJobRequest;
import com.example.kling.inference.contract.model.KlingGenerationEvent;
import com.example.kling.inference.contract.model.KlingGenerationJob;
import com.example.kling.inference.contract.model.KlingGenerationRequest;
import com.example.kling.inference.contract.model.KlingGenerationResult;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class WebClientKlingInferenceClient implements KlingInferenceClient {

    public static final String VIDEO_GENERATIONS_PATH = "/internal/v1/video-generations";
    public static final String IMAGE_GENERATIONS_PATH = "/internal/v1/image-generations";

    private final WebClient webClient;
    private final KlingInferenceClientOptions options;
    private final String generationPath;

    public WebClientKlingInferenceClient(KlingInferenceClientOptions options) {
        this(WebClient.builder(), options, VIDEO_GENERATIONS_PATH);
    }

    public WebClientKlingInferenceClient(KlingInferenceClientOptions options, String generationPath) {
        this(WebClient.builder(), options, generationPath);
    }

    public WebClientKlingInferenceClient(WebClient.Builder webClientBuilder, KlingInferenceClientOptions options) {
        this(webClientBuilder, options, VIDEO_GENERATIONS_PATH);
    }

    public WebClientKlingInferenceClient(
            WebClient.Builder webClientBuilder,
            KlingInferenceClientOptions options,
            String generationPath
    ) {
        this.options = options;
        this.generationPath = generationPath;
        WebClient.Builder builder = webClientBuilder.baseUrl(options.baseUrl());
        if (options.apiKey() != null && !options.apiKey().isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + options.apiKey());
        }
        this.webClient = builder.build();
    }

    @Override
    public Mono<KlingGenerationJob> submit(KlingGenerationRequest request) {
        return webClient.post()
                .uri(generationPath)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(KlingGenerationJob.class);
    }

    @Override
    public Mono<KlingGenerationJob> getJob(String jobId) {
        return webClient.get()
                .uri(generationPath + "/{jobId}", jobId)
                .retrieve()
                .bodyToMono(KlingGenerationJob.class);
    }

    @Override
    public Mono<KlingGenerationJob> waitJob(String jobId, Duration timeout) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(generationPath + "/{jobId}:wait")
                        .queryParam("timeoutSeconds", timeout.toSeconds())
                        .build(jobId))
                .retrieve()
                .bodyToMono(KlingGenerationJob.class);
    }

    @Override
    public Flux<KlingGenerationEvent> watchJob(String jobId) {
        return webClient.get()
                .uri(generationPath + "/{jobId}/events", jobId)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(KlingGenerationEvent.class);
    }

    @Override
    public Mono<KlingGenerationJob> cancelJob(String jobId, CancelJobRequest request) {
        return webClient.post()
                .uri(generationPath + "/{jobId}:cancel", jobId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request == null ? new CancelJobRequest(null, null, null) : request)
                .retrieve()
                .bodyToMono(KlingGenerationJob.class);
    }

    @Override
    public CompletableFuture<KlingGenerationResult> generateAsync(KlingGenerationRequest request) {
        return submit(request)
                .flatMap(job -> waitUntilTerminal(job.jobId()))
                .flatMap(this::requireSuccessfulResult)
                .toFuture();
    }

    private Mono<KlingGenerationJob> waitUntilTerminal(String jobId) {
        return waitJob(jobId, options.waitTimeout())
                .flatMap(job -> {
                    if (job.status().isTerminal()) {
                        return Mono.just(job);
                    }
                    return Mono.delay(options.pollInterval()).then(waitUntilTerminal(jobId));
                });
    }

    private Mono<KlingGenerationResult> requireSuccessfulResult(KlingGenerationJob job) {
        if (!job.status().isTerminal()) {
            return Mono.error(new KlingInferenceException("Kling inference job wait ended before terminal status: " + job.jobId()));
        }
        if (job.status() == InferenceJobStatus.SUCCEEDED && job.result() != null) {
            return Mono.just(job.result());
        }
        return Mono.error(new KlingInferenceJobException(job));
    }
}
