package com.example.kling.inference.client.example;

import com.example.kling.inference.client.KlingInferenceClient;
import com.example.kling.inference.contract.model.KlingGenerationJob;
import com.example.kling.inference.contract.model.KlingGenerationRequest;
import com.example.kling.inference.contract.model.VideoGenerationPayload;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public class KlingSubmitAndGetJobExample {

    public static void main(String[] args) throws InterruptedException {
        KlingInferenceClient client = KlingExampleSupport.client();
        String businessId = "biz-submit-001";
        KlingGenerationRequest<VideoGenerationPayload> request = KlingExampleSupport.textToVideoRequest(
                "req-submit-001",
                "idem-submit-001"
        );

        KlingGenerationJob submitted = Objects.requireNonNull(client.submit(request).block());
        KlingExampleSupport.persistJobId(businessId, submitted.jobId());

        Instant deadline = Instant.now().plus(Duration.ofMinutes(10));
        while (Instant.now().isBefore(deadline)) {
            KlingGenerationJob snapshot = Objects.requireNonNull(client.getJob(submitted.jobId()).block());
            if (snapshot.status().isTerminal()) {
                KlingExampleSupport.handleTerminalJob(businessId, snapshot);
                return;
            }

            Thread.sleep(Duration.ofSeconds(5));
        }

        // Client-side business timeout is not the same as backend TIMEOUT.
        // Keep jobId for reconciliation, alerting, retry, or later status repair.
        System.out.printf("Business %s timed out waiting locally, jobId=%s%n", businessId, submitted.jobId());
    }
}
