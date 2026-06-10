package com.example.kling.inference.client.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kling.inference.contract.enums.GenerationType;
import com.example.kling.inference.contract.model.InferenceCaller;
import com.example.kling.inference.contract.model.VideoGenerationRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VideoGenerationRequestBuilderTest {

    @Test
    void buildsTextToVideoRequestWithDefaultsAndCaller() {
        VideoGenerationRequest request = VideoGenerationRequestBuilder
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
}
