package com.example.kling.inference.service.backend;

import com.example.kling.inference.contract.model.VideoGenerationRequest;
import com.example.kling.inference.core.InferenceBackendClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Profile("real-kling-backend")
public class KlingInternalInferenceBackendClient implements InferenceBackendClient {

    private final WebClient webClient;
    private final String submitPath;
    private final String cancelPathTemplate;

    public KlingInternalInferenceBackendClient(
            WebClient.Builder webClientBuilder,
            @Value("${kling.inference.backend.base-url}") String baseUrl,
            @Value("${kling.inference.backend.submit-path:/internal/inference/tasks}") String submitPath,
            @Value("${kling.inference.backend.cancel-path-template:/internal/inference/tasks/{backendTaskId}/cancel}") String cancelPathTemplate
    ) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.submitPath = submitPath;
        this.cancelPathTemplate = cancelPathTemplate;
    }

    @Override
    public Mono<BackendSubmission> submit(VideoGenerationRequest request) {
        return webClient.post()
                .uri(submitPath)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(KlingBackendSubmissionResponse.class)
                .map(response -> new BackendSubmission(
                        response.backendTaskId(),
                        response.backendProvider(),
                        response.traceId()
                ));
    }

    @Override
    public Mono<Void> cancel(String backendTaskId) {
        return webClient.post()
                .uri(cancelPathTemplate, backendTaskId)
                .retrieve()
                .bodyToMono(Void.class);
    }

    private record KlingBackendSubmissionResponse(
            String backendTaskId,
            String backendProvider,
            String traceId
    ) {
    }
}
