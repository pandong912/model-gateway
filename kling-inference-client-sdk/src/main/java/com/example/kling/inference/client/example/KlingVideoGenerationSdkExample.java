package com.example.kling.inference.client.example;

import com.example.kling.inference.client.KlingInferenceClient;
import com.example.kling.inference.client.KlingInferenceClientOptions;
import com.example.kling.inference.client.WebClientKlingInferenceClient;
import com.example.kling.inference.contract.enums.GenerationType;
import com.example.kling.inference.contract.model.InferenceCaller;
import com.example.kling.inference.contract.model.KlingGenerationJob;
import com.example.kling.inference.contract.model.KlingGenerationRequest;
import com.example.kling.inference.contract.model.KlingGenerationResult;
import com.example.kling.inference.contract.model.VideoGenerationPayload;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class KlingVideoGenerationSdkExample {

    public static void main(String[] args) {
        KlingInferenceClient client = new WebClientKlingInferenceClient(
                new KlingInferenceClientOptions(
                        "http://localhost:8091",
                        null,
                        Duration.ofSeconds(30),
                        Duration.ofMillis(500)
                )
        );

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

        KlingGenerationRequest<VideoGenerationPayload> request = new KlingGenerationRequest<>(
                "req-text-to-video-demo-001",
                "demo-futuristic-city-003",
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

        String jobId = client.submit(request).block().jobId();
        client.watchJob(jobId).subscribe(event -> System.out.println("event=" + event));
        KlingGenerationJob snapshot = client.waitJob(jobId, Duration.ofSeconds(30)).block();
        System.out.println("wait snapshot=" + snapshot);

        CompletableFuture<KlingGenerationResult> future = client.generateAsync(request);
        System.out.println("future result=" + future.join());
    }
}
