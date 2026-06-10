package com.example.kling.inference.client.example;

import com.example.kling.inference.client.KlingInferenceClient;
import com.example.kling.inference.contract.model.KlingGenerationJob;
import com.example.kling.inference.contract.model.KlingGenerationRequest;
import com.example.kling.inference.contract.model.VideoGenerationPayload;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public class KlingWaitJobLoopExample {

    public static void main(String[] args) {
        KlingInferenceClient client = KlingExampleSupport.client();
        String businessId = "biz-wait-001";
        KlingGenerationRequest<VideoGenerationPayload> request = KlingExampleSupport.textToVideoRequest(
                "req-wait-001",
                "idem-wait-001"
        );

        KlingGenerationJob submitted = Objects.requireNonNull(client.submit(request).block());
        KlingExampleSupport.persistJobId(businessId, submitted.jobId());

        Instant deadline = Instant.now().plus(Duration.ofMinutes(10));
        KlingGenerationJob snapshot = submitted;

        while (Instant.now().isBefore(deadline)) {
            snapshot = Objects.requireNonNull(client.waitJob(snapshot.jobId(), Duration.ofSeconds(30)).block());
            System.out.println("wait snapshot=" + snapshot);

            if (snapshot.status().isTerminal()) {
                KlingExampleSupport.handleTerminalJob(businessId, snapshot);
                return;
            }
        }

        // At this point the backend task may still be running.
        // Keep jobId and reconcile later instead of marking the generation as succeeded.
        System.out.printf("Business %s wait loop reached local deadline, jobId=%s%n", businessId, snapshot.jobId());
    }
}
