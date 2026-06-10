package com.example.kling.inference.client.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kling.inference.contract.enums.GenerationType;
import com.example.kling.inference.contract.model.ImageGenerationPayload;
import com.example.kling.inference.contract.model.InferenceCaller;
import com.example.kling.inference.contract.model.KlingGenerationPayload;
import com.example.kling.inference.contract.model.KlingGenerationRequest;
import com.example.kling.inference.contract.model.VideoGenerationPayload;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KlingGenerationRequestBuilderTest {

    @Test
    void buildsTextToVideoRequestWithDefaultsAndCaller() {
        KlingGenerationRequest<? extends KlingGenerationPayload> request = KlingGenerationRequestBuilder
                .textToVideo("generate a cinematic city video")
                .idempotencyKey("idem-001")
                .caller(new InferenceCaller("model-gateway", "INTERNAL_SERVICE", "tenant-a", "project-a", "user-a", Map.of()))
                .scenario("storyboard")
                .durationSeconds(5)
                .build();

        assertThat(request.requestId()).startsWith("req_");
        assertThat(request.idempotencyKey()).isEqualTo("idem-001");
        assertThat(request.generationType()).isEqualTo(GenerationType.TEXT_TO_VIDEO);
        assertThat(request.model()).isEqualTo("kling-video");
        assertThat(request.caller().callerId()).isEqualTo("model-gateway");
        assertThat(request.payload()).isInstanceOf(VideoGenerationPayload.class);
        VideoGenerationPayload payload = (VideoGenerationPayload) request.payload();
        assertThat(payload.prompt()).isEqualTo("generate a cinematic city video");
        assertThat(payload.durationSeconds()).isEqualTo(5);
    }

    @Test
    void buildsImageGenerationRequest() {
        KlingGenerationRequest<? extends KlingGenerationPayload> request = KlingGenerationRequestBuilder
                .imageGeneration("generate a product hero image")
                .scenario("ecommerce-hero-image")
                .resolution("1024x1024")
                .build();

        assertThat(request.generationType()).isEqualTo(GenerationType.IMAGE_GENERATION);
        assertThat(request.model()).isEqualTo("kling-image");
        assertThat(request.payload()).isInstanceOf(ImageGenerationPayload.class);
        ImageGenerationPayload payload = (ImageGenerationPayload) request.payload();
        assertThat(payload.prompt()).isEqualTo("generate a product hero image");
        assertThat(payload.resolution()).isEqualTo("1024x1024");
    }
}
