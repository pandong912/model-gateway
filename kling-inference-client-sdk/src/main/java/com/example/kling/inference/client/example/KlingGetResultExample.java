package com.example.kling.inference.client.example;

import com.example.kling.inference.client.KlingInferenceClient;
import com.example.kling.inference.contract.enums.AssetType;
import com.example.kling.inference.contract.model.KlingGenerationJob;
import com.example.kling.inference.contract.model.KlingGenerationRequest;
import com.example.kling.inference.contract.model.KlingGenerationResult;
import com.example.kling.inference.contract.model.OutputAsset;
import com.example.kling.inference.contract.model.VideoGenerationPayload;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public class KlingGetResultExample {

    public static void main(String[] args) {
        KlingInferenceClient client = KlingExampleSupport.client();
        String businessId = "biz-result-001";
        KlingGenerationRequest<VideoGenerationPayload> request = KlingExampleSupport.textToVideoRequest(
                "req-result-001",
                "idem-result-001"
        );

        KlingGenerationJob submitted = Objects.requireNonNull(client.submit(request).block());
        KlingExampleSupport.persistJobId(businessId, submitted.jobId());

        KlingGenerationJob terminalJob = waitUntilTerminal(client, submitted.jobId(), Duration.ofMinutes(10));
        if (!terminalJob.status().isTerminal()) {
            System.out.printf("Business %s is still running, jobId=%s%n", businessId, submitted.jobId());
            return;
        }
        if (!terminalJob.status().name().equals("SUCCEEDED")) {
            KlingExampleSupport.markBusinessFailed(businessId, terminalJob);
            return;
        }

        KlingGenerationResult result = Objects.requireNonNull(client.getResult(submitted.jobId()).block());
        for (OutputAsset output : result.outputs()) {
            if (output.type() == AssetType.VIDEO) {
                System.out.printf("Generated video asset: id=%s, url=%s%n", output.assetId(), output.url());
            } else {
                System.out.printf("Generated asset: type=%s, id=%s, url=%s%n", output.type(), output.assetId(), output.url());
            }
        }
    }

    private static KlingGenerationJob waitUntilTerminal(
            KlingInferenceClient client,
            String jobId,
            Duration maxWait
    ) {
        Instant deadline = Instant.now().plus(maxWait);
        KlingGenerationJob snapshot = Objects.requireNonNull(client.getJob(jobId).block());
        while (!snapshot.status().isTerminal() && Instant.now().isBefore(deadline)) {
            snapshot = Objects.requireNonNull(client.waitJob(jobId, Duration.ofSeconds(30)).block());
        }
        return snapshot;
    }
}
