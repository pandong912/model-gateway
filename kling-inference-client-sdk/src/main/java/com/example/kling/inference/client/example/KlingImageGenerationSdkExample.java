package com.example.kling.inference.client.example;

import com.example.kling.inference.client.KlingInferenceClient;
import com.example.kling.inference.client.KlingInferenceClientOptions;
import com.example.kling.inference.client.WebClientKlingInferenceClient;
import com.example.kling.inference.contract.enums.GenerationType;
import com.example.kling.inference.contract.model.ImageGenerationPayload;
import com.example.kling.inference.contract.model.InferenceCaller;
import com.example.kling.inference.contract.model.KlingGenerationRequest;
import com.example.kling.inference.contract.model.KlingGenerationResult;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class KlingImageGenerationSdkExample {

    public static void main(String[] args) {
        KlingInferenceClient client = new WebClientKlingInferenceClient(
                new KlingInferenceClientOptions(
                        "http://localhost:8091",
                        null,
                        Duration.ofSeconds(30),
                        Duration.ofMillis(500)
                ),
                WebClientKlingInferenceClient.IMAGE_GENERATIONS_PATH
        );

        ImageGenerationPayload payload = new ImageGenerationPayload(
                "A premium ecommerce hero image of a ceramic coffee mug on a warm wooden table",
                null,
                List.of(),
                "1k",
                20260610,
                Map.of(
                        "style", "photorealistic",
                        "background", "warm studio lighting"
                )
        );

        KlingGenerationRequest<ImageGenerationPayload> request = new KlingGenerationRequest<>(
                "req-image-generation-demo-001",
                "demo-image-generation-mug-003",
                new InferenceCaller(
                        "creator-tools",
                        "INTERNAL_SERVICE",
                        "tenant-a",
                        "project-a",
                        "user-a",
                        Map.of()
                ),
                GenerationType.IMAGE_GENERATION,
                "ecommerce-hero-image",
                "kling-v3",
                5,
                null,
                Map.of(),
                payload
        );

        KlingGenerationResult result = client.generateAsync(request).join();
        System.out.println("image generation result=" + result);
    }
}
