package com.example.kling.inference.client.example;

import com.example.kling.inference.client.KlingInferenceClient;
import com.example.kling.inference.client.KlingInferenceClientOptions;
import com.example.kling.inference.client.WebClientKlingInferenceClient;
import com.example.kling.inference.contract.enums.GenerationType;
import com.example.kling.inference.contract.model.InferenceCaller;
import com.example.kling.inference.contract.model.InferenceError;
import com.example.kling.inference.contract.model.KlingGenerationJob;
import com.example.kling.inference.contract.model.KlingGenerationRequest;
import com.example.kling.inference.contract.model.VideoGenerationPayload;
import java.time.Duration;
import java.util.List;
import java.util.Map;

final class KlingExampleSupport {

    private KlingExampleSupport() {
    }

    static KlingInferenceClient client() {
        return new WebClientKlingInferenceClient(
                new KlingInferenceClientOptions(
                        "http://localhost:8091",
                        null,
                        Duration.ofSeconds(30),
                        Duration.ofMillis(500)
                )
        );
    }

    static KlingGenerationRequest<VideoGenerationPayload> textToVideoRequest(String requestId, String idempotencyKey) {
        VideoGenerationPayload payload = new VideoGenerationPayload(
                "A cinematic shot of a futuristic city at sunrise",
                null,
                List.of(),
                5,
                "16:9",
                "std",
                null,
                Map.of()
        );

        return new KlingGenerationRequest<>(
                requestId,
                idempotencyKey,
                new InferenceCaller(
                        "model-gateway",
                        "INTERNAL_SERVICE",
                        "tenant-a",
                        "project-a",
                        "user-a",
                        Map.of()
                ),
                GenerationType.TEXT_TO_VIDEO,
                "ai-video-storyboard",
                "kling-v3",
                5,
                null,
                Map.of(),
                payload
        );
    }

    static void persistJobId(String businessId, String jobId) {
        // Replace this with your own database update.
        System.out.printf("Persist businessId=%s, jobId=%s%n", businessId, jobId);
    }

    static void markBusinessSucceeded(String businessId, KlingGenerationJob job) {
        // Replace this with your own success state update.
        System.out.printf("Business %s succeeded, result=%s%n", businessId, job.result());
    }

    static void markBusinessFailed(String businessId, KlingGenerationJob job) {
        InferenceError error = job.error();
        // Replace this with your own failure state update, retry policy, or alert.
        System.out.printf(
                "Business %s failed, status=%s, errorCode=%s, errorMessage=%s%n",
                businessId,
                job.status(),
                error == null ? null : error.code(),
                error == null ? null : error.message()
        );
    }

    static void handleTerminalJob(String businessId, KlingGenerationJob job) {
        if (!job.status().isTerminal()) {
            return;
        }
        switch (job.status()) {
            case SUCCEEDED -> markBusinessSucceeded(businessId, job);
            case FAILED, CANCELLED, TIMEOUT -> markBusinessFailed(businessId, job);
            default -> throw new IllegalStateException("Unexpected terminal status: " + job.status());
        }
    }
}
