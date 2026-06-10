package com.example.kling.inference.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kling.inference.client.exception.KlingInferenceJobException;
import com.example.kling.inference.contract.enums.GenerationType;
import com.example.kling.inference.contract.enums.InferenceJobStatus;
import com.example.kling.inference.contract.model.KlingGenerationJob;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KlingInferenceJobExceptionTest {

    @Test
    void keepsFailedJobSnapshot() {
        Instant now = Instant.now();
        KlingGenerationJob job = new KlingGenerationJob(
                "kg_001",
                "req_001",
                "idem_001",
                "model-gateway",
                GenerationType.TEXT_TO_VIDEO,
                InferenceJobStatus.FAILED,
                10,
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

        KlingInferenceJobException exception = new KlingInferenceJobException(job);

        assertThat(exception.getJob()).isSameAs(job);
        assertThat(exception.getMessage()).contains("FAILED").contains("kg_001");
    }
}
