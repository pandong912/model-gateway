package com.example.kling.inference.service.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kling.inference.contract.enums.AssetType;
import com.example.kling.inference.contract.enums.GenerationType;
import com.example.kling.inference.contract.enums.InferenceEventType;
import com.example.kling.inference.contract.enums.InferenceJobStatus;
import com.example.kling.inference.contract.model.BackendTaskEvent;
import com.example.kling.inference.contract.model.OutputAsset;
import com.example.kling.inference.contract.model.VideoGenerationEvent;
import com.example.kling.inference.contract.model.VideoGenerationJob;
import com.example.kling.inference.contract.model.VideoGenerationRequest;
import com.example.kling.inference.contract.model.VideoGenerationResult;
import com.example.kling.inference.core.InferenceEventPublisher;
import com.example.kling.inference.core.InferenceJobRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class BackendTaskEventHandlerTest {

    @Test
    void updatesJobAndPublishesCompletionEvent() {
        Instant now = Instant.now();
        VideoGenerationJob queued = new VideoGenerationJob(
                "kg_001",
                "req_001",
                "idem_001",
                "model-gateway",
                GenerationType.TEXT_TO_VIDEO,
                InferenceJobStatus.QUEUED,
                0,
                "backend_001",
                "mock",
                "trace_001",
                now,
                now,
                now.plusSeconds(3600),
                60,
                null,
                null,
                Map.of()
        );
        VideoGenerationResult result = new VideoGenerationResult(
                List.of(new OutputAsset("asset_001", AssetType.VIDEO, "https://example/video.mp4", "video/mp4", null, null, 5, Map.of())),
                "https://example/cover.jpg",
                "kling-video",
                "v1",
                null,
                Map.of()
        );
        InMemoryJobRepository jobRepository = new InMemoryJobRepository(queued);
        CapturingEventPublisher eventPublisher = new CapturingEventPublisher();
        BackendTaskEventHandler handler = new BackendTaskEventHandler(jobRepository, eventPublisher);

        VideoGenerationJob updated = handler.handle(new BackendTaskEvent(
                "backend_001",
                InferenceJobStatus.SUCCEEDED,
                100,
                result,
                null,
                now.plusSeconds(5),
                Map.of("source", "test")
        )).block();

        assertThat(updated.status()).isEqualTo(InferenceJobStatus.SUCCEEDED);
        assertThat(updated.progress()).isEqualTo(100);
        assertThat(updated.result()).isEqualTo(result);
        assertThat(eventPublisher.events).hasSize(1);
        assertThat(eventPublisher.events.getFirst().type()).isEqualTo(InferenceEventType.COMPLETED);
    }

    private static class InMemoryJobRepository implements InferenceJobRepository {
        private final AtomicReference<VideoGenerationJob> job;

        private InMemoryJobRepository(VideoGenerationJob job) {
            this.job = new AtomicReference<>(job);
        }

        @Override
        public Mono<VideoGenerationJob> save(VideoGenerationJob job, VideoGenerationRequest request) {
            this.job.set(job);
            return Mono.just(job);
        }

        @Override
        public Mono<VideoGenerationJob> update(VideoGenerationJob job) {
            this.job.set(job);
            return Mono.just(job);
        }

        @Override
        public Mono<VideoGenerationJob> findById(String jobId) {
            return Mono.just(job.get()).filter(value -> value.jobId().equals(jobId));
        }

        @Override
        public Mono<VideoGenerationJob> findByIdempotencyKey(String callerId, String idempotencyKey) {
            return Mono.just(job.get()).filter(value -> callerId.equals(value.callerId()) && idempotencyKey.equals(value.idempotencyKey()));
        }

        @Override
        public Mono<VideoGenerationJob> findByBackendTaskId(String backendTaskId) {
            return Mono.just(job.get()).filter(value -> backendTaskId.equals(value.backendTaskId()));
        }
    }

    private static class CapturingEventPublisher implements InferenceEventPublisher {
        private final List<VideoGenerationEvent> events = new ArrayList<>();

        @Override
        public Mono<Void> publish(VideoGenerationEvent event) {
            events.add(event);
            return Mono.empty();
        }

        @Override
        public Flux<VideoGenerationEvent> watch(String jobId) {
            return Flux.fromIterable(events).filter(event -> event.jobId().equals(jobId));
        }
    }
}
