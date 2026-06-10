package com.example.kling.inference.client.example;

import com.example.kling.inference.client.KlingInferenceClient;
import com.example.kling.inference.contract.model.KlingGenerationEvent;
import com.example.kling.inference.contract.model.KlingGenerationJob;
import com.example.kling.inference.contract.model.KlingGenerationRequest;
import com.example.kling.inference.contract.model.VideoGenerationPayload;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import reactor.core.Disposable;

public class KlingWatchJobEventsExample {

    public static void main(String[] args) throws InterruptedException {
        KlingInferenceClient client = KlingExampleSupport.client();
        String businessId = "biz-watch-001";
        KlingGenerationRequest<VideoGenerationPayload> request = KlingExampleSupport.textToVideoRequest(
                "req-watch-001",
                "idem-watch-001"
        );

        KlingGenerationJob submitted = Objects.requireNonNull(client.submit(request).block());
        KlingExampleSupport.persistJobId(businessId, submitted.jobId());

        CountDownLatch terminalEventReceived = new CountDownLatch(1);
        AtomicReference<Throwable> streamError = new AtomicReference<>();

        Disposable subscription = client.watchJob(submitted.jobId())
                .subscribe(
                        event -> handleEvent(businessId, event, terminalEventReceived),
                        error -> {
                            streamError.set(error);
                            terminalEventReceived.countDown();
                        }
                );

        boolean completedInTime = terminalEventReceived.await(Duration.ofMinutes(10).toSeconds(), TimeUnit.SECONDS);
        subscription.dispose();

        if (streamError.get() != null || !completedInTime) {
            // SSE can disconnect. Always repair state with a snapshot query.
            KlingGenerationJob repaired = Objects.requireNonNull(client.getJob(submitted.jobId()).block());
            if (repaired.status().isTerminal()) {
                KlingExampleSupport.handleTerminalJob(businessId, repaired);
            } else {
                System.out.printf("Business %s still running after SSE interruption, jobId=%s%n", businessId, submitted.jobId());
            }
        }
    }

    private static void handleEvent(
            String businessId,
            KlingGenerationEvent event,
            CountDownLatch terminalEventReceived
    ) {
        System.out.println("event=" + event);
        if (event.status() != null && event.status().isTerminal()) {
            KlingGenerationJob terminalSnapshot = new KlingGenerationJob(
                    event.jobId(),
                    null,
                    null,
                    null,
                    null,
                    event.status(),
                    event.progress(),
                    null,
                    null,
                    null,
                    event.occurredAt(),
                    event.occurredAt(),
                    event.occurredAt(),
                    null,
                    event.result(),
                    event.error(),
                    event.metadata()
            );
            KlingExampleSupport.handleTerminalJob(businessId, terminalSnapshot);
            terminalEventReceived.countDown();
        }
    }
}
