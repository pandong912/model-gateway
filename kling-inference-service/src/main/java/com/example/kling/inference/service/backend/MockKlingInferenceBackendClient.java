package com.example.kling.inference.service.backend;

import com.example.kling.inference.contract.enums.AssetType;
import com.example.kling.inference.contract.enums.GenerationType;
import com.example.kling.inference.contract.enums.InferenceJobStatus;
import com.example.kling.inference.contract.model.BackendTaskEvent;
import com.example.kling.inference.contract.model.ImageGenerationPayload;
import com.example.kling.inference.contract.model.KlingGenerationPayload;
import com.example.kling.inference.contract.model.OutputAsset;
import com.example.kling.inference.contract.model.KlingGenerationRequest;
import com.example.kling.inference.contract.model.KlingGenerationResult;
import com.example.kling.inference.contract.model.VideoGenerationPayload;
import com.example.kling.inference.core.InferenceBackendClient;
import com.example.kling.inference.service.event.BackendTaskEventHandler;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Profile("!real-kling-backend")
@RequiredArgsConstructor
public class MockKlingInferenceBackendClient implements InferenceBackendClient {

    private final BackendTaskEventHandler eventHandler;

    @Override
    public Mono<BackendSubmission> submit(KlingGenerationRequest<? extends KlingGenerationPayload> request) {
        String backendTaskId = "mock_kt_" + UUID.randomUUID().toString().replace("-", "");
        BackendSubmission submission = new BackendSubmission(
                backendTaskId,
                "mock-kling-internal",
                "trace_" + UUID.randomUUID().toString().replace("-", "")
        );
        simulateAsyncCompletion(backendTaskId, request).subscribe();
        return Mono.just(submission);
    }

    @Override
    public Mono<Void> cancel(String backendTaskId) {
        return Mono.empty();
    }

    private Mono<Void> simulateAsyncCompletion(
            String backendTaskId,
            KlingGenerationRequest<? extends KlingGenerationPayload> request
    ) {
        return Mono.delay(Duration.ofSeconds(1))
                .then(eventHandler.handle(event(backendTaskId, InferenceJobStatus.RUNNING, 10, null, request)).then())
                .then(Mono.delay(Duration.ofSeconds(1)))
                .then(eventHandler.handle(event(backendTaskId, InferenceJobStatus.RUNNING, 60, null, request)).then())
                .then(Mono.delay(Duration.ofSeconds(1)))
                .then(eventHandler.handle(event(backendTaskId, InferenceJobStatus.SUCCEEDED, 100, result(request), request)).then());
    }

    private BackendTaskEvent event(
            String backendTaskId,
            InferenceJobStatus status,
            int progress,
            KlingGenerationResult result,
            KlingGenerationRequest<? extends KlingGenerationPayload> request
    ) {
        return new BackendTaskEvent(
                backendTaskId,
                status,
                progress,
                result,
                null,
                Instant.now(),
                Map.of(
                        "source", "mock-backend",
                        "requestId", request.requestId()
                )
        );
    }

    private KlingGenerationResult result(KlingGenerationRequest<? extends KlingGenerationPayload> request) {
        if (request.generationType() == GenerationType.IMAGE_GENERATION
                || request.generationType() == GenerationType.IMAGE_EDITING) {
            return imageResult(request);
        }
        return videoResult(request);
    }

    private KlingGenerationResult videoResult(KlingGenerationRequest<? extends KlingGenerationPayload> request) {
        VideoGenerationPayload payload = (VideoGenerationPayload) request.payload();
        OutputAsset video = new OutputAsset(
                "asset_" + UUID.randomUUID().toString().replace("-", ""),
                AssetType.VIDEO,
                "https://example.internal/mock-kling/videos/" + request.requestId() + ".mp4",
                "video/mp4",
                null,
                null,
                payload.durationSeconds(),
                Map.of("mock", true)
        );
        return new KlingGenerationResult(
                List.of(video),
                "https://example.internal/mock-kling/covers/" + request.requestId() + ".jpg",
                request.model(),
                payload.seed(),
                Map.of("backend", "mock-kling-internal")
        );
    }

    private KlingGenerationResult imageResult(KlingGenerationRequest<? extends KlingGenerationPayload> request) {
        ImageGenerationPayload payload = (ImageGenerationPayload) request.payload();
        OutputAsset image = new OutputAsset(
                "asset_" + UUID.randomUUID().toString().replace("-", ""),
                AssetType.IMAGE,
                "https://example.internal/mock-kling/images/" + request.requestId() + ".png",
                "image/png",
                1024,
                1024,
                null,
                Map.of("mock", true)
        );
        return new KlingGenerationResult(
                List.of(image),
                "https://example.internal/mock-kling/images/" + request.requestId() + ".png",
                request.model(),
                payload.seed(),
                Map.of("backend", "mock-kling-internal")
        );
    }
}
