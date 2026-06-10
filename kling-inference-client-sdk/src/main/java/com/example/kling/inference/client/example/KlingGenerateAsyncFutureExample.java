package com.example.kling.inference.client.example;

import com.example.kling.inference.client.KlingInferenceClient;
import com.example.kling.inference.client.exception.KlingInferenceJobException;
import com.example.kling.inference.contract.model.KlingGenerationRequest;
import com.example.kling.inference.contract.model.KlingGenerationResult;
import com.example.kling.inference.contract.model.VideoGenerationPayload;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

public class KlingGenerateAsyncFutureExample {

    public static void main(String[] args) {
        KlingInferenceClient client = KlingExampleSupport.client();
        String businessId = "biz-future-001";
        KlingGenerationRequest<VideoGenerationPayload> request = KlingExampleSupport.textToVideoRequest(
                "req-future-001",
                "idem-future-001"
        );

        CompletableFuture<KlingGenerationResult> future = client.generateAsync(request)
                .orTimeout(Duration.ofMinutes(10).toSeconds(), java.util.concurrent.TimeUnit.SECONDS);

        try {
            KlingGenerationResult result = future.join();
            System.out.printf("Business %s succeeded, result=%s%n", businessId, result);
        } catch (CompletionException ex) {
            handleFutureFailure(businessId, request, ex);
        }
    }

    private static void handleFutureFailure(
            String businessId,
            KlingGenerationRequest<VideoGenerationPayload> request,
            CompletionException ex
    ) {
        Throwable cause = ex.getCause();
        if (cause instanceof KlingInferenceJobException jobException) {
            // The backend reached a terminal non-success status.
            KlingExampleSupport.markBusinessFailed(businessId, jobException.getJob());
            return;
        }
        if (cause instanceof TimeoutException) {
            // The client stopped waiting locally. The backend job may still be running.
            // Use requestId/idempotencyKey for reconciliation, alerting, or a later repair workflow.
            System.out.printf(
                    "Business %s timed out locally, requestId=%s, idempotencyKey=%s%n",
                    businessId,
                    request.requestId(),
                    request.idempotencyKey()
            );
            return;
        }
        throw ex;
    }
}
