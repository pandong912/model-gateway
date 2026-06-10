package com.example.kling.inference.client.example;

import com.example.kling.inference.client.KlingInferenceClient;
import com.example.kling.inference.client.KlingInferenceClientOptions;
import com.example.kling.inference.client.WebClientKlingInferenceClient;
import com.example.kling.inference.client.model.KlingGenerationRequestBuilder;
import com.example.kling.inference.contract.model.InferenceCaller;
import com.example.kling.inference.contract.model.KlingGenerationJob;
import com.example.kling.inference.contract.model.KlingGenerationRequest;
import com.example.kling.inference.contract.model.KlingGenerationResult;
import java.time.Duration;
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

        KlingGenerationRequest request = KlingGenerationRequestBuilder
                .textToVideo("A cinematic shot of a futuristic city at sunrise")
                .idempotencyKey("demo-futuristic-city-001")
                .caller(new InferenceCaller(
                        "model-gateway",
                        "INTERNAL_SERVICE",
                        "tenant-a",
                        "project-a",
                        "user-a",
                        Map.of()
                ))
                .scenario("ai-video-storyboard")
                .durationSeconds(5)
                .aspectRatio("16:9")
                .resolution("1080p")
                .build();

        String jobId = client.submit(request).block().jobId();
        client.watchJob(jobId).subscribe(event -> System.out.println("event=" + event));
        KlingGenerationJob snapshot = client.waitJob(jobId, Duration.ofSeconds(30)).block();
        System.out.println("wait snapshot=" + snapshot);

        CompletableFuture<KlingGenerationResult> future = client.generateAsync(request);
        System.out.println("future result=" + future.join());
    }
}
