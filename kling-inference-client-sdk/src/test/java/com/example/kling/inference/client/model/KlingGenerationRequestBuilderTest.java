package com.example.kling.inference.client.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kling.inference.contract.enums.GenerationType;
import com.example.kling.inference.contract.model.InferenceCaller;
import com.example.kling.inference.contract.model.KlingGenerationRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KlingGenerationRequestBuilderTest {

    @Test
    void buildsTextToVideoRequestWithDefaultsAndCaller() {
        KlingGenerationRequest request = KlingGenerationRequestBuilder
                .textToVideo("generate a cinematic city video")
                .idempotencyKey("idem-001")
                .caller(new InferenceCaller("model-gateway", "INTERNAL_SERVICE", "tenant-a", "project-a", "user-a", Map.of()))
                .scenario("storyboard")
                .durationSeconds(5)
                .build();

        assertThat(request.requestId()).startsWith("req_");
        assertThat(request.idempotencyKey()).isEqualTo("idem-001");
        assertThat(request.generationType()).isEqualTo(GenerationType.TEXT_TO_VIDEO);
        assertThat(request.prompt()).isEqualTo("generate a cinematic city video");
        assertThat(request.model()).isEqualTo("kling-video");
        assertThat(request.durationSeconds()).isEqualTo(5);
        assertThat(request.caller().callerId()).isEqualTo("model-gateway");
    }

    @Test
    void buildsImageGenerationRequest() {
        KlingGenerationRequest request = KlingGenerationRequestBuilder
                .imageGeneration("generate a product hero image")
                .scenario("ecommerce-hero-image")
                .resolution("1024x1024")
                .build();

        assertThat(request.generationType()).isEqualTo(GenerationType.IMAGE_GENERATION);
        assertThat(request.model()).isEqualTo("kling-image");
        assertThat(request.prompt()).isEqualTo("generate a product hero image");
        assertThat(request.resolution()).isEqualTo("1024x1024");
    }
}
