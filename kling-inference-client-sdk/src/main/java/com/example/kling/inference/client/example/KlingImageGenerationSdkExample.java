package com.example.kling.inference.client.example;

import com.example.kling.inference.client.KlingInferenceClient;
import com.example.kling.inference.client.KlingInferenceClientOptions;
import com.example.kling.inference.client.WebClientKlingInferenceClient;
import com.example.kling.inference.client.model.KlingGenerationRequestBuilder;
import com.example.kling.inference.contract.model.InferenceCaller;
import com.example.kling.inference.contract.model.KlingGenerationPayload;
import com.example.kling.inference.contract.model.KlingGenerationRequest;
import com.example.kling.inference.contract.model.KlingGenerationResult;
import java.time.Duration;
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

        KlingGenerationRequest<? extends KlingGenerationPayload> request = KlingGenerationRequestBuilder
                .imageGeneration("A premium ecommerce hero image of a ceramic coffee mug on a warm wooden table")
                .idempotencyKey("demo-image-generation-mug-002")
                .caller(new InferenceCaller(
                        "creator-tools",
                        "INTERNAL_SERVICE",
                        "tenant-a",
                        "project-a",
                        "user-a",
                        Map.of()
                ))
                .scenario("ecommerce-hero-image")
                .resolution("1024x1024")
                .seed(20260610)
                .parameters(Map.of(
                        "style", "photorealistic",
                        "background", "warm studio lighting"
                ))
                .build();

        KlingGenerationResult result = client.generateAsync(request).join();
        System.out.println("image generation result=" + result);
    }
}
